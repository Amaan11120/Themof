package com.themof.app

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.*

data class T(val name: String, val c1: Int, val c2: Int, val o: GradientDrawable.Orientation)

class MainActivity : Activity() {
    private val hues = listOf("Red","Orange","Amber","Lime","Green","Mint","Cyan","Sky","Blue","Violet","Pink","Rose")
    private val moods = listOf("Neon","Pastel","Dark","Sunset","Ocean","Candy","Royal","Soft","Bold","Night","Aurora","Lava","Forest","Galaxy","Peach","Steel","Glow","Dusk","Retro","Ice")
    private val sat = floatArrayOf(1f,.35f,.8f,.9f,.7f,.6f,.85f,.4f,1f,.75f,.7f,.95f,.75f,.85f,.4f,.2f,1f,.6f,.8f,.25f)
    private val vl = floatArrayOf(1f,1f,.45f,.95f,.8f,1f,.6f,.9f,.9f,.3f,.9f,.85f,.55f,.5f,1f,.75f,1f,.7f,.85f,1f)

    private val all = ArrayList<T>()
    private val chips = ArrayList<TextView>()
    private lateinit var prefs: SharedPreferences
    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private lateinit var count: TextView
    private var favOnly = false
    private var cat = "All"
    private var cell = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun draw(t: T) = GradientDrawable(t.o, intArrayOf(t.c1, t.c2))
    private fun favs(): MutableSet<String> = HashSet(prefs.getStringSet("favs", emptySet())!!)
    private fun accent() = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(0xFFB026FF.toInt(), 0xFF00B8FF.toInt())).apply { cornerRadius = dp(18).toFloat() }
    private fun glass(r: Int) = GradientDrawable().apply {
        cornerRadius = dp(r).toFloat(); setColor(0x14FFFFFF); setStroke(dp(1), 0x22FFFFFF)
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b); Setup.run(this)
        prefs = getSharedPreferences("themof", MODE_PRIVATE)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = 0xFF030308.toInt()

        for (i in 0 until 240) {
            val h = (i % 12) * 30f
            val m = i / 12
            all.add(T(
                "${moods[m]} ${hues[i % 12]}",
                Color.HSVToColor(floatArrayOf(h, sat[m], vl[m])),
                Color.HSVToColor(floatArrayOf((h + (if (m < 10) 50f else 120f)) % 360f, sat[m], vl[m] * 0.55f)),
                GradientDrawable.Orientation.values()[i % 8]
            ))
        }
        for (s in prefs.getStringSet("custom", emptySet())!!) {
            val p = s.split("|")
            if (p.size == 4) all.add(T(p[0], p[1].toInt(), p[2].toInt(), GradientDrawable.Orientation.values()[p[3].toInt()]))
        }

        cell = resources.displayMetrics.widthPixels / 2 - dp(24)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(36), dp(12), dp(8))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF1A0B3D.toInt(), 0xFF05050D.toInt(), 0xFF030308.toInt()))
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo); scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(Color.BLACK) }
            clipToOutline = true; outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        titles.addView(TextView(this).apply {
            text = "Themof"; textSize = 24f; setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD); letterSpacing = 0.04f
        })
        count = TextView(this).apply { textSize = 12f; setTextColor(0x99FFFFFF.toInt()) }
        titles.addView(count)
        val favBtn = TextView(this).apply {
            text = "♡"; textSize = 20f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = glass(22)
        }
        favBtn.setOnClickListener {
            favOnly = !favOnly
            favBtn.text = if (favOnly) "♥" else "♡"
            favBtn.setTextColor(if (favOnly) 0xFFFF4FD8.toInt() else Color.WHITE)
            refresh()
        }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), 0, dp(4), dp(12)) }
        head.addView(logo, LinearLayout.LayoutParams(dp(46), dp(46)))
        head.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(favBtn, LinearLayout.LayoutParams(dp(44), dp(44)))

        search = EditText(this).apply {
            hint = "Search themes"; setHintTextColor(0x88FFFFFF.toInt()); setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT; setSingleLine(true)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = glass(18)
        }

        val row = LinearLayout(this)
        for (n in listOf("All", "Mine") + moods) {
            val c = TextView(this).apply {
                text = n; textSize = 13f; setTextColor(Color.WHITE)
                setPadding(dp(16), dp(8), dp(16), dp(8))
            }
            c.setOnClickListener { cat = n; styleChips(); refresh() }
            row.addView(c, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
            chips.add(c)
        }
        val chipScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false; addView(row); setPadding(0, dp(10), 0, dp(6))
        }

        grid = GridLayout(this).apply { columnCount = 2 }
        val sv = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER; addView(grid)
        }

        val dock = LinearLayout(this).apply {
            background = glass(26); setPadding(dp(6), dp(6), dp(6), dp(6)); gravity = Gravity.CENTER
        }
        fun item(label: String, click: () -> Unit) {
            val v = TextView(this).apply {
                text = label; textSize = 12f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
            }
            v.setOnClickListener { click() }
            dock.addView(v, LinearLayout.LayoutParams(0, -2, 1f))
        }
        item("✨\nThemes") { sv.smoothScrollTo(0, 0) }
        item("🎨\nStudio") { startActivity(Intent(this, EditorActivity::class.java)) }
        item("⚡\nCharge") { startActivity(Intent(this, ChargingActivity::class.java)) }
        item("🎬\nVideo") { startActivity(Intent(this, VideoEditorActivity::class.java)) }
        item("➕\nCreate") { create() }

        root.addView(head)
        root.addView(search, LinearLayout.LayoutParams(-1, -2))
        root.addView(chipScroll)
        root.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(dock, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(6) })
        setContentView(root)
        styleChips()
        refresh()
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { refresh() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun styleChips() {
        for (c in chips) c.background = if (c.text.toString() == cat) accent() else glass(18)
    }

    private fun refresh() {
        val q = search.text.toString().trim().lowercase()
        val f = favs()
        val list = all.filter { t ->
            t.name.lowercase().contains(q) &&
            (cat == "All" || (cat == "Mine" && t.name.startsWith("My Theme")) || t.name.startsWith(cat)) &&
            (!favOnly || t.name in f)
        }
        count.text = "${list.size} themes"
        show(list)
    }

    private fun show(list: List<T>) {
        grid.removeAllViews()
        val f0 = favs()
        list.forEachIndexed { idx, t ->
            val f = FrameLayout(this)
            f.background = draw(t).apply { cornerRadius = dp(22).toFloat(); setStroke(dp(1), 0x33FFFFFF) }
            f.clipToOutline = true
            f.outlineProvider = ViewOutlineProvider.BACKGROUND
            val scrim = View(this).apply {
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x00000000, 0xAA000000.toInt()))
            }
            f.addView(scrim, FrameLayout.LayoutParams(-1, dp(80), Gravity.BOTTOM))
            val tv = TextView(this).apply {
                text = t.name; setTextColor(Color.WHITE); textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setPadding(dp(14), 0, dp(14), dp(12))
            }
            f.addView(tv, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM))
            val heart = TextView(this).apply {
                text = if (t.name in f0) "♥" else "♡"
                textSize = 24f; setTextColor(Color.WHITE); setPadding(dp(12), dp(8), dp(12), dp(8))
                setOnClickListener {
                    val s = favs()
                    if (!s.remove(t.name)) s.add(t.name)
                    prefs.edit().putStringSet("favs", s).apply()
                    text = if (t.name in s) "♥" else "♡"
                    animate().scaleX(1.4f).scaleY(1.4f).setDuration(120).withEndAction { animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
                    if (favOnly) refresh()
                }
            }
            f.addView(heart, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END))
            f.setOnClickListener { preview(t) }
            f.setOnTouchListener { v, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(.96f).scaleY(.96f).setDuration(100).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
                false
            }
            f.alpha = 0f; f.translationY = dp(24).toFloat()
            f.animate().alpha(1f).translationY(0f).setStartDelay(minOf(idx, 10) * 35L).setDuration(320).start()
            val lp = GridLayout.LayoutParams().apply { width = cell; height = (cell * 1.6).toInt(); setMargins(dp(6), dp(6), dp(6), dp(6)) }
            grid.addView(f, lp)
        }
    }

    private fun create() {
        val h1 = SeekBar(this).apply { max = 359; progress = 280 }
        val b1 = SeekBar(this).apply { max = 100; progress = 100 }
        val h2 = SeekBar(this).apply { max = 359; progress = 190 }
        val b2 = SeekBar(this).apply { max = 100; progress = 80 }
        val dir = SeekBar(this).apply { max = 7; progress = 0 }
        val pv = View(this)
        fun col(h: SeekBar, b: SeekBar) =
            Color.HSVToColor(floatArrayOf(h.progress.toFloat(), 0.8f, maxOf(b.progress, 10) / 100f))
        fun cur() = T("", col(h1, b1), col(h2, b2), GradientDrawable.Orientation.values()[dir.progress])
        fun upd() { pv.background = draw(cur()).apply { cornerRadius = dp(20).toFloat() } }
        val l = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { upd() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(10), dp(20), 0) }
        box.addView(pv, LinearLayout.LayoutParams(-1, dp(220)))
        val items = listOf("Color 1" to h1, "Color 1 brightness" to b1, "Color 2" to h2, "Color 2 brightness" to b2, "Direction" to dir)
        for ((name, sb) in items) {
            box.addView(TextView(this).apply { text = name; setTextColor(0xCCFFFFFF.toInt()); setPadding(0, dp(10), 0, 0) })
            box.addView(sb)
            sb.setOnSeekBarChangeListener(l)
        }
        upd()
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Create theme")
            .setView(ScrollView(this).apply { addView(box) })
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
        val box = FrameLayout(this).apply { setPadding(dp(24), dp(12), dp(24), 0) }
        box.addView(View(this).apply { background = draw(t).apply { cornerRadius = dp(24).toFloat() } },
            FrameLayout.LayoutParams(-1, dp(420)))
        box.addView(TextView(this).apply {
            text = "9:41"; textSize = 56f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        }, FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply { topMargin = dp(60) })
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle(t.name).setView(box)
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
