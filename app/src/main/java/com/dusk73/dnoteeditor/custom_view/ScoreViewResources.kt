package com.dusk73.dnoteeditor.custom_view

import com.dusk73.musicxmltools.enums.NoteType

class ScoreViewResources {
    companion object {

        val singleNotesUp = mapOf(
            NoteType.WHOLE to "\uD834\uDD5F",
            NoteType.HALF to "\uD834\uDD60",
            NoteType.QUARTER to "\uD834\uDD61",
            NoteType.EIGHTH to "\uD834\uDD62",
            NoteType._16TH to "\uD834\uDD63",
            NoteType._32EN to "\uD834\uDD64",
            NoteType._64TH to "\uD834\uDD65",
        )

        val restNotes = mapOf(
            NoteType.WHOLE to "\uD834\uDD3B",
            NoteType.HALF to "\uD834\uDD3C",
            NoteType.QUARTER to "\uD834\uDD3D",
            NoteType.EIGHTH to "\uD834\uDD3E",
            NoteType._16TH to "\uD834\uDD3F",
            NoteType._32EN to "\uD834\uDD40",
            NoteType._64TH to "\uD834\uDD41",
        )

        val flagsUp = mapOf(
            NoteType.EIGHTH to "\uE240",
            NoteType._16TH to "\uE242",
            NoteType._32EN to "\uE244",
            NoteType._64TH to "\uE246",
        )

        val flagsDown = mapOf(
            NoteType.EIGHTH to "\uE241",
            NoteType._16TH to "\uE243",
            NoteType._32EN to "\uE245",
            NoteType._64TH to "\uE247",
        )

        val noteTypes = mapOf(2 to "\uE082", 3 to "\uE083", 4 to "\uE084")

       /* val noStemNotes = mapOf(
            NoteType.WHOLE to "\uD834\uDD5D",
            NoteType.HALF to "\uD834\uDD57",
            NoteType.QUARTER to "\uD834\uDD58"
        )*/

         val noStemNotes = mapOf(
            NoteType.WHOLE to "\uE0A2",
            NoteType.HALF to "\uE0A3",
            NoteType.QUARTER to "\uE0A4"
        )

/*        const val steamUpHalf = "\uE1D3"
        const val steamUpQuarter = "\uE1D5"

        const val steamDownHalf = "\uE1D3"
        const val steamDownQuarter = "\uE1D5"*/

        const val stem = "\uD834\uDD65"
        const val sharp = "\uE262"
        const val flat = "\uE260"
        const val natural = "\uE261"

    }
}