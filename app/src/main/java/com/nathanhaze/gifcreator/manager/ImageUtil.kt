package com.nathanhaze.gifcreator.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import com.nathanhaze.gifcreator.AnimatedGifEncoder
import com.nathanhaze.gifcreator.event.GifCreationEvent
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object ImageUtil {

    fun saveBitmap(image: Bitmap, activty: Activity): String {
        var path = ""
        val imageFile: File =
            getOutputMediaFile(activty) ?: return ""
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
            scanMedia(path, activty)
        }
        return path
    }

    private fun getOutputMediaFile(activty: Activity): File? {
        val mediaStorageDir: File = getRootDirectory()

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }

        var mediaFile: File? = null
        var extension = ".png"

        val index = String.format("%07d", Utils.getLastFileInt(activty))
        Utils.increamentFileInt(activty)
        mediaFile = File(
            mediaStorageDir.path + File.separator +
                    index + extension
        )
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

    private fun getPath(): String? {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .toString() + "/GifCreator/"
    }

    private fun getRootDirectory(): File {
        return File(getPath())
    }

    fun saveGif(fileNames: ArrayList<String>, context: Context?) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val filePath: File = File(
                getPath(),
                "GIF_$timeStamp.gif"
            )
            val outStream = FileOutputStream(filePath)
            outStream.write(fileNames?.let { generateGIF(it) })
            outStream.close()
            val event = GifCreationEvent(filePath)
            EventBus.getDefault().post(event)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateGIF(fileNames: ArrayList<String>): ByteArray? {
        val bitmaps = ArrayList<Bitmap>()
        for (name in fileNames) {
            val image = File(name)
            val bmOptions = BitmapFactory.Options()
            val bitmap = BitmapFactory.decodeFile(image.absolutePath, bmOptions)
            if (bitmap != null) {
                bitmaps.add(bitmap)
            }
        }
        if (bitmaps.isEmpty()) {
            return null
        }
        val bos = ByteArrayOutputStream()
        val encoder = AnimatedGifEncoder()
        encoder.start(bos)
        for (bitmap in bitmaps) {
            encoder.addFrame(bitmap)
        }
        encoder.finish()
        return bos.toByteArray()
    }
}