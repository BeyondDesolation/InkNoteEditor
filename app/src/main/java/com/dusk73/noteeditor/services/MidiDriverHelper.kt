package com.dusk73.noteeditor.services

import android.util.Log
import org.billthefarmer.mididriver.GeneralMidiConstants
import org.billthefarmer.mididriver.MidiConstants
import org.billthefarmer.mididriver.MidiDriver

class MidiDriverHelper : MidiDriver.OnMidiStartListener {
    private val TAG = "MidiDriverHelper"
    private var midi: MidiDriver? = null

    private var instruments: HashMap<String, Byte>
    var instrumentNames: List<String>

    init {
        Log.i(TAG, "INIT")
        midi = MidiDriver.getInstance()
        instruments = HashMap()

        val instTemp = GeneralMidiConstants::class.java.declaredFields
        instrumentNames = instTemp.map { f -> f.name }
        for (inst in instTemp) {
            instruments[inst.name] = inst.getByte(inst)
        }
    }

    fun noteOn(note: Byte, velocity: Byte, channel: Byte = 0) {
        val m = (MidiConstants.NOTE_ON + channel).toByte()
        sendMidi(m, note, velocity)
    }

    fun noteOff(note: Byte, channel: Byte = 0) {
        val m = (MidiConstants.NOTE_OFF + channel).toByte()
        sendMidi(m, note, 0)
    }

    fun changePitch(value: Byte) {
        sendMidi(MidiConstants.PITCH_BEND, value, value)
    }

    fun changeInstrument(nInstrument: Byte) {
        sendMidi(MidiConstants.PROGRAM_CHANGE, nInstrument)
    }

    fun changeInstrumentByName(instrumentName: String): Byte {
        sendMidi(MidiConstants.PROGRAM_CHANGE, instruments[instrumentName]!!)
        return instruments[instrumentName]!!
    }

    fun start() {
        midi?.start()
    }

    fun stop() {
        midi?.stop()
    }

    override fun onMidiStart() {
        sendMidi(
            MidiConstants.PROGRAM_CHANGE,
            GeneralMidiConstants.ACOUSTIC_GRAND_PIANO
        )

        val config = midi!!.config()

        val info = "maxVoices: ${config[0]} numChannels: ${config[1]} sampleRate: ${config[2]} mixBufferSize: ${config[3]}"
        Log.i(TAG, info)

    }

    private fun sendMidi(m: Byte, n: Byte) {
        val msg = ByteArray(2)
        msg[0] = m
        msg[1] = n
        midi!!.write(msg)
    }

    private fun sendMidi(m: Byte, n: Byte, v: Byte) {
        val msg = ByteArray(3)
        msg[0] = m
        msg[1] = n
        msg[2] = v
        midi!!.write(msg)
    }
}