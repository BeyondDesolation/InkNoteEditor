package com.dusk73.noteeditor.view.editor

import android.annotation.SuppressLint
import android.os.Build
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dusk73.noteeditor.custom_view.models.TouchInfo
import com.dusk73.noteeditor.services.ScorePlayer
import com.dusk73.musicxmltools.MusicEditor
import com.dusk73.musicxmltools.enums.Accidental
import com.dusk73.musicxmltools.enums.NoteType
import com.dusk73.musicxmltools.models.Key
import com.dusk73.musicxmltools.models.ScorePart
import com.dusk73.musicxmltools.models.Time
import com.dusk73.musicxmltools.models.tools.ElementPosition
import com.dusk73.musicxmltools.xml.MusicXmlSerializer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class EditorViewModel : ViewModel() {

    private val musicXmlSerializer = MusicXmlSerializer()
    private val scorePlayer = ScorePlayer()
    val musicEditor = MusicEditor()

    private var _isPlaying: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPlaying get(): LiveData<Boolean> = _isPlaying

    init {
        scorePlayer.setScorePartwise(musicEditor.scorePartwise)
    }

    fun save() {
        val file = createFile()
        musicXmlSerializer.serialize(file, musicEditor.scorePartwise)
    }

    fun addNote(touchInfo: TouchInfo?, type: NoteType): Boolean {
        if (touchInfo == null)
            return false

        musicEditor.addNote(
            ElementPosition(
                touchInfo.partIndex,
                touchInfo.measureIndex,
                touchInfo.nearestElement.elementIndex
            ),
            touchInfo.line,
            type
        )
        return true
    }

    fun setRest(touchInfo: TouchInfo?): Boolean {
        if (touchInfo == null || !touchInfo.isElementTouched)
            return false

        return musicEditor.setRest(
            touchInfo.partIndex,
            touchInfo.measureIndex,
            touchInfo.nearestElement.elementIndex
        )
    }

    fun addPart(touchInfo: TouchInfo?): Boolean {
        val id = "P" + musicEditor.scorePartwise.parts.size.toString()
        musicEditor.addPart(ScorePart(id, id + "N"))
        return true
    }

    fun addMeasure(touchInfo: TouchInfo?): Boolean {
        musicEditor.addMeasure(musicEditor.scorePartwise.parts[0].measures.size)
        return true
    }

    fun deleteMeasure(touchInfo: TouchInfo?): Boolean {
        if (touchInfo == null)
            return false
        musicEditor.deleteMeasure(touchInfo.measureIndex)
        return true
    }

    fun deletePart(touchInfo: TouchInfo?): Boolean {
        if (touchInfo == null)
            return false
        musicEditor.deletePart(touchInfo.partIndex)
        return true
    }

    fun deleteNote(touchInfo: TouchInfo?): Boolean {
        if (touchInfo == null)
            return false

        if (!touchInfo.isElementTouched)
            return false

        return musicEditor.deleteNote(
            touchInfo.partIndex,
            touchInfo.measureIndex,
            touchInfo.nearestElement.elementIndex
        )
    }

    fun changeAccidental(touchInfo: TouchInfo?, value: Accidental): Boolean {
        if (touchInfo == null)
            return false

        if (touchInfo.isElementTouched) {
            return musicEditor.updateAccidental(
                touchInfo.partIndex,
                touchInfo.measureIndex,
                touchInfo.nearestElement.elementIndex,
                value
            )
        }

        return changeFifths(touchInfo, value)
    }

    private fun changeFifths(touchInfo: TouchInfo, value: Accidental?): Boolean {
        if (value == Accidental.NATURAL)
            return false

        val alterValue = when (value) {
            Accidental.SHARP -> 1f
            Accidental.FLAT -> -1f
            else -> null
        }

        val time: Time?
        val measure =
            musicEditor.scorePartwise.parts[touchInfo.partIndex].measures[touchInfo.measureIndex]

        if (measure.attributes == null) {
            if (alterValue != null)
                musicEditor.updateMeasureAttributes(
                    touchInfo.measureIndex,
                    Key(alterValue.toInt()),
                    null
                )
        } else {
            time = measure.attributes!!.time

            if (measure.attributes!!.key == null) {
                if (alterValue == null)
                    return false

                musicEditor.updateMeasureAttributes(
                    touchInfo.measureIndex,
                    Key(alterValue.toInt()),
                    time
                )
            } else {
                if (alterValue != null) {
                    val fifths = measure.attributes!!.key!!.fifths + alterValue.toInt()
                    if (abs(fifths) > 6)
                        return false

                    musicEditor.updateMeasureAttributes(touchInfo.measureIndex, Key(fifths), time)
                } else {
                    musicEditor.updateMeasureAttributes(touchInfo.measureIndex, null, time)
                }
            }
        }

        return true
    }

    fun startPlaying() {
        if(_isPlaying.value!!) {
            _isPlaying.value = false
            scorePlayer.stop()
        }else{
            _isPlaying.value = true
            scorePlayer.start()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun createFile(): File {
        val folderName = "DNoteEditor"
        val df = SimpleDateFormat("dd-MM-yyyy HH-mm-ss")

        val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
                ).toString() + "/" + folderName
            )
        } else {
            File(Environment.getExternalStorageDirectory().toString() + "/" + folderName)
        }

        if (!dir.exists()) {
            val success: Boolean = dir.mkdirs()
            if (!success) {
                throw java.lang.Exception("Exception during directory creation")
            }
        }
        return File(dir, df.format(Calendar.getInstance().time) + ".musicxml")
    }
}