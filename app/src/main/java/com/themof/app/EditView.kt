package com.themof.app

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class EditView(c: Context) : View(c) {
    var bmp: Bitmap? = null
        set(v) { field = v; invalidate() }
    var cf: ColorFilter? = null
        set(v) { field = v; invalidate() }
    var mode = 0
    var color = Color.WHITE
    var brush = 8f
    private class Stroke(val path: Path, val color: Int, val w: Float)
    private class Tx(val s: String, var x: Float, var y: Float, val size: Float, val color: Int)
    private val strokes = ArrayList<Stroke>()
    private val texts = ArrayList<Tx>()
    private val order = ArrayList<Any>()
    private var cur: Path? = null
    private var lx = 0f
    private var ly = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 0f, 0x99000000.toInt())
    }

    private fun unit() = bmp?.let { maxOf(it.width, it.height) / 1000f } ?: 1f
    private fun sc() = bmp?.let { minOf(width / it.width.toFloat(), height / it.height.toFloat()) } ?: 1f
    private fun ox() = bmp?.let { (width - it.width * sc()) / 2 } ?: 0f
    private fun oy() = bmp?.let { (height - it.height * sc()) / 2 } ?: 0f

    private fun overlays(cv: Canvas) {
        for (s in strokes) { sp.color = s.color; sp.strokeWidth = s.w; cv.drawPath(s.path, sp) }
        for (t in texts) { tp.color = t.color; tp.textSize = t.size; cv.drawText(t.s, t.x, t.y, tp) }
    }

    override fun onDraw(cv: Canvas) {
        val b = bmp ?: return
        cv.save()
        cv.translate(ox(), oy()); cv.scale(sc(), sc())
        p.colorFilter = cf; cv.drawBitmap(b, 0f, 0f, p); p.colorFilter = null
        overlays(cv)
        cv.restore()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (bmp == null) return false
        val x = (e.x - ox()) / sc(); val y = (e.y - oy()) / sc()
        if (mode == 1) {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    val path = Path().apply { moveTo(x, y); lineTo(x + 0.01f, y) }
                    val s = Stroke(path, color, brush * unit())
                    cur = path; strokes.add(s); order.add(s)
                }
                MotionEvent.ACTION_MOVE -> cur?.lineTo(x, y)
                else -> cur = null
            }
            invalidate(); return true
        }
        if (mode == 2) {
            val t = texts.lastOrNull() ?: return false
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { lx = x; ly = y }
                MotionEvent.ACTION_MOVE -> { t.x += x - lx; t.y += y - ly; lx = x; ly = y }
            }
            invalidate(); return true
        }
        return false
    }

    fun addText(s: String) {
        val b = bmp ?: return
        val t = Tx(s, b.width / 2f, b.height / 2f, maxOf(b.width, b.height) / 12f, color)
        texts.add(t); order.add(t); mode = 2; invalidate()
    }

    fun undo() {
        val o = order.removeLastOrNull() ?: return
        strokes.remove(o); texts.remove(o); invalidate()
    }

    private fun bake() {
        val b = bmp ?: return
        val out = b.copy(Bitmap.Config.ARGB_8888, true)
        overlays(Canvas(out))
        strokes.clear(); texts.clear(); order.clear()
        bmp = out
    }

    fun crop(ratio: Float) {
        bake()
        val b = bmp ?: return
        var w = b.width.toFloat(); var h = w / ratio
        if (h > b.height) { h = b.height.toFloat(); w = h * ratio }
        val x = ((b.width - w) / 2).toInt(); val y = ((b.height - h) / 2).toInt()
        bmp = Bitmap.createBitmap(b, x, y, w.toInt(), h.toInt())
    }

    fun rotate() {
        bake()
        val b = bmp ?: return
        bmp = Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(90f) }, true)
    }

    fun render(): Bitmap? {
        val b = bmp ?: return null
        val out = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        p.colorFilter = cf; c.drawBitmap(b, 0f, 0f, p); p.colorFilter = null
        overlays(c)
        return out
    }
}
