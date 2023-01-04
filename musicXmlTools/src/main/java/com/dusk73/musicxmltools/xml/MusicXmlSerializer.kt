package com.dusk73.musicxmltools.xml

import android.util.Xml
import java.io.File
import java.io.FileOutputStream

class MusicXmlSerializer {

    fun serialize(file: File) {

        val serializer = Xml.newSerializer()

        val fos = FileOutputStream(file)
        fos.use {

            serializer.setOutput(fos,"UTF-8")
            serializer.startDocument("UTF-8",true)
            serializer.endDocument()
            serializer.flush()
        }
    }
}