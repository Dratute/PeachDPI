package com.peachdpi.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(255, 244, 238))
        }

        val title = TextView(this).apply {
            text = "🍑 PeachDPI"
            textSize = 32f
            setTextColor(Color.rgb(220, 105, 100))
            gravity = Gravity.CENTER
        }

        val status = TextView(this).apply {
            text = "Обход DPI выключен"
            textSize = 18f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 32)
        }

        val button = TextView(this).apply {
            text = "ВКЛЮЧИТЬ"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(40, 18, 40, 18)
            setBackgroundColor(Color.rgb(235, 135, 125))
            setOnClickListener {
                status.text = "Движок обхода ещё не подключён"
            }
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(button)
        setContentView(layout)
    }
}
