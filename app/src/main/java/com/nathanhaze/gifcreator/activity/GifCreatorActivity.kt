package com.nathanhaze.gifcreator.activity

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.manager.ImageUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GifCreatorActivity : AppCompatActivity() {


    private val PERMISSION_EXTRCT = 0

    val executorService: ExecutorService = Executors.newFixedThreadPool(4)
    val mainThreadHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())


    fun getImages(filePath: String) {
//        executorService.execute {
//            try {
//                val response = makeSynchronousLoginRequest(jsonBody)
//                callback(response)
//            } catch (e: Exception) {
//                val errorResult = Result.Error(e)
//                callback(errorResult)
//            }
//        }

        Executors.newSingleThreadExecutor().execute {
            // todo: background tasks
            val mediaRetriever: MediaMetadataRetriever = MediaMetadataRetriever()
            mediaRetriever.setDataSource(filePath)

            val extractionType = MediaMetadataRetriever.OPTION_CLOSEST

            val videoLength = 9
            var currentFrame = 0L
            while (currentFrame < videoLength) {
                val bitmap = mediaRetriever.getFrameAtTime(
                    TimeUnit.MILLISECONDS.toMicros(currentFrame),
                    extractionType
                )
                bitmap?.let {
                    ImageUtil.saveBitmap(bitmap, this)
                }
                currentFrame++
            }

            runOnUiThread {

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif_creator)
    }


    fun extractPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_EXTRCT
                )
            } else {
                getImages("")
            }
        } else {
            getImages("")
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_EXTRCT -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    getImages("")
                }
            }
        }
    }
}