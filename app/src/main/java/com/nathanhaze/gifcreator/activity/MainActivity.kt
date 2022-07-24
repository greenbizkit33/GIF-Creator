package com.nathanhaze.gifcreator.activity


import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.gallery.PhotoGallery
import com.nathanhaze.gifcreator.manager.Utils
import mehdi.sakout.fancybuttons.FancyButton
import org.greenrobot.eventbus.EventBus


class MainActivity : AppCompatActivity() {

    private var pd: ProgressDialog? = null
    private var mAdView: AdView? = null

    private val MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 0
    private val GALLERY_INTENT_CALLED = 1

    var btnOpenFolder: FancyButton? = null
    var btnOpenInternalFolder: FancyButton? = null

    var removeAds: TextView? = null

    init {
        System.loadLibrary("NativeImageProcessor")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnVideoPicker = findViewById<View>(R.id.button_pick_video) as LinearLayout
        btnVideoPicker.setOnClickListener { importVideo() }

        btnOpenInternalFolder = findViewById<View>(R.id.button_internal) as FancyButton
        btnOpenInternalFolder!!.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, PhotoGallery::class.java)
            startActivity(intent)
        })
        val setting = findViewById<View>(R.id.iv_setting) as ImageView
        setting.setOnClickListener {
//            startActivity(
//                Intent(
//                    applicationContext,
//                    SettingActivity::class.java
//                )
//            )
        }
        pd = ProgressDialog(this)
        pd!!.setCancelable(false)
        pd!!.setCanceledOnTouchOutside(false)
        val act: Activity = this
        val llAd = findViewById<View>(R.id.ll_ads) as LinearLayout

        // Get intent, action and MIME type
        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null && type.startsWith("video/")) {
            handleSendImage(intent) // Handle single image being sent
        }
        Utils.trackScreenView(this, "Chooser Page")
        // EventBus.getDefault().register(this)
        Utils.resetValues()

        val mAdView = findViewById<View>(R.id.adView) as AdView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        btnOpenFolder?.visibility = View.GONE
        btnOpenInternalFolder?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    fun handleSendImage(intent: Intent) {
        val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (imageUri != null) {
            val value = handleUri(imageUri)
            if (!value) {
                Toast.makeText(this, resources.getString(R.string.sorry_wrong), Toast.LENGTH_LONG)
                    .show()
                Utils.trackEvent(Bundle(), "bad_image_uri", this)
            }
        }
    }

    private fun handleVideo(path: String) {
        Utils.setVideoPath(path, this)
        startActivity(Intent(this, SimpleSetupActivity::class.java))
    }

    private fun getVideoLength(path: String): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMillisec = time!!.toLong()
        retriever.release()
        return timeInMillisec
    }

    private fun importVideo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE
                )
            } else {
                getImportedVideo()
            }
        } else {
            getImportedVideo()
        }
    }

    private fun getImportedVideo() {
        val intent = Intent(Intent.ACTION_PICK, null)
        intent.type = "video/*"
        try {
            startActivityForResult(intent, GALLERY_INTENT_CALLED)
        } catch (ex: ActivityNotFoundException) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            val builder1 = AlertDialog.Builder(this)
            builder1.setMessage(resources.getString(R.string.no_photo_gallery_app))
            builder1.setCancelable(true)
            builder1.setPositiveButton(
                resources.getString(R.string.ok)
            ) { dialog, id ->
                dialog.cancel()
                finish()
            }
            val alert11 = builder1.create()
            alert11.show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (null == data || resultCode != RESULT_OK || null == data) {
            return
        }
        val selectedImage = data.data
        handleUri(selectedImage)
    }

    private fun handleUri(selectedImage: Uri?): Boolean {
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null
        cursor = try {
            contentResolver.query(
                selectedImage!!,
                filePathColumn, null, null, null
            )
        } catch (ex: Exception) {
            return false
        }
        if (cursor == null || cursor.count < 1) {
            return false // no cursor or no record. DO YOUR ERROR HANDLING
        }
        cursor.moveToFirst()
        val columnIndex = cursor.getColumnIndex(filePathColumn[0])
        if (columnIndex < 0) { // no column index
            return false
        }
        val path = cursor.getString(columnIndex)
        handleVideo(path)
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    getImportedVideo()
                }
            }
        }
    }
}