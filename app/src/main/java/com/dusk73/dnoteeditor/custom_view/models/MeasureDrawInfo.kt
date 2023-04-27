package com.dusk73.dnoteeditor.custom_view.models

import com.dusk73.musicxmltools.models.Clef

data class MeasureDrawInfo(
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float,
    var elements: ArrayList<ElementDrawInfo>,
    var clef: Clef? = null,
    var fifths: Int? = null,
    var staffsGroup: Int,
    var attributesWidth: Float,
)