package com.nathanhaze.gifcreator.manager

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VideoExportUtil {

    /**
     * Trims [inputPath] from [startMs] to [endMs] and writes an MP4 to the GifCreator folder.
     * Returns the output file path on success, null on failure.
     */
    fun trimToMp4(context: Context, inputPath: String, startMs: Long, endMs: Long): File? {
        val outputDir = File(
            android.os.Environment
                .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES),
            "GifCreator"
        )
        if (!outputDir.exists()) outputDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val outputFile = File(outputDir, "CLIP_$timeStamp.mp4")

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(inputPath)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackMap = mutableMapOf<Int, Int>()
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    trackMap[i] = muxer.addTrack(format)
                }
            }

            muxer.start()
            extractor.seekTo(startMs * 1_000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()
            while (true) {
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endMs * 1_000L) break

                info.presentationTimeUs = sampleTimeUs - startMs * 1_000L
                info.flags = extractor.sampleFlags
                val srcTrack = extractor.sampleTrackIndex
                trackMap[srcTrack]?.let { dst ->
                    muxer.writeSampleData(dst, buffer, info)
                }
                extractor.advance()
            }

            muxer.stop()
            scanMedia(context, outputFile)
            outputFile
        } catch (e: Exception) {
            outputFile.delete()
            null
        } finally {
            muxer?.release()
            extractor.release()
        }
    }

    private fun scanMedia(context: Context, file: File) {
        context.sendBroadcast(
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file))
        )
    }
}
