package com.themof.app

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.*

class EditorActivity : Activity() {
    private var src: Bitmap? = null
    private var filter: FloatArray? = null
    private lateinit var iv: ImageView
    private val bri = SeekBar(this)
    private val con = SeekBar(this)
    private val sat = SeekBar(this)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun sepia() = floatArrayOf(.393f,.769f,.189f,0f,0f, .349f,.686f,.168f,0f,0f, .272f,.534f,.131f,0f,0f, 0f,0f,0f,1f,0f)
    private val filters: List<Pair<String, FloatArray?>> = listOf(
        "Original" to null,
        "B&W" to floatArrayOf(.299f,.587f,.114f,0f,0f, .299f,.587f,.114f,0f,0f, .299f,.587f,.114f,0f,0f, 0f,0f,0f,1f,0f),
        "Sepia" to sepia(),
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

    private fun upd() { iv.colorFilter = ColorMatrixColorFilter(matrix()) }

    private fun btn(label: String, click: () -> Unit) = TextView(this).apply {
        text = label; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER
        setPadding(dp(16), dp(10), dp(16), dp(10))
        background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(0x26FFFFFF) }
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.TRANSPARENT
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(40), dp(12), dp(8))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFF1A1236.toInt(), 0xFF07070F.toInt()))
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply { text = "Studio"; textSize = 26f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(btn("Pick") { pick() })
        top.addView(btn("Rotate") { rotate() })
        top.addView(btn("Save") { save() })
        root.addView(top)

        iv = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        root.addView(iv, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8); bottomMargin = dp(8) })

        val row = LinearLayout(this)
        for ((n, f) in filters) row.addView(btn(n) { filter = f; upd() })
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(row) })

        val l = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { upd() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        bri.max = 200; bri.progress = 100
        con.max = 200; con.progress = 100
        sat.max = 200; sat.progress = 100
        for ((n, sb) in listOf("Brightness" to bri, "Contrast" to con, "Saturation" to sat)) {
            root.addView(TextView(this).apply { text = n; setTextColor(0xCCFFFFFF.toInt()); setPadding(0, dp(6), 0, 0) })
            sb.setOnSeekBarChangeListener(l)
            root.addView(sb)
        }
        setContentView(root)
        pick()
    }

    private fun pick() {
        startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), 1)
    }

    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        super.onActivityResult(rc, res, d)
        val u: Uri = d?.data ?: return
        try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }
            var ss = 1
            while (o.outWidth / ss > 2048 || o.outHeight / ss > 2048) ss *= 2
            val o2 = BitmapFactory.Options().apply { inSampleSize = ss }
            src = contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o2) }
            iv.setImageBitmap(src)
            upd()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun rotate() {
        val s = src ?: return
        val m = Matrix().apply { postRotate(90f) }
        src = Bitmap.createBitmap(s, 0, 0, s.width, s.height, m, true)
        iv.setImageBitmap(src)
        upd()
    }

    private fun save() {
        val s = src ?: run { Toast.makeText(this, "Pehle photo pick karo", Toast.LENGTH_SHORT).show(); return }
        try {
            val out = Bitmap.createBitmap(s.width, s.height, Bitmap.Config.ARGB_8888)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix()) }
            Canvas(out).drawBitmap(s, 0f, 0f, p)
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
