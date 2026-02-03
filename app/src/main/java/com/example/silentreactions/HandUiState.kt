package com.example.silentreactions

data class HandPoint(val x: Float, val y: Float)
data class HandLandmarks(val points: List<HandPoint>)

data class GestureUi(val label: String, val confidence: Float)

data class HandUiState(
    val hands: List<HandLandmarks> = emptyList(),
    val gesture: GestureUi? = null,
    val reactionToken: Long = 0L,
    val reactionLabel: String? = null
)