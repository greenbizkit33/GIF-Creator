package com.nathanhaze.gifcreator.activity

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
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

        val tvFreq = findViewById<TextView>(R.id.tv_freq)

        val frequencyRange = findViewById<Slider>(R.id.frame_rate)

        frequencyRange.addOnChangeListener { slider, value, fromUser ->
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            df.format(value / 1000).toString()

            tvFreq.setText(getText(R.string.range_frequency).toString() + " " + df.format(value / 1000).toString())
        }

        frequencyRange.setLabelFormatter(LabelFormatter { value -> //It is just an example
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            df.format(value / 1000).toString()
        })

     //   frequencyRange.value = .1F

        val rangeSlider = findViewById<RangeSlider>(R.id.range_time)

        val mediaRetriever: MediaMetadataRetriever = MediaMetadataRetriever()
        mediaRetriever.setDataSource(Utils.getVideoPath(this))

        val extractionType = MediaMetadataRetriever.OPTION_CLOSEST

        val time: String? =
            mediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val videoLengthMilli: Float = time?.toFloat() ?: 0F

//        rangeSlider.valueFrom = 0F
//        if (videoLengthMilli != null) {
//          //  rangeSlider.valueTo = videoLengthMilli
//            rangeSlider.valueTo = 10F
//        }

        Utils.startTimeMilli = 0
        Utils.endTimeMilli = videoLengthMilli.toInt()

        rangeSlider.setValues(0.0f, 1.0f);

        val tvStart = findViewById<TextView>(R.id.tv_start_time)
        val tvEnd = findViewById<TextView>(R.id.tv_end_time)


        val end = String.format(
            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(videoLengthMilli.toLong()),
            TimeUnit.MILLISECONDS.toMinutes(videoLengthMilli.toLong()) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(videoLengthMilli.toLong()) % TimeUnit.MINUTES.toSeconds(1)
        )

        tvEnd.setText(end)

        rangeSlider.setLabelFormatter(LabelFormatter { value -> //It is just an example
            val milli = value.times(videoLengthMilli).toLong()

            String.format(
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(milli.toLong()),
                TimeUnit.MILLISECONDS.toMinutes(milli.toLong()) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(milli.toLong()) % TimeUnit.MINUTES.toSeconds(1)
            )
        })

        rangeSlider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: RangeSlider) {

            }

            override fun onStopTrackingTouch(slider: RangeSlider) {
                val values = rangeSlider.values

                val startMilli = values[0].times(videoLengthMilli).toLong()
                val start = String.format(
                    "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(startMilli),
                    TimeUnit.MILLISECONDS.toMinutes(startMilli) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(startMilli) % TimeUnit.MINUTES.toSeconds(1)
                )

                val endMilli = values[1].times(videoLengthMilli).toLong()

                val end = String.format(
                    "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(endMilli),
                    TimeUnit.MILLISECONDS.toMinutes(endMilli) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(endMilli) % TimeUnit.MINUTES.toSeconds(1)
                )
                tvStart.setText(start)
                tvEnd.setText(end)

                Utils.startTimeMilli = startMilli.toInt()
                Utils.endTimeMilli = endMilli.toInt()
            }


        })

        val createGifButton = findViewById<Button>(R.id.btn_create_gif)

        createGifButton.setOnClickListener {
            Utils.frameFrequencyMilli = frequencyRange.value.toInt()
            startActivity(Intent(this, GifCreatorActivity::class.java))
        }

    }
}