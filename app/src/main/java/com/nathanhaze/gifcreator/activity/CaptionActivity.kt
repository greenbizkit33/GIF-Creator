package com.nathanhaze.gifcreator.activity

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.manager.Utils

class CaptionActivity : AppCompatActivity() {

    private val colorOptions = listOf(
        "White" to Color.WHITE,
        "Black" to Color.BLACK,
        "Yellow" to Color.YELLOW,
        "Red" to Color.RED,
        "Cyan" to Color.CYAN
    )

    private lateinit var tvPreview: TextView
    private lateinit var bgBar: View
    private lateinit var cgStyle: ChipGroup
    private var selectedColor: Int = Utils.captionColor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_caption)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvPreview = findViewById(R.id.tv_caption_preview)
        bgBar = findViewById(R.id.caption_bg_bar)
        val etCaption = findViewById<TextInputEditText>(R.id.et_caption)
        val cgPosition = findViewById<ChipGroup>(R.id.cg_position)
        cgStyle = findViewById(R.id.cg_style)
        val sliderSize = findViewById<Slider>(R.id.slider_caption_size)
        val switchBg = findViewById<SwitchCompat>(R.id.switch_caption_bg)
        val llColors = findViewById<LinearLayout>(R.id.ll_colors)

        // Restore current Utils values
        etCaption.setText(Utils.captionText)
        sliderSize.value = (Utils.captionSizeMultiplier * 100).toInt().toFloat().coerceIn(4f, 15f)
        switchBg.isChecked = Utils.captionBackground
        selectedColor = Utils.captionColor

        cgPosition.check(
            when (Utils.captionPosition) {
                1 -> R.id.chip_pos_center
                2 -> R.id.chip_pos_top
                else -> R.id.chip_pos_bottom
            }
        )

        cgStyle.check(
            when (Utils.captionStyle) {
                1 -> R.id.chip_style_bold
                2 -> R.id.chip_style_outline
                else -> R.id.chip_style_normal
            }
        )

        // Color swatches
        buildColorSwatches(llColors)

        // Live preview updates
        fun updatePreview() {
            val text = etCaption.text?.toString()?.takeIf { it.isNotBlank() } ?: getString(R.string.add_caption)
            val sizeSp = sliderSize.value * 1.8f
            val hasBg = switchBg.isChecked
            val positionId = cgPosition.checkedChipId

            tvPreview.text = text
            tvPreview.textSize = sizeSp
            tvPreview.setTextColor(selectedColor)

            // Apply style to preview
            when (cgStyle.checkedChipId) {
                R.id.chip_style_bold -> {
                    tvPreview.typeface = Typeface.DEFAULT_BOLD
                    tvPreview.setShadowLayer(4f, 1f, 1f, Color.BLACK)
                    tvPreview.paint.style = android.graphics.Paint.Style.FILL
                }
                R.id.chip_style_outline -> {
                    tvPreview.typeface = Typeface.DEFAULT_BOLD
                    // Simulate outline: dark shadow offset + contrasting stroke approximation
                    tvPreview.setShadowLayer(6f, 0f, 0f, invertColor(selectedColor))
                    tvPreview.paint.style = android.graphics.Paint.Style.FILL
                }
                else -> {
                    tvPreview.typeface = Typeface.DEFAULT
                    tvPreview.setShadowLayer(4f, 1f, 1f, Color.BLACK)
                    tvPreview.paint.style = android.graphics.Paint.Style.FILL
                }
            }

            bgBar.visibility = if (hasBg) View.VISIBLE else View.GONE

            val gravity = when (positionId) {
                R.id.chip_pos_top -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                R.id.chip_pos_center -> Gravity.CENTER
                else -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            (tvPreview.layoutParams as FrameLayout.LayoutParams).gravity = gravity
            (bgBar.layoutParams as FrameLayout.LayoutParams).gravity =
                if (positionId == R.id.chip_pos_top) Gravity.TOP else Gravity.BOTTOM
            tvPreview.requestLayout()
            bgBar.requestLayout()
        }

        etCaption.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) = updatePreview()
        })

        cgPosition.setOnCheckedStateChangeListener { _, _ -> updatePreview() }
        cgStyle.setOnCheckedStateChangeListener { _, _ -> updatePreview() }
        sliderSize.addOnChangeListener { _, _, _ -> updatePreview() }
        switchBg.setOnCheckedChangeListener { _, _ -> updatePreview() }

        updatePreview()

        Utils.trackScreenView(this, "Caption Editor")

        // Apply
        findViewById<View>(R.id.btn_apply_caption).setOnClickListener {
            Utils.captionText = etCaption.text?.toString()?.trim() ?: ""
            Utils.captionColor = selectedColor
            Utils.captionSizeMultiplier = sliderSize.value / 100f
            Utils.captionBackground = switchBg.isChecked
            Utils.captionPosition = when (cgPosition.checkedChipId) {
                R.id.chip_pos_top -> 2
                R.id.chip_pos_center -> 1
                else -> 0
            }
            Utils.captionStyle = when (cgStyle.checkedChipId) {
                R.id.chip_style_bold -> 1
                R.id.chip_style_outline -> 2
                else -> 0
            }

            val bundle = Bundle().apply {
                putBoolean("has_text", Utils.captionText.isNotBlank())
                putString("style", when (Utils.captionStyle) {
                    1 -> "bold"
                    2 -> "outline"
                    else -> "normal"
                })
                putString("position", when (Utils.captionPosition) {
                    2 -> "top"
                    1 -> "center"
                    else -> "bottom"
                })
                putString("color", colorOptions.firstOrNull { it.second == selectedColor }?.first?.lowercase() ?: "custom")
                putBoolean("has_background", Utils.captionBackground)
            }
            Utils.trackEvent(bundle, "caption_applied", this)

            setResult(RESULT_OK)
            finish()
        }
    }

    private fun invertColor(color: Int): Int {
        return Color.rgb(255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color))
    }

    private fun buildColorSwatches(container: LinearLayout) {
        val sizePx = resources.getDimensionPixelSize(R.dimen.color_swatch_size)
        val marginPx = resources.getDimensionPixelSize(R.dimen.color_swatch_margin)

        colorOptions.forEach { (name, color) ->
            val swatch = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).also {
                    it.marginEnd = marginPx
                }
                background = buildSwatchDrawable(color)
                contentDescription = name
                tag = color
                setOnClickListener { selectColor(color, container) }
            }
            container.addView(swatch)
        }

        selectColor(selectedColor, container)
    }

    private fun selectColor(color: Int, container: LinearLayout) {
        selectedColor = color
        for (i in 0 until container.childCount) {
            val swatch = container.getChildAt(i)
            val isSelected = swatch.tag == color
            swatch.scaleX = if (isSelected) 1.3f else 1.0f
            swatch.scaleY = if (isSelected) 1.3f else 1.0f
        }
        tvPreview.setTextColor(color)
    }

    private fun buildSwatchDrawable(color: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(color)
            setStroke(2, Color.argb(120, 0, 0, 0))
        }
    }
}
