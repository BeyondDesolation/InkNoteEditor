package com.dusk73.musicxmltools.models

data class Measure(
    val number: Int,
    val attributes: Attributes,
    val notes: List<Note>,
)
