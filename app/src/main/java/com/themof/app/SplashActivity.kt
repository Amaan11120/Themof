package com.themof.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

class SplashActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        val d = resources.displayMetrics.density
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val iv = ImageView(this).apply {
            setImageResource(R.drawable.logo)
            alpha = 0f; scaleX = 0.7f; scaleY = 0.7f
        }
        val s = (resources.displayMetrics.widthPixels * 0.75).toInt()
        root.addView(iv, FrameLayout.LayoutParams(s, s, Gravity.CENTER))
        val tag = TextView(this).apply {
            text = "THEMES  •  STUDIO  •  CHARGING"
            setTextColor(0x99FFFFFF.toInt()); textSize = 11f
            letterSpacing = 0.25f; gravity = Gravity.CENTER; alpha = 0f
        }
        root.addView(tag, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = (64 * d).toInt() })
        setContentView(root)
        iv.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(900)
            .setInterpolator(OvershootInterpolator(1.1f)).start()
        tag.animate().alpha(1f).setStartDelay(500).setDuration(600).start()
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 1900)
    }
}
