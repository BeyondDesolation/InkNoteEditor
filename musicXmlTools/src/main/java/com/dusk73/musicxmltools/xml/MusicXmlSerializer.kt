package com.dusk73.musicxmltools.xml

import android.util.Xml
import com.dusk73.musicxmltools.models.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MusicXmlSerializer {
    private val serializer = Xml.newSerializer()

    fun serialize(file: File, scorePartwise: ScorePartwise) {

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
        for(scorePart in scorePartwise.partList.scoreParts) {
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

        for (i in 0 until part.measures.size) {
            writeMeasure(part.measures[i], i + 1)
        }
        serializer.endTag(null, "part")
    }

    private fun writeMeasure(measure: Measure, number: Int) {
        serializer.startTag(null, "measure")
        serializer.attribute(null, "number", number.toString())

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

        if(attributes.divisions != null)
            writeTag("divisions", attributes.divisions.toString())

        if(attributes.key != null)
            writeTagWithAttachments("key", listOf(
                Pair("fifths", attributes.key.fifths.toString())
            ))

        if(attributes.time != null)
            writeTagWithAttachments("time", listOf(
                Pair("beats", attributes.time.beats.toString()),
                Pair("beat-type", attributes.time.beatType.toString()),
            ))

        if(attributes.clef != null)
            writeTagWithAttachments("clef", listOf(
                Pair("sign", attributes.clef.sign),
                Pair("line", attributes.clef.line.toString()),
            ))

        serializer.endTag(null, "attributes")
    }

    private fun writeNote(note: Note) {
        serializer.startTag(null, "note")

        if(note.chord)
            writeTag("chord", null)

        if(note.rest)
            writeTag("rest", null)

        if(note.pitch != null) {
            val pith = ArrayList<Pair<String, String>>(3)
            pith.add(Pair("step", note.pitch.step))
            if(note.pitch.alter != null)
                pith.add(Pair("alter", note.pitch.alter.toString()))
            pith.add(Pair("octave", note.pitch.octave.toString()))

            writeTagWithAttachments("pitch", pith)
        }

        writeTag("duration", note.duration.toString())

        if(note.voice != null)
            writeTag("voice", note.voice.toString())

        writeTag("type", note.type.strValue)

        if(note.accidental != null)
            writeTag("accidental", note.accidental.strValue)

        if(note.timeModification != null) {
            val timeMod = ArrayList<Pair<String, String>>(3)
            timeMod.add(Pair("actual-notes", note.timeModification.actualNotes.toString()))
            timeMod.add(Pair("normal-notes", note.timeModification.normalNotes.toString()))
            if(note.timeModification.normalType != null)
                timeMod.add(Pair("normal-type", note.timeModification.normalType))

            writeTagWithAttachments("time-modification", timeMod)
        }

        if(note.stem != null)
            writeTag("stem", note.stem)

        if(note.beams != null) {
            for (i in 0 until note.beams.size) {
                writeTag("beam", note.beams[i], listOf(Pair("number", (i+1).toString())))
            }
        }
        
        if(note.notations != null) {
            writeNotations(note.notations)
        }
        
        if(note.staff != null)
            writeTag("staff", note.staff.toString())

        serializer.endTag(null, "note")
    }

    private fun writeNotations(notations: Notations) {

    }

    private fun writeTag(
        tag: String,
        value: String?,
        attributes: List<Pair<String, String>>? = null
    ) {
        serializer.startTag(null, tag)

        if(attributes != null )
            for(av in attributes) {
                serializer.attribute(null, av.first, av.second)
            }

        if(value != null)
            serializer.text(value)

        serializer.endTag(null, tag)
    }

    private fun writeTagWithAttachments(
        tag: String,
        values: List<Pair<String, String>>,
        attributes: List<Pair<String, String>>? = null
    ) {
        serializer.startTag(null, tag)

        if(attributes != null )
            for(av in attributes) {
                serializer.attribute(null, av.first, av.second)
            }

        for (tv in values) {
            serializer.startTag(null, tv.first)
            serializer.text(tv.second)
            serializer.endTag(null, tv.first)
        }
        serializer.endTag(null, tag)
    }
}

