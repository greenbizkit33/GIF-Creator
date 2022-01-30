package com.nathanhaze.gifcreator.manager

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ImageUtil {

    fun saveBitmap(image: Bitmap, context: Context): String {
        var path = ""
        val imageFile: File =
            getOutputMediaFile(context)
        var out: FileOutputStream? = null

        try {
            path = imageFile.absolutePath
            out = FileOutputStream(path)
            image.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (e: Exception) {
        } finally {
            try {
                out?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (path != null && !path.isEmpty()) {
            scanMedia(path, context)
        }
        return path
    }

    private fun getOutputMediaFile(context: Context): File {
        val mediaStorageDir: File = nathanhaze.com.videoediting.ImageUtil.getRootDirectory()

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }

        var mediaFile: File? = null
        var extension = ".jpeg"
        if (VideoEditingApp.getInstance().getSavePng()) {
            extension = ".png"
        }
        if (VideoEditingApp.getInstance().useFileInt()) {
            val index = String.format("%07d", VideoEditingApp.getInstance().getLastFileInt())
            VideoEditingApp.getInstance().increamentFileInt()
            mediaFile = File(
                mediaStorageDir.path + File.separator +
                        index + extension
            )
        } else {
            // Create a media file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            mediaFile = File(
                (mediaStorageDir.path + File.separator +
                        "IMG_" + timeStamp + append + extension)
            )
        }
        return mediaFile
    }

    private fun scanMedia(path: String, context: Context) {
        val file = File(path)
        val uri = Uri.fromFile(file)
        val scanFileIntent = Intent(
            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri
        )
        context.sendBroadcast(scanFileIntent)
    }
}