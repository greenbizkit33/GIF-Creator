package com.nathanhaze.gifcreator.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.manager.Utils
import it.sephiroth.android.library.rangeseekbar.RangeSeekBar
import kotlin.math.roundToInt
import com.google.android.material.slider.LabelFormatter
import java.lang.String


class SimpleSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_setup)

        val frequencyRange = findViewById<Slider>(R.id.frame_rate)

        frequencyRange.setLabelFormatter(LabelFormatter { value -> //It is just an example
            (value/1000).toString()
        })
        val createGifButton = findViewById<Button>(R.id.btn_create_gif)

        createGifButton.setOnClickListener {
            Utils.frameFrequencyMilli = frequencyRange.value.toInt()
            startActivity(Intent(this, GifCreatorActivity::class.java))
        }

    }
}