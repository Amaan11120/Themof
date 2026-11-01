package com.themof.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.*

class EditorActivity : Activity() {
    private lateinit var ev: EditView
    private lateinit var bri: SeekBar
    private lateinit var con: SeekBar
    private lateinit var sat: SeekBar
    private var filter: FloatArray? = null
    private val palette = intArrayOf(Color.WHITE, Color.BLACK, 0xFFFF3B30.toInt(), 0xFFFFD60A.toInt(), 0xFF00E5FF.toInt(), 0xFFFF4FD8.toInt(), 0xFF30D158.toInt())
    private var ci = 0
    private val sizes = floatArrayOf(4f, 8f, 16f, 28f)
    private var si = 1

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private val filters: List<Pair<String, FloatArray?>> = listOf(
        "Original" to null,
        "B&W" to floatArrayOf(.299f,.587f,.114f,0f,0f, .299f,.587f,.114f,0f,0f, .299f,.587f,.114f,0f,0f, 0f,0f,0f,1f,0f),
        "Sepia" to floatArrayOf(.393f,.769f,.189f,0f,0f, .349f,.686f,.168f,0f,0f, .272f,.534f,.131f,0f,0f, 0f,0f,0f,1f,0f),
        "Vintage" to floatArrayOf(.9f,.5f,.1f,0f,0f, .3f,.8f,.1f,0f,0f, .2f,.3f,.5f,0f,0f, 0f,0f,0f,1f,0f),
        "Cool" to floatArrayOf(.9f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1.15f,0f,10f, 0f,0f,0f,1f,0f),
        "Warm" to floatArrayOf(1.15f,0f,0f,0f,10f, 0f,1f,0f,0f,0f, 0f,0f,.85f,0f,0f, 0f,0f,0f,1f,0f),
        "Vivid" to ColorMatrix().apply { setSaturation(1.7f) }.array,
        "Fade" to floatArrayOf(.8f,0f,0f,0f,40f, 0f,.8f,0f,0f,40f, 0f,0f,.8f,0f,40f, 0f,0f,0f,1f,0f),
        "Night" to floatArrayOf(.6f,0f,0f,0f,0f, 0f,.7f,0f,0f,0f, 0f,0f,1.1f,0f,20f, 0f,0f,0f,1f,0f),
        "Invert" to floatArrayOf(-1f,0f,0f,0f,255f, 0f,-1f,0f,0f,255f, 0f,0f,-1f,0f,255f, 0f,0f,0f,1f,0f)
    )

    private fun matrix(): ColorMatrix {
        val m = ColorMatrix()
        val s = ColorMatrix(); s.setSaturation(sat.progress / 100f)
        m.postConcat(s)
        val c = con.progress / 100f
        val t = (1 - c) * 128f + (bri.progress - 100) * 1.2f
        m.postConcat(ColorMatrix(floatArrayOf(c,0f,0f,0f,t, 0f,c,0f,0f,t, 0f,0f,c,0f,t, 0f,0f,0f,1f,0f)))
        filter?.let { m.postConcat(ColorMatrix(it)) }
        return m
    }

    private fun upd() { ev.cf = ColorMatrixColorFilter(matrix()) }

    private fun btn(label: String, click: (TextView) -> Unit) = TextView(this).apply {
        text = label; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER
        setPadding(dp(16), dp(10), dp(16), dp(10))
        background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(0x26FFFFFF) }
        setOnClickListener { click(this) }
        layoutParams = LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) }
    }

    private fun scroller(row: LinearLayout) = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false; addView(row)
        setPadding(0, dp(4), 0, dp(4))
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.TRANSPARENT
        ev = EditView(this)
        bri = SeekBar(this).apply { max = 200; progress = 100 }
        con = SeekBar(this).apply { max = 200; progress = 100 }
        sat = SeekBar(this).apply { max = 200; progress = 100 }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(40), dp(12), dp(8))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFF1A1236.toInt(), 0xFF07070F.toInt()))
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply { text = "Studio"; textSize = 26f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(btn("Pick") { pick() })
        top.addView(btn("Save") { save() })
        root.addView(top)

        root.addView(ev, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8); bottomMargin = dp(4) })

        val tools = LinearLayout(this)
        tools.addView(btn("Draw") { v ->
            ev.mode = if (ev.mode == 1) 0 else 1
            v.text = if (ev.mode == 1) "Draw ✓" else "Draw"
        })
        val colorBtn = btn("●") { v -> ci = (ci + 1) % palette.size; ev.color = palette[ci]; v.setTextColor(palette[ci]) }
        colorBtn.setTextColor(palette[0]); colorBtn.textSize = 20f
        tools.addView(colorBtn)
        tools.addView(btn("Brush 8") { v -> si = (si + 1) % sizes.size; ev.brush = sizes[si]; v.text = "Brush ${sizes[si].toInt()}" })
        tools.addView(btn("Text") { addText() })
        tools.addView(btn("Undo") { ev.undo() })
        tools.addView(btn("Crop") { crop() })
        tools.addView(btn("Rotate") { ev.rotate() })
        root.addView(scroller(tools))

        val row = LinearLayout(this)
        for ((n, f) in filters) row.addView(btn(n) { filter = f; upd() })
        root.addView(scroller(row))

        val l = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { upd() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        for ((n, sb) in listOf("Brightness" to bri, "Contrast" to con, "Saturation" to sat)) {
            root.addView(TextView(this).apply { text = n; setTextColor(0xCCFFFFFF.toInt()); textSize = 12f })
            sb.setOnSeekBarChangeListener(l)
            root.addView(sb)
        }
        setContentView(root)
        pick()
    }

    private fun addText() {
        if (ev.bmp == null) return
        val input = EditText(this).apply { hint = "Text likho"; setTextColor(Color.WHITE); setHintTextColor(0x88FFFFFF.toInt()) }
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Add text").setView(input)
            .setPositiveButton("Add") { _, _ ->
                val s = input.text.toString()
                if (s.isNotBlank()) {
                    ev.addText(s)
                    Toast.makeText(this, "Photo pe drag karke text hilao", Toast.LENGTH_SHORT).show()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun crop() {
        val names = arrayOf("1:1", "4:5", "3:4", "16:9", "9:16")
        val r = floatArrayOf(1f, 4f / 5f, 3f / 4f, 16f / 9f, 9f / 16f)
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Crop ratio")
            .setItems(names) { _, i -> ev.crop(r[i]) }.show()
    }

    private fun pick() {
        startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), 1)
    }

    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        super.onActivityResult(rc, res, d)
        if (res != RESULT_OK) return
        val u = d?.data ?: return
        try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
            var ss = 1
            while (o.outWidth / ss > 2048 || o.outHeight / ss > 2048) ss *= 2
            val o2 = BitmapFactory.Options().apply { inSampleSize = ss }
            ev.bmp = contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o2) }
            upd()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun save() {
        val out = ev.render() ?: run { Toast.makeText(this, "Pehle photo pick karo", Toast.LENGTH_SHORT).show(); return }
        try {
            val name = "Themof_${System.currentTimeMillis()}"
            if (Build.VERSION.SDK_INT >= 29) {
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Themof")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)!!
                contentResolver.openOutputStream(uri)!!.use { out.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.insertImage(contentResolver, out, name, null)
            }
            Toast.makeText(this, "Saved in Pictures/Themof", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
