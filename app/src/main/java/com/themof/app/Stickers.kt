package com.themof.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object Stickers {
    private val list = ("😀 😂 🥰 😎 🤩 😍 😜 🥳 😴 🤯 😭 😡 👍 👏 🙌 🙏 💪 ✌️ " +
        "❤️ 🧡 💛 💚 💙 💜 🖤 💔 💯 🔥 ⚡ ✨ 🌟 💫 🎉 🎊 🎁 🏆 " +
        "🌈 ☀️ 🌙 ⭐ ☁️ ❄️ 🌸 🌹 🌴 🍀 🦋 🐶 🐱 🦁 🐼 🦄 " +
        "🍕 🍔 🍟 🍦 🍩 🍫 ☕ 🍉 🍓 🎵 🎧 🎮 📸 🚀 ✈️ 👑 💎 🕶️").split(" ")

    fun pick(a: Activity, ev: EditView) {
        if (ev.bmp == null) {
            Toast.makeText(a, "Pehle photo pick karo", Toast.LENGTH_SHORT).show()
            return
        }
        val d = a.resources.displayMetrics.density
        val g = GridLayout(a).apply { columnCount = 6; setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), 0) }
        val dlg = AlertDialog.Builder(a, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Stickers")
            .setView(ScrollView(a).apply { addView(g) })
            .setNegativeButton("Close", null).create()
        for (e in list) {
            g.addView(TextView(a).apply {
                text = e; textSize = 28f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                setPadding((6 * d).toInt(), (10 * d).toInt(), (6 * d).toInt(), (10 * d).toInt())
                setOnClickListener {
                    ev.addText(e)
                    dlg.dismiss()
                    Toast.makeText(a, "Photo pe drag karke sticker hilao", Toast.LENGTH_SHORT).show()
                }
            })
        }
        dlg.show()
    }
}
