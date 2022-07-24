package com.nathanhaze.gifcreator.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import com.nathanhaze.gifcreator.AnimatedGifEncoder
import com.nathanhaze.gifcreator.event.GifCreationEvent
import com.nathanhaze.gifcreator.event.ProgressUpdateEvent
import org.greenrobot.eventbus.EventBus
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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
        val extension = ".png"

        val index = String.format("%07d", Utils.getLastFileInt(activty))
        Utils.incrementFileInt(activty)
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

    fun saveGif(bitmaps: ArrayList<Bitmap>, context: Context?) {
        var filePath: File? = null
        try {
            EventBus.getDefault()
                .post(ProgressUpdateEvent("Saving GIF", Utils.endTimeMilli, false, false))
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val file: File = File(getPath())
            if (!file.exists()) {
                file.mkdir()
            }

            filePath = File(
                getPath(),
                "GIF_$timeStamp.gif"
            )

            filePath.createNewFile()
            val outStream = FileOutputStream(filePath)
            outStream.write(bitmaps.let { generateGIF(it) })
            outStream.close()
            val event = GifCreationEvent(filePath, false)
            Utils.lastGifFilePath = filePath
            EventBus.getDefault().post(event)
            context?.let { scanMedia(filePath.absolutePath, it) }
        } catch (e: Exception) {
            filePath?.delete()
            EventBus.getDefault().post(
                ProgressUpdateEvent(
                    "Something bad happen " + e.localizedMessage,
                    Utils.endTimeMilli,
                    false,
                    false
                )
            )
            EventBus.getDefault().post(GifCreationEvent(null, true))
            e.printStackTrace()
        }
    }

    fun generateGIF(bitmaps: ArrayList<Bitmap>): ByteArray? {
        EventBus.getDefault().post(
            ProgressUpdateEvent(
                "Have images generating GIF",
                Utils.endTimeMilli - 100,
                false,
                false
            )
        )
        if (bitmaps.isEmpty()) {
            EventBus.getDefault().post(
                ProgressUpdateEvent(
                    "Images was empty",
                    Utils.endTimeMilli - 200,
                    false,
                    false
                )
            )
            return null
        }
        val bos = ByteArrayOutputStream()
        val encoder = AnimatedGifEncoder()
        encoder.setDelay(Utils.frameFrequencyMilli)
        encoder.start(bos)
        var index = 0
        for (bitmap in bitmaps) {
            val worked = encoder.addFrame(bitmap)
            if (!worked) {
                break
            }
            index++
            EventBus.getDefault().post(
                ProgressUpdateEvent(
                    "Adding Image " + index + " of " + bitmaps.size + " to GIF",
                    index,
                    false,
                    true
                )
            )

            bitmap.recycle()
        }
        encoder.finish()
        EventBus.getDefault()
            .post(ProgressUpdateEvent("Done Generating GIF", Utils.endTimeMilli, false, false))
        return bos.toByteArray()
    }
}