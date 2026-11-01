package com.themof.app

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

data class T(val name: String, val c1: Int, val c2: Int, val o: GradientDrawable.Orientation)

class MainActivity : Activity() {
    private val hues = listOf("Red","Orange","Amber","Lime","Green","Mint","Cyan","Sky","Blue","Violet","Pink","Rose")
    private val moods = listOf("Neon","Pastel","Dark","Sunset","Ocean","Candy","Royal","Soft","Bold","Night")
    private val sat = floatArrayOf(1f,.35f,.8f,.9f,.7f,.6f,.85f,.4f,1f,.75f)
    private val val_ = floatArrayOf(1f,1f,.45f,.95f,.8f,1f,.6f,.9f,.9f,.3f)

    private val themes = List(120) { i ->
        val h = (i % 12) * 30f
        val m = i / 12
        T(
            "${moods[m]} ${hues[i % 12]}",
            Color.HSVToColor(floatArrayOf(h, sat[m], val_[m])),
            Color.HSVToColor(floatArrayOf((h + 50f) % 360f, sat[m], val_[m] * 0.55f)),
            GradientDrawable.Orientation.values()[i % 8]
        )
    }

    private fun draw(t: T) = GradientDrawable(t.o, intArrayOf(t.c1, t.c2))

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val w = resources.displayMetrics.widthPixels
        val cell = w / 2 - 24
        val grid = GridLayout(this).apply { columnCount = 2; setPadding(12, 40, 12, 12) }
        for (t in themes) {
            val f = FrameLayout(this)
            f.background = draw(t).apply { cornerRadius = 28f }
            val tv = TextView(this).apply {
                text = t.name; setTextColor(Color.WHITE); textSize = 14f
                setPadding(16, 0, 16, 16)
            }
            f.addView(tv, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM))
            f.setOnClickListener { preview(t) }
            val lp = GridLayout.LayoutParams().apply { width = cell; height = (cell * 1.6).toInt(); setMargins(12, 12, 12, 12) }
            grid.addView(f, lp)
        }
        setContentView(ScrollView(this).apply { addView(grid) })
    }

    private fun preview(t: T) {
        val v = View(this).apply { background = draw(t) }
        val box = FrameLayout(this).apply {
            setPadding(40, 20, 40, 0)
            addView(v, FrameLayout.LayoutParams(-1, 800))
        }
        AlertDialog.Builder(this).setTitle(t.name).setView(box)
            .setPositiveButton("Apply Wallpaper") { _, _ -> apply(t) }
            .setNegativeButton("Cancel", null).show()
    }

    private fun apply(t: T) {
        val dm = resources.displayMetrics
        val bmp = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, Bitmap.Config.ARGB_8888)
        val d = draw(t)
        d.setBounds(0, 0, bmp.width, bmp.height)
        d.draw(Canvas(bmp))
        try {
            WallpaperManager.getInstance(this).setBitmap(bmp)
            Toast.makeText(this, "Wallpaper set!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
