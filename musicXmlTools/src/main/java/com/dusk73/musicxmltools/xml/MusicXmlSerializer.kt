package com.dusk73.musicxmltools.xml

import android.util.Xml
import com.dusk73.musicxmltools.models.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MusicXmlSerializer {

    private val scorePart = ScorePart("P1", "Part 1")
    private val partList = PartList(listOf(scorePart))

    private val pitch = Pitch("C", 4)
    private val note = Note(pitch, 4, "whole")

    private val clef = Clef("G", 2)
    private val time = Time(4, 4)
    private val key = Key(0)
    private val attributes = Attributes(1, key, time, clef)

    private val measure = Measure(1, attributes, listOf(note))
    private val part = Part("P1", listOf(measure))

    private val scorePartwise = ScorePartwise("4.0", partList, listOf(part))

    private val serializer = Xml.newSerializer()

    fun serialize(file: File) {

        val fos = FileOutputStream(file)
        try {
            fos.use {

                serializer.setOutput(fos,"UTF-8")
                serializer.startDocument("UTF-8",false)
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
                serializer.docdecl(" score-partwise PUBLIC " +
                        "\"-//Recordare//DTD MusicXML 4.0 Partwise//EN\" " +
                        "\"http://www.musicxml.org/dtds/partwise.dtd\"")

                writeScorePartwise(scorePartwise)

                serializer.endDocument()
                serializer.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun writeScorePartwise(scorePartwise: ScorePartwise) {
        serializer.startTag(null, "score-partwise")
        serializer.attribute(null, "version", scorePartwise.version)

        serializer.startTag(null, "part-list")
        for(scorePart in partList.scoreParts) {
            writeScorePart(scorePart)
        }
        serializer.endTag(null, "part-list")

        for (part in scorePartwise.parts) {
            writePart(part)
        }
        serializer.endTag(null, "score-partwise")
    }

    private fun writeScorePart(scorePart: ScorePart) {
        serializer.startTag(null, "score-part")
        serializer.attribute(null, "id", scorePart.id)

        writeTag("part-name", scorePart.partName)

        serializer.endTag(null, "score-part")
    }

    private fun writePart(part: Part) {
        serializer.startTag(null, "part")
        serializer.attribute(null, "id", part.id)

        for (measure in part.measures) {
            writeMeasure(measure)
        }
        serializer.endTag(null, "part")
    }

    private fun writeMeasure(measure: Measure) {
        serializer.startTag(null, "measure")
        serializer.attribute(null, "number", measure.number.toString())

        if(measure.attributes != null) {
            writeAttributes(measure.attributes)
        }
        for (element in measure.elements) {
            when (element) {
                is Note -> writeNote(element)
            }
        }
        serializer.endTag(null, "measure")
    }

    private fun writeAttributes(attributes: Attributes) {
        serializer.startTag(null, "attributes")

        writeTag("divisions", attributes.divisions.toString())

        writeTagWithAttachments("key", listOf(
            Pair("fifths", attributes.key.fifths.toString())
        ))

        writeTagWithAttachments("time", listOf(
            Pair("beats", attributes.time.beats.toString()),
            Pair("beat-type", attributes.time.beatType.toString()),
        ))

        writeTagWithAttachments("clef", listOf(
            Pair("sign", attributes.clef.sign),
            Pair("line", attributes.clef.line.toString()),
        ))

        serializer.endTag(null, "attributes")
    }

    private fun writeNote(note: Note) {
        serializer.startTag(null, "note")

        writeTagWithAttachments("pitch", listOf(
            Pair("step", note.pitch.step),
            Pair("octave", note.pitch.octave.toString())
        ))

        writeTag("duration", note.duration.toString())
        writeTag("type", note.type)

        serializer.endTag(null, "note")
    }

    private fun writeTag(
        tag: String,
        value: String,
        attribute: String? = null,
        attributeValue: String? = null
    ) {
        serializer.startTag(null, tag)
        if(attribute != null && attributeValue != null) {
            serializer.attribute(null, attribute, attributeValue)
        }
        serializer.text(value)
        serializer.endTag(null, tag)
    }

    private fun writeTagWithAttachments(
        tag: String,
        values: List<Pair<String, String>>,
        attribute: String? = null,
        attributeValue: String? = null
    ) {
        serializer.startTag(null, tag)
        if(attribute != null && attributeValue != null) {
            serializer.attribute(null, attribute, attributeValue)
        }
        for (tv in values) {
            serializer.startTag(null, tv.first)
            serializer.text(tv.second)
            serializer.endTag(null, tv.first)
        }
        serializer.endTag(null, tag)
    }
}

