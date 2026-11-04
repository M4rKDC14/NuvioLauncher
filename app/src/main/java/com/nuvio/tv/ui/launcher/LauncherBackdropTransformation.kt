package com.nuvio.tv.ui.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import coil3.size.Size
import coil3.transform.Transformation
import com.nuvio.tv.ui.util.BlurTransformation

/**
 * The whole launcher background in one small opaque picture (160x90, stretched to the screen):
 * the grey base, the blurred title picture at [alpha] over it, then the dark wash on the left and the
 * one at the bottom. Before, those were four full-screen layers painted on every frame (plus a grey
 * gradient behind them), and the TV's GPU overdraw view showed the whole screen painted 4+ times.
 * Now the screen gets one image draw. The washes are smooth gradients, so painting them at 160x90 and
 * stretching looks the same.
 */
class LauncherBackdropTransformation(
    private val alpha: Float = 0.6f,
    private val blurRadius: Int = 6
) : Transformation() {

    override val cacheKey: String = "launcher_backdrop_v1_${alpha}_$blurRadius"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val blurred = BlurTransformation(radius = blurRadius).transform(input, size)
        val w = W
        val h = H
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // grey base (LauncherStyle.GreyTop -> GreyBottom)
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF2A2B30.toInt(), 0xFF1E1F23.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        // the picture, centre-cropped to 16:9 (what ContentScale.Crop did on screen), at [alpha]
        val bw = blurred.width
        val bh = blurred.height
        val src = if (bw * h > bh * w) {
            val cw = bh * w / h
            Rect((bw - cw) / 2, 0, (bw - cw) / 2 + cw, bh)
        } else {
            val ch = bw * h / w
            Rect(0, (bh - ch) / 2, bw, (bh - ch) / 2 + ch)
        }
        p.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        c.drawBitmap(blurred, src, Rect(0, 0, w, h), p)
        p.alpha = 255

        // dark wash on the left: .55 at the edge to .15 at 60% of the width, then .15
        p.shader = LinearGradient(0f, 0f, w * 0.6f, 0f, 0x8C141518.toInt(), 0x26141518, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        // dark wash at the bottom: .10 at the top to .45 at the bottom
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0x1A141518, 0x73141518, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        return out
    }

    private companion object {
        const val W = 160
        const val H = 90
    }
}
