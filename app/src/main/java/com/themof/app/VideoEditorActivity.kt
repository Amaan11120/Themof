package com.themof.app

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.*
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.RgbAdjustment
import androidx.media3.transformer.*
import java.io.File

@OptIn(UnstableApi::class)
class VideoEditorActivity : Activity() {
    private var uri: Uri? = null
    private var dur = 0L
    private var fx: List<Effect> = emptyList()
    private var mute = false
    private var busy = false
    private lateinit var vv: VideoView
    private lateinit var sb1: SeekBar
    private lateinit var sb2: SeekBar
    private lateinit var info: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rgb(r: Float, g: Float, b: Float): Effect =
        RgbAdjustment.Builder().setRedScale(r).setGreenScale(g).setBlueScale(b).build()

    private val filters: List<Pair<String, List<Effect>>> by lazy {
        listOf(
            "Original" to emptyList<Effect>(),
            "Warm" to listOf(rgb(1.15f, 1f, 0.85f)),
            "Cool" to listOf(rgb(0.9f, 1f, 1.15f)),
            "Dark" to listOf(Brightness(-0.2f)),
            "Bright" to listOf(Brightness(0.2f)),
            "Punchy" to listOf(Contrast(0.3f)),
            "Soft" to listOf(Contrast(-0.25f))
        )
    }

    private fun btn(label: String, click: (TextView) -> Unit) = TextView(this).apply {
        text = label; setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER
        setPadding(dp(16), dp(10), dp(16), dp(10))
        background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(0x26FFFFFF) }
        setOnClickListener { click(this) }
        layoutParams = LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.TRANSPARENT
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(40), dp(12), dp(8))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFF1A0B3D.toInt(), 0xFF030308.toInt()))
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(ImageView(this).apply { setImageResource(R.drawable.logo) }, LinearLayout.LayoutParams(dp(36), dp(36)).apply { rightMargin = dp(10) })
        top.addView(TextView(this).apply { text = "Video"; textSize = 26f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(btn("Pick") { pick() })
        top.addView(btn("Export") { export() })
        root.addView(top)

        vv = VideoView(this)
        root.addView(vv, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8); bottomMargin = dp(8) })
        vv.setOnClickListener { if (vv.isPlaying) vv.pause() else vv.start() }

        info = TextView(this).apply { setTextColor(0xCCFFFFFF.toInt()); textSize = 12f; text = "Video pick karo" }
        root.addView(info)

        val l = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { if (u) trimChanged(s === sb1) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        sb1 = SeekBar(this).apply { max = 1000; progress = 0; setOnSeekBarChangeListener(l) }
        sb2 = SeekBar(this).apply { max = 1000; progress = 1000; setOnSeekBarChangeListener(l) }
        root.addView(TextView(this).apply { text = "Start"; setTextColor(0xCCFFFFFF.toInt()); textSize = 12f })
        root.addView(sb1)
        root.addView(TextView(this).apply { text = "End"; setTextColor(0xCCFFFFFF.toInt()); textSize = 12f })
        root.addView(sb2)

        val row = LinearLayout(this)
        for ((n, f) in filters) row.addView(btn(n) { fx = f })
        row.addView(btn("Mute: off") { v -> mute = !mute; v.text = if (mute) "Mute: on" else "Mute: off" })
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(row); setPadding(0, dp(6), 0, dp(6)) })
        setContentView(root)
        pick()
    }

    private fun fmt(ms: Long) = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)

    private fun trimChanged(startMoved: Boolean) {
        if (sb1.progress > sb2.progress - 20) {
            if (startMoved) sb1.progress = maxOf(0, sb2.progress - 20) else sb2.progress = minOf(1000, sb1.progress + 20)
        }
        val s = dur * sb1.progress / 1000; val e = dur * sb2.progress / 1000
        info.text = "Trim: ${fmt(s)} - ${fmt(e)}  (${fmt(e - s)})"
        vv.seekTo(s.toInt())
    }

    private fun pick() {
        startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("video/*"), 1)
    }

    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        super.onActivityResult(rc, res, d)
        if (res != RESULT_OK) return
        val u = d?.data ?: return
        try {
            val r = MediaMetadataRetriever()
            r.setDataSource(this, u)
            dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            r.release()
            uri = u
            vv.setVideoURI(u)
            vv.setOnPreparedListener { it.isLooping = true; vv.seekTo(1) }
            sb1.progress = 0; sb2.progress = 1000
            info.text = "Trim: 0:00 - ${fmt(dur)}  (${fmt(dur)})  |  video pe tap = play/pause"
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun export() {
        val u = uri ?: run { Toast.makeText(this, "Pehle video pick karo", Toast.LENGTH_SHORT).show(); return }
        if (busy) return
        val st = dur * sb1.progress / 1000; val en = dur * sb2.progress / 1000
        if (en - st < 500) { Toast.makeText(this, "Trim bahut chhota hai", Toast.LENGTH_SHORT).show(); return }
        try {
            val clip = MediaItem.ClippingConfiguration.Builder().setStartPositionMs(st).setEndPositionMs(en).build()
            val item = MediaItem.Builder().setUri(u).setClippingConfiguration(clip).build()
            val ed = EditedMediaItem.Builder(item)
                .setEffects(Effects(listOf<AudioProcessor>(), fx))
                .setRemoveAudio(mute).build()
            val out = File(cacheDir, "out_${System.currentTimeMillis()}.mp4")
            busy = true; info.text = "Exporting... thoda wait karo"
            Transformer.Builder(this).setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        busy = false; save(out)
                    }
                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        busy = false; info.text = "Export error"
                        Toast.makeText(this@VideoEditorActivity, "Error: ${exportException.message}", Toast.LENGTH_LONG).show()
                    }
                }).build().start(ed, out.absolutePath)
        } catch (e: Exception) {
            busy = false
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun save(f: File) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val cv = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, "Themof_${System.currentTimeMillis()}.mp4")
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Themof")
                }
                val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv)!!
                contentResolver.openOutputStream(uri)!!.use { o -> f.inputStream().use { it.copyTo(o) } }
                info.text = "Saved in Movies/Themof"
                Toast.makeText(this, "Saved in Movies/Themof", Toast.LENGTH_LONG).show()
            } else {
                info.text = "Android 10+ chahiye save ke liye"
            }
            f.delete()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
