package com.themof.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat

object Setup {
    fun run(a: Activity) {
        if (Build.VERSION.SDK_INT >= 33) {
            a.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        if (!Settings.canDrawOverlays(a)) {
            Toast.makeText(a, "Themof ko 'Display over other apps' allow karo", Toast.LENGTH_LONG).show()
            a.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + a.packageName)))
        }
        ContextCompat.startForegroundService(a, Intent(a, ChargeService::class.java))
    }
}
