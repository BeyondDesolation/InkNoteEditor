package com.dusk73.noteeditor.services

import com.dusk73.musicxmltools.models.Note
import com.dusk73.musicxmltools.models.ScorePartwise
import com.dusk73.musicxmltools.models.base.MeasureElement
import kotlinx.coroutines.*

class ScorePlayer {
    private val midiHelper = MidiDriverHelper()
    private lateinit var scorePartwise: ScorePartwise

    private val noteNumbers = mapOf("C" to 0, "D" to 2, "E" to 4, "F" to 5, "G" to 7, "A" to 9, "B" to 11)

    private val temp = 120
    private var tickDurationTime = 0

    private val newScope = CoroutineScope(Dispatchers.Default)
    private var playing = ArrayList<Job>()

    fun setScorePartwise(scorePartwise: ScorePartwise) {
        this.scorePartwise = scorePartwise

    }

    private fun calcTickDurationTime() {
        val quarterTime = 60000 / temp
        tickDurationTime = quarterTime / scorePartwise.parts[0].measures[0].attributes!!.divisions!!
    }

    fun start() {
        midiHelper.start()
        calcTickDurationTime()
        playing.clear()
        for(partIndex in 0 until scorePartwise.parts.size) {
            playing.add(newScope.launch {
                play(partIndex)
            })
        }
    }

    fun stop() {
        for (p in playing) {
            p.cancel()
        }
        midiHelper.stop()
    }

    private suspend fun play(partIndex: Int) {
        for(measure in scorePartwise.parts[partIndex].measures){
            var elementIndex = 0
            while (elementIndex < measure.elements.size) {
                val element = measure.elements[elementIndex]
                if(element is Note) {
                    val chord = getChord(measure.elements, elementIndex)
                    startChord(chord, partIndex)
                    delay((element.duration * tickDurationTime).toLong())
                    stopChord(chord, partIndex)
                    elementIndex += if (chord.isNotEmpty()) chord.size else 1
                } else {
                    elementIndex++
                }
            }
        }
    }

    private fun getChord(elements: List<MeasureElement>, startIndex: Int): List<Byte> {
        val firstNote = elements[startIndex] as Note
        if(firstNote.rest)
            return listOf()

        val chord = ArrayList<Byte>()
        chord.add(noteToByte(firstNote))

        for (i in startIndex + 1 until elements.size) {
            val element = elements[i]
            if(element is Note && element.chord)
                chord.add(noteToByte(element))
            else
                break
        }
        return chord
    }

    private fun noteToByte(note: Note): Byte {
        val firstOctaveC = 60
        val octaveShift = (note.pitch!!.octave - 4) * 12
        val alter = note.pitch!!.alter?.toInt() ?: 0
        val noteNumber = noteNumbers[note.pitch!!.step]!!
        return (firstOctaveC + octaveShift + alter + noteNumber).toByte()
    }

    private fun startChord(chord: List<Byte>, channel: Int) {
        for (note in chord){
            midiHelper.noteOn(note, 96, channel.toByte())
        }
    }

    private fun stopChord(chord: List<Byte>, channel: Int) {
        for (note in chord){
            midiHelper.noteOff(note, channel.toByte())
        }
    }
}