package com.nathanhaze.gifcreator.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.manager.Utils
import it.sephiroth.android.library.rangeseekbar.RangeSeekBar
import kotlin.math.roundToInt

class SimpleSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_setup)

        val frequencyRange = findViewById<RangeSeekBar>(R.id.frame_rate)

        val createGifButton = findViewById<Button>(R.id.btn_create_gif)

        createGifButton.setOnClickListener {
            Utils.frameFrequency = frequencyRange.keyProgressIncrement.roundToInt()
            startActivity(Intent(this, GifCreatorActivity::class.java))
        }

    }
}