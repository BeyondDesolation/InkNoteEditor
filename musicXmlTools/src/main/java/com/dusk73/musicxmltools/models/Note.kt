package com.dusk73.musicxmltools.models

import com.dusk73.musicxmltools.models.base.MeasureElement

data class Note (
    val pitch: Pitch,
    val duration: Int,
    val type: String,
) : MeasureElement()
