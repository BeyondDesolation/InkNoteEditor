package com.dusk73.dnoteeditor.view.editor

import android.annotation.SuppressLint
import android.os.Build
import android.os.Environment
import androidx.lifecycle.ViewModel
import com.dusk73.musicxmltools.xml.MusicXmlSerializer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditorViewModel : ViewModel() {

    private val musicXmlSerializer = MusicXmlSerializer()

    fun save() {
        val file = createFile()
        musicXmlSerializer.serialize(file)
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
        return File(dir, df.format(Calendar.getInstance().time) + ".xml")
    }
}