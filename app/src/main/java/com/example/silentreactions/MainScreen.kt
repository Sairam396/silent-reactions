package com.example.silentreactions

import android.Manifest
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.framework.image.MPImage
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Composable
fun MainScreen(viewModel: HandViewModel) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe ViewModel UI state
    // Whenever ViewModel changes hands/gestures/reactionToken,
    // Compose automatically updates the UI
    val ui by viewModel.ui.collectAsState()

    // Check camera permission once at startup.
    // If not granted, we show a permission screen first.
    var hasCameraPermission by remember {
        mutableStateOf(
            PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PermissionChecker.PERMISSION_GRANTED
        )
    }

    // Permission launcher:
    // When User clicks "Allow Camera", Android shows the permission dialog.
    // The result(granted/denied) updates hasCameraPermission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    // Create MediaPipe recognizer ONCE (important for performance).
    // We use remember {} so it is not recreated on recomposition.
    val recognizer = remember {
        MpGestureRecognizer(
            context = context,

            // MediaPipe result callback:
            // We forward results into the ViewModel.
            onResult = { gesture, hands, tsMs ->
                viewModel.onResult(gesture, hands, tsMs)
            },
            // MediaPipe error callback:
            // For now we do nothing, later we can show an error message
            onError = { }
        )
    }

    // Cleanup:
    // When this screen is removed/destroyed, close MediaPipe to free resources.
    DisposableEffect(Unit) {
        onDispose { recognizer.close() }
    }

    // If Permission is not granted, show a simple permission UI and return.
    if (!hasCameraPermission) {
        PermissionScreen(
            onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        return
    }

    /*
    Main UI: a Box that stacks 3 layers.

    Why Box?
    - Because we need Camera as background + overlays on top.
     */
    Box(Modifier.fillMaxSize()) {

        /*
        Layer 1: Camera Preview + Frame Analysis:
        This composable:
        - shows the front camera preview
        - also gives us frames (mpImage + timestamp) via onFrame callback
         */
        CameraPreviewWithAnalysis(
            context = context,
            lifecycleOwner = lifecycleOwner,
            modifier = Modifier.fillMaxSize(),

            // onFrame is called for each analyzed camera frame.
            // We send the frame to MediaPipe for gesture recognition.
            onFrame = { mpImage: MPImage, tsMs: Long ->
                recognizer.recognizeAsync(mpImage, tsMs)
            }
        )

        /*
        LAYER 2: LANDMARKS OVERLAY (Canvas)
        - Draw dots for each landmark point (21 points per hand).
        - This helps us visually confirm tracking works.
       */
        LandmarksOverlay(
            hands = ui.hands,
            modifier = Modifier.fillMaxSize(),
            // Front camera often looks mirrored like a selfie.
            // If dots appear on the opposite side, toggle this.
            mirrorX = true
        )

        /*
        LAYER 3: REACTION UI
        - When ViewModel decides a gesture is held for 1 second,
         - it changes reactionToken and reactionLabel.
         - This shows briefly whenever reactionToken changes.
         */
        EmojiRainOverlay(
            token = ui.reactionToken,
            label = ui.reactionLabel,
            modifier = Modifier.fillMaxSize()
        )

    }
}

/*
Simple permission screen
- Explains why camera permission is needed.
- Button to launch permission dialog.
 */
@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Silent Reactions needs camera access to detect hand gestures.",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onGrant) {
            Text("Allow Camera")
        }
    }
}

/*
LandmarksOverlay:
- Draws the hand landmark dots on top of the camera preview.
- Each HandPoint is normalized 0..1, so we scale by canvas width/height.
- mirrorX = true flips X axis for front camera selfie view.
 */

@Composable
fun LandmarksOverlay(
    hands: List<HandLandmarks>,
    modifier: Modifier = Modifier,
    mirrorX: Boolean
) {
    Canvas(modifier = modifier) {
        hands.forEach { hand ->
            hand.points.forEach { p ->
            // If front camera is mirrored, flip X coordinate.
                val xNorm = if (mirrorX) (1f - p.x) else p.x

                // Convert normalized coordinates (0..1) to screen pixels.
                val x = xNorm * size.width
                val y = p.y * size.height

                // Draw as small dot for each landmark.
                drawCircle(
                    color = Color.Green, // If the dots are too big or too bright, try: color = Color.White.copy(alpha = 0.9f),
                    radius = 6f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/*
EmojiRainOverlay - "emoji rain" particle animation
When the ViewModel confirms a gesture (held for ~1 second), it updates:
reactionToken = timestampMs
reactionLabel = "Agree" / "Wait" / ...

This composable receives:
token -> changes whenever a NEW reaction happens
label -> what the reaction means (Agree/Disagree/etc.)

What we do here:
1) When token changes, create a bunch of particles and add them to a list.
2) Run an update loop that moves particles downward and fades them out.
3) Draw particles on a Canvas on top of the camera preview.
 */

/*
  A single falling emoji particle.

  emoji:        The emoji string we draw ("👍", "✋", etc.)
  xPx, yPx:     Current position in pixels on the screen
  vyPxPerSec:   Vertical speed (pixels per second). Higher = faster fall.
  sizePx:       Text size for the emoji (in pixels)
  alpha:        Opacity (1.0 fully visible -> 0.0 invisible)
  lifeMs:       How much time is left before the particle disappears (milliseconds)
*/

private data class EmojiParticle(
    val emoji: String,
    var xPx: Float,
    var yPx: Float,
    var vyPxPerSec: Float,
    var sizePx: Float,
    var alpha: Float,
    var lifeMs: Long
)
@Composable
fun EmojiRainOverlay(
    token: Long,
    label: String?,
    modifier: Modifier = Modifier
) {
    /*
    particles = the active list of emoji particles currently falling.

    We use mutableStateListOf so composable knows:
    - when particles are added/removed
    - it should redraw the Canvas
     */
    val particles = remember { mutableStateListOf<EmojiParticle>() }

    var canvasW by remember { mutableStateOf(0f) }
    var canvasH by remember { mutableStateOf(0f) }

    // Android Paint used to draw emoji text on the nativeCanvas.
    // isAntiAlias makes text smoother.

    val paint = remember {
        Paint().apply { isAntiAlias = true }
    }

    LaunchedEffect(token) {
        // token == 0 means no reaction has ever happened yet.
        if (token == 0L) return@LaunchedEffect

        // if we don't know canvas size yet, we can't spawn correctly
        if (canvasW <= 0f || canvasH <= 0f) return@LaunchedEffect

        // Convert label ("Agree") into an emoji ("👍")
        val emoji = emojiForOfficeLabel(label)

        // Number of emojis to spawn for this one reaction
        val count = 40

        repeat(count) {
            // Randomize initial particle properties so rain looks natural.
            val x = Random.nextFloat() * canvasW
            val startY = -Random.nextFloat() * 200f // above the top edge
            val speed = 500f + Random.nextFloat() * 900f // 500..1400 px/sec
            val size = 48f + Random.nextFloat() * 36f // 48..84 px
            val life = 1200L + Random.nextLong(0L, 900L) // 1200..2100 ms

            // Add a new particle to the list (this triggers Canvas redraw automatically
            particles.add(
                EmojiParticle(
                    emoji = emoji,
                    xPx = x,
                    yPx = startY,
                    vyPxPerSec = speed,
                    sizePx = size,
                    alpha = 1f,
                    lifeMs = life
                )
            )
        }
    }

    // Animation loop (Update Particle positions over time)
    LaunchedEffect(Unit) {
        var lastMs = System.currentTimeMillis()

        while (true) {
            // Current time
            val nowMs = System.currentTimeMillis()

            // dtMs = time elapsed since last loop iteration (in milliseconds)
            val dtMs = nowMs - lastMs
            lastMs = nowMs

            // Convert milliseconds to seconds (because speed is px/sec)
            val dtSec = dtMs / 1000f

            // Update and remove particles safely.

            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()

                // Move particle down: y = y + speed * time
                p.yPx += p.vyPxPerSec * dtSec

                // Decrease lifetime
                p.lifeMs -= dtMs

                // Fade out in the last fadeWindow milliseconds of life
                val fadeWindow = 400L
                p.alpha =
                    if (p.lifeMs >= fadeWindow) 1f
                    else max(0f, p.lifeMs / fadeWindow.toFloat()) // goes from 1 -> 0


                // Remove particle
                val offScreen = p.yPx > canvasH + 200f
                val dead = p.lifeMs <= 0L
                if (dead || offScreen) {
                    iterator.remove()
                }
            }

            // Sleep ~16ms to target ~60 FPS (reduces CPU usage)
            kotlinx.coroutines.delay(16)
        }
    }

    // DRAW PARTICLES ON TOP OF THE CAMERA
    Canvas(modifier = modifier) {
        canvasW = size.width
        canvasH = size.height

        // Draw using nativeCanvas because drawing emoji as text is easiest with Paint.
        drawContext.canvas.nativeCanvas.apply {
            particles.forEach { p ->
                paint.textSize = p.sizePx

                // Convert alpha (0..1) to Paint alpha (0..255)
                paint.alpha = (min(1f, max(0f, p.alpha)) * 255).toInt()

                drawText(p.emoji, p.xPx, p.yPx + p.sizePx, paint)
            }
        }
    }
}

private fun emojiForOfficeLabel(label: String?): String {
    return when (label?.lowercase()) {
        "agree" -> "👍"
        "disagree" -> "👎"
        "wait" -> "✋"
        "question" -> "☝️"
        "done" -> "✌️"
        "need help" -> "✊"
        else -> "✨"
    }
}




































