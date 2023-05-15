package com.dusk73.musicxmltools

import com.dusk73.musicxmltools.enums.Accidental
import com.dusk73.musicxmltools.enums.NoteType
import com.dusk73.musicxmltools.models.*
import com.dusk73.musicxmltools.models.base.MeasureElement
import com.dusk73.musicxmltools.models.tools.ElementPosition
import java.lang.Exception

class MusicEditor {

    // const
    private val notesMap =
        mapOf("C" to 0, "D" to 1, "E" to 2, "F" to 3, "G" to 4, "A" to 5, "B" to 6)
    private val notes = listOf("C", "D", "E", "F", "G", "A", "B")
    private val orderOfSharps = listOf("F", "C", "G", "D", "A", "E", "B")
    private val orderOfFlats = listOf("B", "E", "A", "D", "G", "C", "F")

    // divisions and duration data
    private var minNoteType = NoteType.QUARTER
    private var hasTriol = false // На будущее, добавить дуоли, квинтоли и тд

    private var divisions = 1

    //DELETE IT
/*    // octave shift data. Cache the octave shifts positions and values
    private data class OctaveShiftInfo(
        var staff: Int?,
        var startMeasure: Int,
        var stopMeasure: Int,
        var value: Int)
    // i - part index
    private val octaveShifts = ArrayList<ArrayList<OctaveShiftInfo>>(1)*/

    // clef and fifts data. Cache the measures indexes that contain the clefs or fifts
    // i - part index, j - measure index
    private val measuresWithClefs = ArrayList<ArrayList<Int>>(1)
    private val measuresWithFifths = ArrayList<ArrayList<Int>>(1)

    // main
    private val scoreParts = ArrayList<ScorePart>(1)
    private val parts = ArrayList<Part>(1)

    val scorePartwise: ScorePartwise = ScorePartwise("4.0", PartList(scoreParts), parts)

    init {
        addPart(ScorePart("P1", "PN1"))
        addMeasure(0)
        updateMeasureAttributes(0, 0, Clef("G", 2))
        updateMeasureAttributes(0, Key(0), Time(4, 4))
        recalcDivisions()
        updateMeasureDivisions()
    }

    // Не проработано
    fun addPart(scorePart: ScorePart) {
        scoreParts.add(scorePart)
        val measures = ArrayList<Measure>()
        parts.add(Part(scorePart.id, measures))

        val startNote = Note(
            rest = true,
            duration = calcDuration(NoteType.WHOLE, null),
            type = NoteType.WHOLE,
        )

        measuresWithClefs.add(ArrayList())
        measuresWithFifths.add(ArrayList())

        for (measureIndex in 0 until parts[0].measures.size) {
            val measure = parts[0].measures[measureIndex]
            measures.add(Measure(
                0,
                measure.attributes,
                ArrayList<MeasureElement>().apply { add(startNote) }
            ))
            if (measure.attributes != null) {
                cacheMeasureAttributes(parts.size - 1, measureIndex, measure.attributes)
            }
        }
    }

    // Не проработано
    fun deletePart(index: Int) {
        if (parts.size == 1)
            return

        val part = parts[index]

        scoreParts.removeAt(index)
        parts.removeAt(index)

        measuresWithClefs.removeAt(index)
        measuresWithFifths.removeAt(index)
    }

    fun deleteMeasure(measureIndex: Int) {
        if (measureIndex == 0)
            return
        for (partIndex in 0 until parts.size) {
            val measure = parts[partIndex].measures[measureIndex]
            if (measure.attributes != null)
                uncacheMeasureAttributes(partIndex, measureIndex, measure.attributes)
            (parts[partIndex].measures as ArrayList).removeAt(measureIndex)
        }
    }

    // Не проработано
    fun addMeasure(measureIndex: Int) {
        for (partIndex in 0 until parts.size) {
            val part = parts[partIndex]

            val startNote = Note(
                rest = true,
                duration = calcDuration(NoteType.WHOLE, null),
                type = NoteType.WHOLE,
            )
            (part.measures as ArrayList).add(
                measureIndex,
                Measure(0, null, ArrayList<MeasureElement>().apply { add(startNote) })
            )
        }
    }

    // Не проработано
    fun updateMeasureAttributes(measureIndex: Int, key: Key? = null, time: Time? = null) {
        for (partIndex in 0 until scorePartwise.parts.size) {
            val part = scorePartwise.parts[partIndex]
            val oldMeasure = part.measures[measureIndex]

            var newTime: Time? = time
            var newKey: Key? = key

            if (measureIndex == 0) {
                newTime = time ?: oldMeasure.attributes!!.time
                newKey = key ?: Key(0)
            }

            val attributes = if (oldMeasure.attributes != null)
                oldMeasure.attributes.copy(key = newKey, time = newTime)
            else
                Attributes(key = newKey, time = newTime)
            (part.measures as ArrayList)[measureIndex] = oldMeasure.copy(attributes = attributes)

            cacheMeasureAttributes(partIndex, measureIndex, attributes)
        }
    }

    fun updateMeasureAttributes(partIndex: Int, measureIndex: Int, clef: Clef?) {
        val part = scorePartwise.parts[partIndex]
        val oldMeasure = part.measures[measureIndex]
        val attributes = if (oldMeasure.attributes != null)
            oldMeasure.attributes.copy(clef = clef)
        else
            Attributes(clef = clef)
        (part.measures as ArrayList)[measureIndex] = oldMeasure.copy(attributes = attributes)

        cacheMeasureAttributes(partIndex, measureIndex, attributes)
    }

    fun updateAccidental(
        partIndex: Int,
        measureIndex: Int,
        noteIndex: Int,
        accidental: Accidental?
    ): Boolean {
        val noteToBeUpdate = parts[partIndex].measures[measureIndex].elements[noteIndex] as Note
        if (noteToBeUpdate.rest)
            return false

        val newAccidental = if (noteToBeUpdate.accidental == accidental) null else accidental

        val alterValue = when (newAccidental) {
            Accidental.SHARP -> 1f
            Accidental.FLAT -> -1f
            else -> null
        }

        val elements = (parts[partIndex].measures[measureIndex].elements as ArrayList)
        elements[noteIndex] = noteToBeUpdate.copy(
            accidental = newAccidental,
            pitch = noteToBeUpdate.pitch?.copy(alter = alterValue)
        )

        for (i in noteIndex + 1 until elements.size) {
            val element = elements[i]
            if (element is Note && element.line == noteToBeUpdate.line) {
                if (element.accidental != null)
                    break

                elements[i] = element.copy(pitch = element.pitch?.copy(alter = alterValue))
            }
        }
        return true
    }

    fun deleteNote(partIndex: Int, measureIndex: Int, noteIndex: Int): Boolean {
        val elements = parts[partIndex].measures[measureIndex].elements
        if (elements[noteIndex] !is Note)
            return false

        val note = elements[noteIndex] as Note
        if (note.rest)
            return false

        val startChordIndex = findStartChordIndex(elements, noteIndex)
        val nextNote = getNote(elements, startChordIndex + 1)
        if (nextNote != null && !nextNote.chord)
            return false

        (elements as ArrayList).removeAt(noteIndex)
        if (noteIndex == startChordIndex) {
            val newStartNote = elements[startChordIndex] as Note
            elements[startChordIndex] = newStartNote.copy(chord = false)
        }
        recalcStems(elements, startChordIndex)
        return true
    }

    fun setRest(partIndex: Int, measureIndex: Int, noteIndex: Int): Boolean {
        val elements = parts[partIndex].measures[measureIndex].elements
        if (elements[noteIndex] !is Note)
            return false

        val note = elements[noteIndex] as Note
        if (note.dots > 0)
            return false

        val startChordIndex = findStartChordIndex(elements, noteIndex)
        val chord = getChord(elements, startChordIndex)

        val rest = Note(rest = true, duration = note.duration, type = note.type, staff = note.staff)
        (elements as ArrayList).add(startChordIndex, rest)

        elements.removeAll(chord)
        return true
    }

    private fun getNote(elements: List<MeasureElement>, noteIndex: Int): Note? {
        if (noteIndex < elements.size) {
            val nextNote = elements[noteIndex]
            if (nextNote is Note)
                return nextNote
        }
        return null
    }

    /**
     * @param line 1 - under 1st staff line, 2 - on 1st line, 3 - under 2nd line etc
     */
    fun addNote(
        elemPosition: ElementPosition,
        line: Int,
        type: NoteType,
        staff: Int? = null
    ) {
        val partIndex = elemPosition.partIndex
        val measureIndex = elemPosition.measureIndex
        val elementIndex = elemPosition.elementIndex

        val part = getPart(partIndex)
        val measure = getMeasure(part, measureIndex)

        if (type.divisions > minNoteType.divisions) {
            minNoteType = type
            recalcDivisions()
            updateMeasureDivisions()
        }
        val oldNote = getNote(measure, elementIndex)
        val duration = calcDuration(type, oldNote.timeModification)

        if (oldNote.duration == duration) {
            if (!oldNote.rest) {
                for (note in getChord(
                    measure.elements,
                    findStartChordIndex(measure.elements, elementIndex)
                )) {
                    if (note.line!! == line)
                        return
                }
            }
        }

        var newNote =
            createNote(partIndex, measureIndex, elementIndex, line, duration, type, staff)

        if (oldNote.duration == duration) {
            if (newNote != null) {
                if (oldNote.rest) {
                    (measure.elements as ArrayList)[elementIndex] = newNote
                } else {
                    (measure.elements as ArrayList).add(elementIndex + 1, newNote)
                    recalcStems(measure.elements, elementIndex)
                }
                return
            }
        }
        val startChordIndex = findStartChordIndex(measure.elements, elementIndex)
        if (oldNote.duration < duration) {
            if (!mergeNotes(measure.elements, startChordIndex, duration)) {
                newNote = null
            }
        } else if (oldNote.duration > duration) {
            splitNotes(measure.elements, startChordIndex, oldNote.duration - duration)
        }

        if (newNote != null)
            (measure.elements as ArrayList).add(startChordIndex, newNote)
    }

    private fun createNote(
        partIndex: Int,
        measureIndex: Int,
        elementIndex: Int,
        line: Int,
        duration: Int,
        type: NoteType,
        staff: Int? = null
    ): Note? {
        val oldNote = parts[partIndex].measures[measureIndex].elements[elementIndex] as Note

        val stepAndOctave = calcNoteForLine(partIndex, measureIndex, line)

        var chord = false
        if (oldNote.pitch != null) {
            if (oldNote.duration == duration) {
                if (oldNote.pitch.step == stepAndOctave.first && oldNote.pitch.octave == stepAndOctave.second) {
                    return null
                }
                chord = true
            }
        }

        //val octaveShiftValue = calcOctaveShift(partIndex, measureIndex, elementIndex)
        val octaveShiftValue = oldNote.octaveShift
        val alter = calcAlter(
            partIndex,
            measureIndex,
            elementIndex,
            stepAndOctave.first,
            stepAndOctave.second
        )

        val stem = calcStem(line, type)

        return Note(
            pitch = Pitch(stepAndOctave.first, stepAndOctave.second + octaveShiftValue, alter),
            duration = duration,
            type = type,
            chord = chord,
            stem = stem,
            staff = staff,
            line = line,
        )
    }

    private fun cacheMeasureAttributes(partIndex: Int, measureIndex: Int, attributes: Attributes) {
        if (attributes.clef != null) {
            measuresWithClefs[partIndex].add(measureIndex)
        }
        if (attributes.key != null) {
            measuresWithFifths[partIndex].add(measureIndex)
        }
    }

    private fun uncacheMeasureAttributes(
        partIndex: Int,
        measureIndex: Int,
        attributes: Attributes
    ) {
        if (attributes.clef != null) {
            measuresWithClefs[partIndex].remove(measureIndex)
        }
        if (attributes.key != null) {
            measuresWithFifths[partIndex].remove(measureIndex)
        }
    }

    private fun updateMeasureDivisions() {
        if (parts[0].measures[0].attributes!!.divisions == divisions)
            return

        for (part in parts) {
            val newAttributes = part.measures[0].attributes!!.copy(divisions = divisions)
            (part.measures as ArrayList)[0] = part.measures[0].copy(attributes = newAttributes)
        }
    }

    private fun recalcStems(elements: ArrayList<MeasureElement>, elementIndex: Int) {
        if ((elements[elementIndex] as Note).type.divisions < 0.5f)
            return

        val startChordIndex = findStartChordIndex(elements, elementIndex)
        val chord = getChord(elements, startChordIndex)

        var minLine = (elements[startChordIndex] as Note).line!!
        var maxLine = (elements[startChordIndex] as Note).line!!

        for (note in chord) {
            val line = note.line!!
            if (line > maxLine) {
                maxLine = line
            } else if (line < minLine) {
                minLine = line
            }
        }
        val stem = if ((6 - minLine) + (6 - maxLine) > 0) "up" else "down"
        for (i in startChordIndex until startChordIndex + chord.size) {
            val note = elements[i] as Note

            if (note.stem != null && note.stem == stem) {
                continue
            } else {
                elements[i] = note.copy(stem = stem)
            }
        }
    }

    private fun findClef(partIndex: Int, measureIndex: Int): Clef {
        val part = parts[partIndex]
        var measureWithClefIndex = -1

        for (i in measuresWithClefs[partIndex]) {
            if (i <= measureIndex && i > measureWithClefIndex) {
                measureWithClefIndex = i
            }
        }
        return part.measures[measureWithClefIndex].attributes?.clef!!
    }

    fun calcLineOfNoteC(clef: Clef): Int {
        var noteCLine = -1
        when (clef.sign) {
            "G" -> noteCLine = clef.line * 2 - 4
            "F" -> noteCLine = clef.line * 2 - 3
            "C" -> noteCLine = clef.line * 2
        }
        return noteCLine
    }

    // УСТАРЕЛО, НО РАБОТАЕТ
    fun calcLineForNote(partIndex: Int, measureIndex: Int, elementIndex: Int): Int {
        val note = parts[partIndex].measures[measureIndex].elements[elementIndex] as Note

        val clef = findClef(partIndex, measureIndex)
        val CLine = calcLineOfNoteC(clef)

        val octave = note.pitch!!.octave - note.octaveShift
        val octaveMod = octave - if (clef.sign == "G" || clef.sign == "C") 4 else 3
        return CLine + (octaveMod * 7) + notesMap[note.pitch.step]!!
    }

    private fun calcNoteForLine(partIndex: Int, measureIndex: Int, line: Int): Pair<String, Int> {
        val clef = findClef(partIndex, measureIndex)
        val CLine = calcLineOfNoteC(clef)

        // 0-C 1-D 2-E 3-F 4-G 5-A 6-B
        val truePosition = line - CLine

        var octave = if (truePosition >= 0)
            truePosition / 7
        else
            ((truePosition * -1 - 1) / 7 + 1) * -1

        octave += if (clef.sign == "G" || clef.sign == "C") 4 else 3

        var noteIndex = truePosition % 7
        if (noteIndex < 0)
            noteIndex += 7

        return Pair(notes[noteIndex], octave)
    }

    // УСТАРЕЛО DELETE IT
    /*private fun calcOctaveShift(partIndex: Int, measureIndex: Int, elementIndex: Int): Int {
        for(octaveShift in octaveShifts[partIndex]) {
            if(octaveShift.startMeasure <= measureIndex && octaveShift.stopMeasure >= measureIndex) {
                if(octaveShift.start <= elementIndex && octaveShift.stop >= elementIndex) {
                    return octaveShift.value
                }
            }
        }
        return 0
    }*/

    private fun calcAlter(
        partIndex: Int,
        measureIndex: Int,
        elementIndex: Int,
        step: String,
        octave: Int
    ): Float? {
        val part = parts[partIndex]
        val measure = part.measures[measureIndex]

        if (measure.elements.isNotEmpty() && elementIndex > 0) {
            for (i in elementIndex - 1..0) {
                val element = measure.elements[i]
                if (element is Note && element.pitch != null) {
                    if (element.pitch.octave == octave && element.pitch.step == step) {
                        return element.pitch.alter
                    }
                }
            }
        }

        var measureWithFifthsIndex = -1
        for (i in measuresWithFifths[partIndex]) {
            if (i <= measureIndex && i > measureWithFifthsIndex) {
                measureWithFifthsIndex = i
            }
        }

        if (measureWithFifthsIndex == -1)
            return null

        val fifths = part.measures[measureWithFifthsIndex].attributes?.key?.fifths!!

        if (fifths > 0) {
            val minSharpsCount = orderOfSharps.indexOf(step) + 1
            return if (fifths >= minSharpsCount) 1f else null
        } else {
            val minFlatsCount = orderOfFlats.indexOf(step) + 1
            return if ((-fifths) >= minFlatsCount) -1f else null
        }
    }

    private fun calcStem(linePosition: Int, type: NoteType): String? {
        if (type == NoteType.WHOLE)
            return null

        return if (linePosition <= 6) "up" else "down"
    }

    private fun recalcDivisions() {
        var newDivisions = minNoteType.divisions
        if (hasTriol)
            newDivisions *= 3

        if (newDivisions.toInt() != divisions) {
            divisions = newDivisions.toInt()
            recalcDurations()
        }
    }

    private fun calcDuration(
        noteType: NoteType,
        timeModification: TimeModification? = null,
        dots: Int = 0
    ): Int {
        var newDuration = divisions / noteType.divisions
        if (timeModification != null) {
            newDuration = newDuration * timeModification.normalNotes / timeModification.actualNotes
        }
        if (dots > 0) {
            var dotMod = newDuration
            for (dot in 0..dots) {
                dotMod /= 2
                newDuration += dotMod
            }
        }

        return newDuration.toInt()
    }

    private fun recalcDurations() {
        for (p in 0 until parts.size) {
            for (m in 0 until parts[p].measures.size) {
                val elements = parts[p].measures[m].elements as ArrayList

                for (e in 0 until elements.size) {
                    val element = elements[e]

                    if (element is Note) {
                        val newDuration =
                            calcDuration(element.type, element.timeModification, element.dots)
                        elements[e] = element.copy(duration = newDuration)
                    }
                }
            }
        }
    }

    private fun splitNotes(
        elements: List<MeasureElement>,
        startChordIndex: Int,
        totalDuration: Int
    ) {

        val newNotesDurations = splitDuration(totalDuration)
        val notesForSplit = getChord(elements, startChordIndex)

        (elements as ArrayList<MeasureElement>).removeAll(notesForSplit)

        var i = startChordIndex
        for (d in newNotesDurations) {
            val newChordStartIndex = i
            for (note in notesForSplit) {
                val newNoteType = durationToType(d, note.timeModification)
                    ?: throw Exception("Can't calculate a note type from a duration")

                val newNote = note.copy(
                    duration = d,
                    type = newNoteType,
                )
                elements.add(i, newNote)
                i++
            }
            if (!notesForSplit[0].rest && notesForSplit[0].type == NoteType.WHOLE) {
                recalcStems(elements, newChordStartIndex)
            }
        }
    }

    private fun splitDuration(from: Int, timeModification: TimeModification? = null): List<Int> {
        var currentDuration = from
        val resultDurations = ArrayList<Int>()

        for (type in NoteType.values()) {
            var noteTypeDuration = divisions / type.divisions
            if (timeModification != null)
                noteTypeDuration =
                    noteTypeDuration * timeModification.normalNotes / timeModification.actualNotes

            if (currentDuration >= noteTypeDuration) {
                currentDuration -= noteTypeDuration.toInt()
                resultDurations.add(noteTypeDuration.toInt())
            }
            if (currentDuration == 0)
                break
        }

        resultDurations.reverse()
        return resultDurations
    }

    // from value must be a duration WITHOUT an influence of dots
    private fun splitDuration(from: Int, to: Int): List<Int> {
        if (to > from)
            throw IllegalArgumentException("Can't split $from to $to")

        if (to == from)
            return listOf(to)

        var currentDuration = from
        val resultDurations = ArrayList<Int>()

        while (currentDuration > to) {
            currentDuration /= 2
            resultDurations.add(currentDuration)
        }
        resultDurations.add(currentDuration)
        resultDurations.reverse()

        return resultDurations
    }

    private fun getChord(elements: List<MeasureElement>, startChordIndex: Int): ArrayList<Note> {
        val chord = ArrayList<Note>()
        chord.add(elements[startChordIndex] as Note)

        for (i in startChordIndex + 1 until elements.size) {
            val nextChordNote = elements[i]
            if (nextChordNote is Note && nextChordNote.chord) {
                chord.add(nextChordNote)
            } else {
                break
            }
        }
        return chord
    }

    private fun durationToType(
        duration: Int,
        timeModification: TimeModification? = null
    ): NoteType? {
        for (type in NoteType.values()) {
            var noteTypeDuration = divisions / type.divisions
            if (timeModification != null)
                noteTypeDuration =
                    noteTypeDuration * timeModification.normalNotes / timeModification.actualNotes

            if (duration == noteTypeDuration.toInt())
                return type
        }
        return null
    }

    private fun mergeNotes(
        elements: List<MeasureElement>,
        startChordIndex: Int,
        totalDuration: Int
    ): Boolean {
        var isPossibleToMerge = false
        var remainDuration = totalDuration

        val notesForDelete = ArrayList<Note>()
        var splitNoteIndex = -1
        var splitNoteRemainDuration = -1

        for (i in startChordIndex until elements.size) {
            val note = elements[i]
            if (note is Note) {
                if (note.chord) {
                    notesForDelete.add(note)
                    continue
                }
                if (remainDuration >= note.duration) {
                    remainDuration -= note.duration
                    notesForDelete.add(note)
                } else {
                    splitNoteRemainDuration = note.duration - remainDuration
                    splitNoteIndex = i
                    isPossibleToMerge = true
                    break
                }
                if (remainDuration == 0) {
                    isPossibleToMerge = true
                    break
                }
            }
        }

        if (!isPossibleToMerge) {
            return false
        }

        if (splitNoteIndex >= 0) {
            splitNotes(elements, splitNoteIndex, splitNoteRemainDuration)
        }

        (elements as ArrayList).removeAll(notesForDelete)
        return true
    }

    private fun findStartChordIndex(elements: List<MeasureElement>, noteIndex: Int): Int {
        var firstChordNote = elements[noteIndex] as Note
        var startChordIndex = noteIndex
        while (firstChordNote.chord) {
            startChordIndex--
            firstChordNote = elements[startChordIndex] as Note
        }
        return startChordIndex
    }

    private fun dotsToNotes() {
        //мб
    }

    private fun getPart(partIndex: Int): Part {
        if (partIndex < 0 || partIndex >= parts.size)
            throw IllegalArgumentException("incorrect part index ($partIndex). Parts size is ${parts.size}")

        return parts[partIndex]
    }

    private fun checkNewMeasureIndex(part: Part, newIndex: Int) {
        if (newIndex < 0 || newIndex > part.measures.size)
            throw IllegalArgumentException("incorrect new measure index ($newIndex). Measures size is ${part.measures.size}")
    }

    private fun checkNewElementIndex(measure: Measure, newIndex: Int) {
        if (newIndex < 0 || newIndex > measure.elements.size)
            throw IllegalArgumentException("incorrect new element index ($newIndex). Measure size is ${measure.elements.size}")
    }

    private fun getMeasure(part: Part, measureIndex: Int): Measure {
        if (measureIndex < 0 || measureIndex >= part.measures.size)
            throw IllegalArgumentException("incorrect measure index ($measureIndex). Measures size is ${part.measures.size}")

        return part.measures[measureIndex]
    }

    private fun getMeasure(partIndex: Int, measureIndex: Int): Measure {
        val part = getPart(partIndex)
        return getMeasure(part, measureIndex)
    }

    private fun getNote(measure: Measure, elementIndex: Int): Note {
        if (elementIndex < 0 || elementIndex >= measure.elements.size)
            throw IllegalArgumentException("incorrect element index ($elementIndex). Measure size is ${measure.elements.size}")

        if (measure.elements[elementIndex] !is Note)
            throw IllegalArgumentException("There is no note on the index $elementIndex")

        return measure.elements[elementIndex] as Note
    }
}