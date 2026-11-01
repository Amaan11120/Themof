package com.themof.app

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*

data class T(val name: String, val c1: Int, val c2: Int, val o: GradientDrawable.Orientation)

class MainActivity : Activity() {
    private val hues = listOf("Red","Orange","Amber","Lime","Green","Mint","Cyan","Sky","Blue","Violet","Pink","Rose")
    private val moods = listOf("Neon","Pastel","Dark","Sunset","Ocean","Candy","Royal","Soft","Bold","Night")
    private val sat = floatArrayOf(1f,.35f,.8f,.9f,.7f,.6f,.85f,.4f,1f,.75f)
    private val vl = floatArrayOf(1f,1f,.45f,.95f,.8f,1f,.6f,.9f,.9f,.3f)

    private val all = ArrayList<T>()
    private lateinit var prefs: SharedPreferences
    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private var favOnly = false
    private var cell = 0

    private fun draw(t: T) = GradientDrawable(t.o, intArrayOf(t.c1, t.c2))
    private fun favs(): MutableSet<String> = HashSet(prefs.getStringSet("favs", emptySet())!!)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = getSharedPreferences("themof", MODE_PRIVATE)
        for (i in 0 until 120) {
            val h = (i % 12) * 30f
            val m = i / 12
            all.add(T(
                "${moods[m]} ${hues[i % 12]}",
                Color.HSVToColor(floatArrayOf(h, sat[m], vl[m])),
                Color.HSVToColor(floatArrayOf((h + 50f) % 360f, sat[m], vl[m] * 0.55f)),
                GradientDrawable.Orientation.values()[i % 8]
            ))
        }
        for (s in prefs.getStringSet("custom", emptySet())!!) {
            val p = s.split("|")
            if (p.size == 4) all.add(T(p[0], p[1].toInt(), p[2].toInt(), GradientDrawable.Orientation.values()[p[3].toInt()]))
        }

        cell = resources.displayMetrics.widthPixels / 2 - 24
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 40, 12, 0) }
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        search = EditText(this).apply { hint = "Search themes" }
        val favBtn = Button(this).apply {
            text = "♡"
            setOnClickListener { favOnly = !favOnly; text = if (favOnly) "♥" else "♡"; refresh() }
        }
        val addBtn = Button(this).apply { text = "+"; setOnClickListener { create() } }
        bar.addView(search, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(favBtn)
        bar.addView(addBtn)
        grid = GridLayout(this).apply { columnCount = 2 }
        root.addView(bar)
        root.addView(ScrollView(this).apply { addView(grid) })
        setContentView(root)
        refresh()
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { refresh() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun refresh() {
        val q = search.text.toString().trim().lowercase()
        val f = favs()
        show(all.filter { it.name.lowercase().contains(q) && (!favOnly || it.name in f) })
    }

    private fun show(list: List<T>) {
        grid.removeAllViews()
        val f0 = favs()
        for (t in list) {
            val f = FrameLayout(this)
            f.background = draw(t).apply { cornerRadius = 28f }
            val tv = TextView(this).apply {
                text = t.name; setTextColor(Color.WHITE); textSize = 14f
                setPadding(16, 0, 16, 16)
            }
            f.addView(tv, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM))
            val heart = TextView(this).apply {
                text = if (t.name in f0) "♥" else "♡"
                textSize = 24f; setTextColor(Color.WHITE); setPadding(16, 8, 16, 8)
                setOnClickListener {
                    val s = favs()
                    if (!s.remove(t.name)) s.add(t.name)
                    prefs.edit().putStringSet("favs", s).apply()
                    text = if (t.name in s) "♥" else "♡"
                    if (favOnly) refresh()
                }
            }
            f.addView(heart, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END))
            f.setOnClickListener { preview(t) }
            val lp = GridLayout.LayoutParams().apply { width = cell; height = (cell * 1.6).toInt(); setMargins(12, 12, 12, 12) }
            grid.addView(f, lp)
        }
    }

    private fun create() {
        val h1 = SeekBar(this).apply { max = 359; progress = 200 }
        val b1 = SeekBar(this).apply { max = 100; progress = 100 }
        val h2 = SeekBar(this).apply { max = 359; progress = 280 }
        val b2 = SeekBar(this).apply { max = 100; progress = 60 }
        val dir = SeekBar(this).apply { max = 7; progress = 0 }
        val pv = View(this)
        fun col(h: SeekBar, b: SeekBar) =
            Color.HSVToColor(floatArrayOf(h.progress.toFloat(), 0.8f, maxOf(b.progress, 10) / 100f))
        fun cur() = T("", col(h1, b1), col(h2, b2), GradientDrawable.Orientation.values()[dir.progress])
        fun upd() { pv.background = draw(cur()) }
        val l = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { upd() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 0) }
        box.addView(pv, LinearLayout.LayoutParams(-1, 500))
        val items = listOf("Color 1" to h1, "Color 1 brightness" to b1, "Color 2" to h2, "Color 2 brightness" to b2, "Direction" to dir)
        for ((name, sb) in items) {
            box.addView(TextView(this).apply { text = name })
            box.addView(sb)
            sb.setOnSeekBarChangeListener(l)
        }
        upd()
        AlertDialog.Builder(this).setTitle("Create theme").setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Save") { _, _ ->
                val c = cur()
                val n = "My Theme ${all.count { it.name.startsWith("My Theme") } + 1}"
                val t = T(n, c.c1, c.c2, c.o)
                all.add(t)
                val s = HashSet(prefs.getStringSet("custom", emptySet())!!)
                s.add("$n|${t.c1}|${t.c2}|${t.o.ordinal}")
                prefs.edit().putStringSet("custom", s).apply()
                refresh()
                Toast.makeText(this, "$n saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun preview(t: T) {
        val v = View(this).apply { background = draw(t) }
        val box = FrameLayout(this).apply {
            setPadding(40, 20, 40, 0)
            addView(v, FrameLayout.LayoutParams(-1, 800))
        }
        AlertDialog.Builder(this).setTitle(t.name).setView(box)
            .setPositiveButton("Home") { _, _ -> apply(t, WallpaperManager.FLAG_SYSTEM) }
            .setNeutralButton("Lock") { _, _ -> apply(t, WallpaperManager.FLAG_LOCK) }
            .setNegativeButton("Both") { _, _ -> apply(t, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) }
            .show()
    }

    private fun apply(t: T, flag: Int) {
        val dm = resources.displayMetrics
        val bmp = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, Bitmap.Config.ARGB_8888)
        val d = draw(t)
        d.setBounds(0, 0, bmp.width, bmp.height)
        d.draw(Canvas(bmp))
        try {
            WallpaperManager.getInstance(this).setBitmap(bmp, null, true, flag)
            Toast.makeText(this, "Wallpaper set!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
