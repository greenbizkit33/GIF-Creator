package com.nathanhaze.gifcreator.activity

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.databinding.ActivitySimpleSetupBinding
import com.nathanhaze.gifcreator.manager.Utils
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit


class SimpleSetupActivity : AppCompatActivity() {

    private var binding: ActivitySimpleSetupBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_setup)

     //   binding = ActivitySimpleSetupBinding.inflate(layoutInflater)

        val frequencyRange = findViewById<Slider>(R.id.frame_rate)

        frequencyRange.setLabelFormatter(LabelFormatter { value -> //It is just an example
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            df.format(value / 1000).toString()
        })

        val rangeSlider = findViewById<RangeSlider>(R.id.range_time)

        val mediaRetriever: MediaMetadataRetriever = MediaMetadataRetriever()
        mediaRetriever.setDataSource(Utils.getVideoPath(this))

        val extractionType = MediaMetadataRetriever.OPTION_CLOSEST

        val time: String? =
            mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val videoLengthMilli : Float = time?.toFloat() ?: 0F

//        rangeSlider.valueFrom = 0F
//        if (videoLengthMilli != null) {
//          //  rangeSlider.valueTo = videoLengthMilli
//            rangeSlider.valueTo = 10F
//        }

        rangeSlider.setValues(0.0f,1.0f);

        val tvStart = findViewById<TextView>(R.id.tv_start_time)
        val tvEnd = findViewById<TextView>(R.id.tv_end_time)

        rangeSlider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener{
            override fun onStartTrackingTouch(slider: RangeSlider) {

            }

            override fun onStopTrackingTouch(slider: RangeSlider) {
                val values = rangeSlider.values
                tvStart.setText("" + values[0].times(videoLengthMilli))
                tvEnd.setText("" + values[1].times(videoLengthMilli))
            }


        })

        val createGifButton = findViewById<Button>(R.id.btn_create_gif)

        createGifButton.setOnClickListener {
            Utils.frameFrequencyMilli = frequencyRange.value.toInt()
            startActivity(Intent(this, GifCreatorActivity::class.java))
        }

    }
}