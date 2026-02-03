package com.example.silentreactions

import android.content.Context
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer

class MpGestureRecognizer(
    context: Context,
    onResult: (GestureUi?, List<HandLandmarks>, Long) -> Unit,
    onError: (Throwable) -> Unit
) {
    private val recognizer: GestureRecognizer

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("gesture_recognizer.task")
            .setDelegate(Delegate.CPU) // To Stabilize native crashes
            .build()

        val options = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, mpImage ->
                val ts = result.timestampMs()

                val gesture = result.gestures()
                    .firstOrNull()?.firstOrNull()
                    ?.let { cat -> GestureUi(cat.categoryName(), cat.score()) }

                val hands = result.landmarks().map { hand ->
                    HandLandmarks(
                        points = hand.map { lm ->
                            HandPoint(x = lm.x(), y = lm.y())
                        }
                    )
                }
                onResult(gesture, hands, ts)
            }
            .setErrorListener { e -> onError(RuntimeException(e)) }
            .build()

        recognizer = GestureRecognizer.createFromOptions(context, options)

    }

    fun recognizeAsync(mpImage: MPImage, timestampMs: Long) {
        recognizer.recognizeAsync(mpImage, timestampMs)
    }

    fun close() = recognizer.close()

}