package com.nathanhaze.gifcreator.gallery

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.nathanhaze.gifcreator.GifCreatorApp
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.event.RefeshGalleryEvent
import com.nathanhaze.gifcreator.gallery.PictureFragment.Companion.newInstance
import org.greenrobot.eventbus.EventBus
import java.io.File

class PhotoPagerActivity : FragmentActivity() {
    private var viewPager: ViewPager2? = null
    var application: GifCreatorApp? = null
    private var path: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_pager)
        viewPager = findViewById(R.id.view_pager)
        application = getApplication() as GifCreatorApp
        val intent = intent
        val bundle = intent.extras
        path = ""
        if (bundle != null) {
            path = bundle.getString("path")
        }
        if (path == null || path!!.isEmpty()) {
            return
        }
        val fileList: List<String?>? = application?.getListFiles()
        if (fileList == null || fileList.isEmpty()) {
            return
        }
        if (path != null) {
            index = fileList.indexOf(path)
        }
        viewPager?.adapter = ScreenSlidePagerAdapter(this, application?.getListFiles())
        viewPager?.currentItem = index
        val shareIcon = findViewById<ImageView>(R.id.iv_share)
        shareIcon.setOnClickListener { _: View? ->
            share(viewPager?.currentItem?.let {
                fileList[it]
            })
        }
        val ivOpen = findViewById<ImageView>(R.id.iv_open)
        ivOpen.setOnClickListener {
            val photo = path?.let { it1 -> File(it1) }
            val photoURI = photo?.let { it1 ->
                FileProvider.getUriForFile(
                    ivOpen.context,
                    ivOpen.context.packageName + ".GenericFileProvider",
                    it1
                )
            }
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) //must provide
            intent.data = photoURI
            startActivity(intent)
        }
        val act: FragmentActivity = this
        val deleteIcon = findViewById<ImageView>(R.id.iv_delete)
        deleteIcon.setOnClickListener { _: View? ->
            val temp: List<String?>? = application?.getListFiles()
            var file: File? = null
            try {
                file = viewPager?.currentItem?.let { temp?.get(it) }?.let { File(it) }
            } catch (ex: IndexOutOfBoundsException) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                try {
                    Toast.makeText(
                        deleteIcon.context,
                        resources.getString(R.string.problem_deleting),
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
            if (file == null) {
                Toast.makeText(
                    deleteIcon.context,
                    resources.getString(R.string.problem_deleting),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            val deleted = file.delete()
            if (deleted) {
                deleteIcon.context.sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(file)
                    )
                )
                val temp2: List<String?>? = application?.getListFiles()
                viewPager?.adapter = ScreenSlidePagerAdapter(act, temp2)
                EventBus.getDefault().post(RefeshGalleryEvent())
                val currentItem = viewPager?.currentItem
                if (currentItem != null) {
                    if (currentItem < temp2!!.size) {
                        viewPager?.currentItem = currentItem
                    } else {
                        finish()
                    }
                }
            } else {
                Toast.makeText(
                    deleteIcon.context,
                    resources.getString(R.string.problem_deleting),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val decorView = window.decorView
            val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            decorView.systemUiVisibility = uiOptions
        } else {
            val decorView = window.decorView
            val uiOptions = View.SYSTEM_UI_FLAG_VISIBLE
            decorView.systemUiVisibility = uiOptions
        }
    }

    private fun share(path: String?) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        shareIntent.type = "image/jpeg"
        val photo = path?.let { File(it) }
        val photoURI = photo?.let {
            FileProvider.getUriForFile(this, this.packageName + ".GenericFileProvider", photo)
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI)
        startActivity(Intent.createChooser(shareIntent, resources.getString(R.string.share)))
    }

    private class ScreenSlidePagerAdapter(
        fa: FragmentActivity?,
        private var pictures: List<String?>?
    ) : FragmentStateAdapter(
        fa!!
    ) {
        override fun createFragment(position: Int): Fragment {
            return newInstance(pictures!![position])
        }

        fun setPictures(pictures: List<String?>?) {
            this.pictures = pictures
        }

        override fun getItemCount(): Int {
            return pictures!!.size
        }
    }

    companion object {
        var index = 0
    }
}