package com.nathanhaze.gifcreator.gallery

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.gallery.GalleryFragment
import androidx.core.app.NavUtils


class PhotoGallery : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_photo_gallery)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.frame_main, GalleryFragment())
        ft.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}