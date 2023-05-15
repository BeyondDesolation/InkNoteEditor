package com.dusk73.musicxmltools.enums

enum class NoteType(val strValue: String, val divisions: Double) {
    WHOLE("whole", 0.25),
    HALF("half", 0.5),
    QUARTER("quarter", 1.0),
    EIGHTH("eighth", 2.0),
    _16TH("16th", 4.0),
    _32EN("32nd", 8.0),
    _64TH("64th", 16.0),
}