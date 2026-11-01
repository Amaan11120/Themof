package com.themof.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import java.text.SimpleDateFormat
import java.util.*

class ChargingView(c: Context) : View(c) {
    var target = 0f
    private var shown = 0f
    private var t = 0f
    private val rnd = Random(7)
    private val px = FloatArray(28) { rnd.nextFloat() }
    private val po = FloatArray(28) { rnd.nextFloat() }
    private val ps = FloatArray(28) { 0.4f + rnd.nextFloat() * 0.8f }
    private val pr = FloatArray(28) { 2f + rnd.nextFloat() * 5f }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
    private var bg: Shader? = null
    private var sweep: SweepGradient? = null
    private val m = Matrix()
    private val oval = RectF()
    private val bolt = Path()
    private val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val df = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    private val bp = floatArrayOf(0.3f, -1f, -0.5f, 0.1f, -0.05f, 0.1f, -0.3f, 1f, 0.5f, -0.15f, 0.05f, -0.15f)
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 5000; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        bg = RadialGradient(w / 2f, h * 0.42f, h * 0.75f,
            intArrayOf(0xFF1B1140.toInt(), 0xFF0A0A18.toInt(), 0xFF000000.toInt()),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        sweep = SweepGradient(w / 2f, h * 0.42f,
            intArrayOf(0xFF00E5FF.toInt(), 0xFF4D5BFF.toInt(), 0xFFB026FF.toInt(), 0xFF00E5FF.toInt()), null)
    }

    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2; val cy = h * 0.42f; val r = minOf(w, h) * 0.27f
        shown += (target - shown) * 0.06f

        p.shader = bg; p.style = Paint.Style.FILL; p.alpha = 255
        cv.drawRect(0f, 0f, w, h, p); p.shader = null

        for (i in px.indices) {
            val f = (t * ps[i] + po[i]) % 1f
            p.color = Color.WHITE
            p.alpha = ((1f - f) * f * 4f * 110).toInt().coerceIn(0, 255)
            cv.drawCircle(px[i] * w, h - f * h, pr[i], p)
        }

        ring.shader = null
        for (i in 0..2) {
            val f = (t * 2f + i / 3f) % 1f
            ring.color = 0xFF4D5BFF.toInt(); ring.alpha = ((1f - f) * 90).toInt(); ring.strokeWidth = 3f
            cv.drawCircle(cx, cy, r * (1.05f + f * 0.9f), ring)
        }

        oval.set(cx - r, cy - r, cx + r, cy + r)
        ring.color = Color.WHITE; ring.alpha = 28; ring.strokeWidth = r * 0.1f
        cv.drawCircle(cx, cy, r, ring)

        m.setRotate(t * 360f, cx, cy); sweep?.setLocalMatrix(m); ring.shader = sweep
        val sw = shown * 3.6f
        cv.save(); cv.rotate(-90f, cx, cy)
        for (k in 3 downTo 1) {
            ring.alpha = 18 * (4 - k); ring.strokeWidth = r * 0.1f + k * r * 0.06f
            cv.drawArc(oval, 0f, sw, false, ring)
        }
        ring.alpha = 255; ring.strokeWidth = r * 0.1f
        cv.drawArc(oval, 0f, sw, false, ring)
        cv.restore(); ring.shader = null

        val ang = Math.toRadians((sw - 90).toDouble())
        val dx = cx + r * Math.cos(ang).toFloat(); val dy = cy + r * Math.sin(ang).toFloat()
        p.color = Color.WHITE; p.alpha = 60
        cv.drawCircle(dx, dy, r * 0.09f * (1f + 0.3f * Math.sin(t * Math.PI * 8).toFloat()), p)
        p.alpha = 255; cv.drawCircle(dx, dy, r * 0.045f, p)

        bolt.reset()
        val s = r * 0.17f; val by = cy - r * 0.52f
        for (k in 0 until 6) {
            val x = cx + bp[k * 2] * s; val y = by + bp[k * 2 + 1] * s
            if (k == 0) bolt.moveTo(x, y) else bolt.lineTo(x, y)
        }
        bolt.close()
        p.color = 0xFFFFE66D.toInt()
        p.alpha = (170 + 85 * Math.sin(t * Math.PI * 4)).toInt().coerceIn(0, 255)
        cv.drawPath(bolt, p)

        txt.color = Color.WHITE; txt.alpha = 255
        txt.typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        txt.letterSpacing = 0f; txt.textSize = r * 0.62f
        cv.drawText("${shown.toInt()}%", cx, cy + r * 0.3f, txt)

        txt.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        txt.textSize = r * 0.15f; txt.letterSpacing = 0.35f; txt.alpha = 200
        cv.drawText(if (target >= 100f) "FULLY CHARGED" else "CHARGING", cx, cy + r * 1.6f, txt)

        val now = Date()
        txt.typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        txt.textSize = r * 0.55f; txt.letterSpacing = 0.05f; txt.alpha = 255
        cv.drawText(tf.format(now), cx, h * 0.82f, txt)
        txt.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        txt.textSize = r * 0.14f; txt.letterSpacing = 0.1f; txt.alpha = 160
        cv.drawText(df.format(now), cx, h * 0.82f + r * 0.3f, txt)
        txt.textSize = r * 0.11f; txt.alpha = 70
        cv.drawText("Tap to close", cx, h * 0.95f, txt)
    }
}
