package com.example.silentreactions

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HandViewModel : ViewModel() {

    private val _ui = MutableStateFlow(HandUiState())
    val ui: StateFlow<HandUiState> = _ui

    private var stableLable: String? = null
    private var stableStartMs: Long = 0L
    private var lastTriggerMs: Long = 0L

    private val holdMs = 1000L
    private val cooldownMs = 1500L
    private val minConfidence = 0.6f

    fun onResult(gesture: GestureUi?, hands: List<HandLandmarks>, timestampMs: Long) {
        _ui.value = _ui.value.copy(hands = hands, gesture = gesture)

        val label = gesture?.label
        val conf = gesture?.confidence ?: 0f

        if (label == null || conf < minConfidence) {
            stableLable = null
            return
        }

        if(stableLable != label) {
            stableLable = label
            stableStartMs = timestampMs
            return
        }

        val heldFor = timestampMs - stableStartMs
        val sinceLast = timestampMs - lastTriggerMs

        if(heldFor >= holdMs && sinceLast >= cooldownMs) {
            lastTriggerMs = timestampMs
            _ui.value = _ui.value.copy(
                reactionToken = timestampMs,
                reactionLabel = mapGestureToOfficeMeaning(label)
            )
        }
    }

    private fun mapGestureToOfficeMeaning(label: String): String {
        // MediaPipe gesture category names can vary, adjust once we see real labels on Pixel.
        return when (label.lowercase()) {
            "thumb_up", "thumbs_up" -> "Agree"
            "thumb_down", "thumbs_down" -> "Disagree"
            "open_palm" -> "Wait"
            "pointing_up" -> "Question"
            "victory" -> "Done"
            "closed_fist" -> "Need help"
            else -> "Reaction"
        }
    }

}