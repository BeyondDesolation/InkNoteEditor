package com.dusk73.musicxmltools.models

data class ScorePartwise(
    val version: String,
    val partList: PartList,
    val parts: List<Part>,
)