package com.nathanhaze.gifcreator

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
import com.amazon.device.ads.AdLayout
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import mehdi.sakout.fancybuttons.FancyButton
import org.greenrobot.eventbus.EventBus

class MainActivity : AppCompatActivity() {

    private var pd: ProgressDialog? = null
    private var app: GifCreatorApp? = null
    private var amazonAd: AdLayout? = null
    private var mAdView: AdView? = null

    private val TAG = "FFmpeg"

    private val MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 0
    private val GALLERY_INTENT_CALLED = 1

    var btnOpenFolder: FancyButton? = null
    var btnOpenInternalFolder: FancyButton? = null

    var removeAds: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //MobileAds.initialize(this, getResources().getString(R.string.admob_account));
        MobileAds.initialize(this, object : OnInitializationCompleteListener() {
            fun onInitializationComplete(initializationStatus: InitializationStatus?) {}
        })
        app = GifCreatorApp().getInstance()
        val btnVideoPicker = findViewById<View>(R.id.button_pick_video) as LinearLayout
        btnVideoPicker.setOnClickListener { importVideo() }

        btnOpenInternalFolder = findViewById<View>(R.id.button_internal) as FancyButton
        btnOpenInternalFolder!!.setOnClickListener(View.OnClickListener {
//            val intent = Intent(app.getApplicationContext(), PhotoGallery::class.java)
//            startActivity(intent)
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
        amazonAd = findViewById<View>(R.id.adview_amazon) as AdLayout
        removeAds = findViewById<View>(R.id.tv_remove_ads) as TextView
        val act: Activity = this
        removeAds!!.setOnClickListener { startActivity(Intent(act, GoingProActivity::class.java)) }
        val llAd = findViewById<View>(R.id.ll_ads) as LinearLayout
        if (!VideoEditingApp.getInstance().getPurchased()) {
            llAd.visibility = View.VISIBLE
            mAdView = app.getAdmobAd(this)
            removeAds!!.visibility = View.VISIBLE
            llAd.addView(mAdView, 0)
            mAdView.setAdListener(object : AdListener() {
                fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    mAdView.setVisibility(View.GONE)
                    amazonAd.setVisibility(View.VISIBLE)
                    app.showAmazonAds(amazonAd)
                }
            })
        } else {
            llAd.visibility = View.GONE
            if (mAdView != null) {
                mAdView.setVisibility(View.GONE)
            }
            removeAds!!.visibility = View.GONE
        }

        // Get intent, action and MIME type
        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null && type.startsWith("video/")) {
            handleSendImage(intent) // Handle single image being sent
        }
        app.trackScreenView(this, "Chooser Page")
        EventBus.getDefault().register(this)
    }

//    @Subscribe
//    fun onEvent(event: PurchasedAppEvent?) {
//        if (removeAds != null) {
//            removeAds!!.visibility = View.GONE
//        }
//        if (amazonAd != null) {
//            amazonAd.setVisibility(View.GONE)
//        }
//        if (mAdView != null) {
//            mAdView.setVisibility(View.GONE)
//        }
//    }


    override fun onResume() {
        super.onResume()
        if (btnOpenFolder != null) {
            if (app == null) {
                app = VideoEditingApp.getInstance()
            }
            //  if (!app.doesFolderExist()) {
            //    btnOpenFolder.setVisibility(View.GONE);
            //     btnOpenInternalFolder.setVisibility(View.GONE);
            //   } else {
            btnOpenFolder.setVisibility(View.GONE)
            btnOpenInternalFolder.setVisibility(View.VISIBLE)
            //  }
        }
        if (app != null) {
            app.isInBackground = false
        }
        VideoManager.getInstance().cleanUp()
    }

    override fun onDestroy() {
        if (amazonAd != null) {
            amazonAd.destroy()
        }
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
                VideoEditingApp.getInstance().trackEvent(Bundle(), "bad_image_uri")
            }
        }
    }

    private fun handleVideo(path: String) {
        app.setVideoPath(path)
        startActivity(Intent(this, VideoViewActivity::class.java))
        /*
        try {
            String workFolder = getApplicationContext().getFilesDir().getAbsolutePath();
            int startMs = 0;
            Log.d(TAG, " Duration " + getVideoLength(path));
            int endMs = (int) getVideoLength(path);
            File moviesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES
            );
            File destDir = new File(moviesDir, "stuff");

            String pathExport = ImageUtil.getSavePath("%03d", this);

            String[] complexCommand = {"-y", "-i", path, "-an", "-r", "1", "-ss", "" + startMs / 1000, "-t", "" + (endMs - startMs) / 1000, pathExport};

            runCommand(complexCommand);
            Log.i(TAG, "ffmpeg4android finished successfully");
        } catch (Throwable e) {
            Log.e(TAG, "vk run exception.", e);
        }
        */
    }

    private fun getVideoLength(path: String): Long {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMillisec = time!!.toLong()
        retriever.release()
        return timeInMillisec
    }

    private fun showSettings() {
//        Intent intent = new Intent(this, SettingActivity.class);
//        startActivity(intent);
    }

    fun importVideo() {
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