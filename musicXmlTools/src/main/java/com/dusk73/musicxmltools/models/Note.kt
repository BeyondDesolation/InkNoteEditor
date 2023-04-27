package com.dusk73.musicxmltools.models

import com.dusk73.musicxmltools.enums.Accidental
import com.dusk73.musicxmltools.enums.NoteType
import com.dusk73.musicxmltools.models.base.MeasureElement

data class Note (
    val rest: Boolean = false,
    val chord: Boolean = false,

    val pitch: Pitch? = null,
    val duration: Int,
    val voice: Int? = null,
    val type: NoteType,

    val dots: Int = 0,
    val accidental: Accidental? = null,
    val timeModification: TimeModification? = null,

    val stem: String? = null,
    val beams: List<String>? = null,
    val notations: Notations? = null,

    val staff: Int? = null,

    // Not MusicXml values
    val octaveShift: Int = 0,
    val line: Int? = null
) : MeasureElement() {
    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}
