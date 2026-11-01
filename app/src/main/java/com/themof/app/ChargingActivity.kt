package com.themof.app

import android.app.Activity
import android.content.*
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager

class ChargingActivity : Activity() {
    private lateinit var v: ChargingView
    private val h = Handler(Looper.getMainLooper())
    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == Intent.ACTION_POWER_DISCONNECTED) finish() else upd(i)
        }
    }

    private fun upd(i: Intent) {
        val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        v.target = l * 100f / s
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        v = ChargingView(this)
        v.setOnClickListener { finish() }
        setContentView(v)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        val f = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        f.addAction(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(rx, f)?.let { upd(it) }
        h.postDelayed({ finish() }, 30000)
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        try { unregisterReceiver(rx) } catch (e: Exception) {}
        super.onDestroy()
    }
}
