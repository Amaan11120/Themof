package com.themof.app

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class ChargeService : Service() {
    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.action == Intent.ACTION_POWER_CONNECTED) launch(c)
        }
    }

    private fun channels() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("svc", "Themof service", NotificationManager.IMPORTANCE_MIN))
            nm.createNotificationChannel(NotificationChannel("charge", "Charging screen", NotificationManager.IMPORTANCE_HIGH))
        }
    }

    override fun onCreate() {
        super.onCreate()
        channels()
        val n = NotificationCompat.Builder(this, "svc")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Themof charging animation on")
            .setPriority(NotificationCompat.PRIORITY_MIN).build()
        ServiceCompat.startForeground(this, 1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        registerReceiver(rx, IntentFilter(Intent.ACTION_POWER_CONNECTED))
    }

    private fun launch(c: Context) {
        val i = Intent(c, ChargingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Settings.canDrawOverlays(c)) {
            c.startActivity(i)
        } else {
            val pi = PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val n = NotificationCompat.Builder(c, "charge")
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle("Charging")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true).setAutoCancel(true).build()
            (c.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(2, n)
        }
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { try { unregisterReceiver(rx) } catch (e: Exception) {}; super.onDestroy() }
}
