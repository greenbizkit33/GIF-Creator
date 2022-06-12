package com.nathanhaze.gifcreator

import android.os.Environment
import androidx.multidex.MultiDexApplication
import java.io.File

class GifCreatorApp : MultiDexApplication() {


    fun getParentFolder(): File? {
        val path = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .toString() + "/GifCreator/"
        )
        if (!path.exists()) {
            path.mkdirs()
        }
        if (path == null) {
        }
        return path
    }

    fun getListFiles(): Array<String?>? {
        val fileList: Array<File> = getParentFolder()?.listFiles()
            ?: return arrayOfNulls(0)
        val length = fileList.size // null pointer
        val stringList = arrayOfNulls<String>(length)
        for (i in 0 until length) {
            stringList[i] = fileList[i].absolutePath
        }
        return stringList
    }
}