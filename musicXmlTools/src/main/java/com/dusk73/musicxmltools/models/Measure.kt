package com.dusk73.musicxmltools.models

import com.dusk73.musicxmltools.models.base.MeasureElement

data class Measure(
    val number: Int,
    val attributes: Attributes?,
    val elements: List<MeasureElement>,
)
