package com.nathanhaze.gifcreator.filter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

interface BitmapFilter {
    val name: String
    fun processFilter(src: Bitmap?): Bitmap?
}

private fun applyMatrix(src: Bitmap, matrix: ColorMatrix): Bitmap {
    val result = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
    Canvas(result).drawBitmap(src, 0f, 0f, paint)
    return result
}

object Grayscale : BitmapFilter {
    override val name = "Grayscale"
    override fun processFilter(src: Bitmap?): Bitmap? {
        src ?: return null
        return applyMatrix(src, ColorMatrix().apply { setSaturation(0f) })
    }
}

object Sepia : BitmapFilter {
    override val name = "Sepia"
    override fun processFilter(src: Bitmap?): Bitmap? {
        src ?: return null
        val m = ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f,     0f,     0f,     1f, 0f
        ))
        return applyMatrix(src, m)
    }
}

object Warm : BitmapFilter {
    override val name = "Warm"
    override fun processFilter(src: Bitmap?): Bitmap? {
        src ?: return null
        val m = ColorMatrix(floatArrayOf(
            1.2f,  0f,    0f,    0f, 10f,
            0f,    1.05f, 0f,    0f,  5f,
            0f,    0f,    0.85f, 0f, -10f,
            0f,    0f,    0f,    1f,  0f
        ))
        return applyMatrix(src, m)
    }
}

object Cool : BitmapFilter {
    override val name = "Cool"
    override fun processFilter(src: Bitmap?): Bitmap? {
        src ?: return null
        val m = ColorMatrix(floatArrayOf(
            0.85f, 0f,    0f,    0f, -10f,
            0f,    0.95f, 0f,    0f,   0f,
            0f,    0f,    1.2f,  0f,  15f,
            0f,    0f,    0f,    1f,   0f
        ))
        return applyMatrix(src, m)
    }
}

object Fade : BitmapFilter {
    override val name = "Fade"
    override fun processFilter(src: Bitmap?): Bitmap? {
        src ?: return null
        // Reduce contrast: compress range toward mid-gray
        val m = ColorMatrix(floatArrayOf(
            0.6f, 0f,   0f,   0f, 50f,
            0f,   0.6f, 0f,   0f, 50f,
            0f,   0f,   0.6f, 0f, 50f,
            0f,   0f,   0f,   1f,  0f
        ))
        return applyMatrix(src, m)
    }
}

object Vivid : BitmapFilter {
    override val name = "Vivid"
    override fun processFilter(src: Bitmap?): Bitmap? {
        src ?: return null
        return applyMatrix(src, ColorMatrix().apply { setSaturation(2.2f) })
    }
}

object Invert : BitmapFilter {
    override val name = "Invert"
    override fun processFilter(src: Bitmap?): Bitmap? {
        src ?: return null
        val m = ColorMatrix(floatArrayOf(
            -1f,  0f,  0f, 0f, 255f,
             0f, -1f,  0f, 0f, 255f,
             0f,  0f, -1f, 0f, 255f,
             0f,  0f,  0f, 1f,   0f
        ))
        return applyMatrix(src, m)
    }
}

object Noir : BitmapFilter {
    override val name = "Noir"
    // High-contrast black & white
    override fun processFilter(src: Bitmap?): Bitmap? {
        src ?: return null
        val desat = ColorMatrix().apply { setSaturation(0f) }
        // Boost contrast after desaturation
        val contrast = ColorMatrix(floatArrayOf(
            1.8f, 0f,   0f,   0f, -80f,
            0f,   1.8f, 0f,   0f, -80f,
            0f,   0f,   1.8f, 0f, -80f,
            0f,   0f,   0f,   1f,   0f
        ))
        contrast.preConcat(desat)
        return applyMatrix(src, contrast)
    }
}

object Vintage : BitmapFilter {
    override val name = "Vintage"
    override fun processFilter(src: Bitmap?): Bitmap? {
        src ?: return null
        // Desaturate slightly, add warm yellow-brown tint, darken edges
        val m = ColorMatrix(floatArrayOf(
            0.7f,  0.2f,  0.1f,  0f, 10f,
            0.15f, 0.65f, 0.1f,  0f,  5f,
            0.1f,  0.1f,  0.5f,  0f, -5f,
            0f,    0f,    0f,    1f,  0f
        ))
        return applyMatrix(src, m)
    }
}

object Neon : BitmapFilter {
    override val name = "Neon"
    // Boosted saturation + slight contrast push
    override fun processFilter(src: Bitmap?): Bitmap? {
        src ?: return null
        val sat = ColorMatrix().apply { setSaturation(3f) }
        val contrast = ColorMatrix(floatArrayOf(
            1.3f, 0f,   0f,   0f, -30f,
            0f,   1.3f, 0f,   0f, -30f,
            0f,   0f,   1.3f, 0f, -30f,
            0f,   0f,   0f,   1f,   0f
        ))
        contrast.preConcat(sat)
        return applyMatrix(src, contrast)
    }
}

object BitmapFilters {
    fun getFilterPack(): List<BitmapFilter> = listOf(
        Grayscale, Sepia, Warm, Cool, Fade, Vivid, Invert, Noir, Vintage, Neon
    )
}
