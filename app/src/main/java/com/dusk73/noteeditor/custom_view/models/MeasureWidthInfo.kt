package com.dusk73.noteeditor.custom_view.models

data class MeasureWidthInfo(
    val width: Float,
    val extraSpace: Float,
    val sumOfIntervalWeights: Float,
    var attributesWidth: Float = 0f,
)