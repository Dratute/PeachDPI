package com.peachdpi.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "🍑\nPeachDPI"
        tv.textSize = 32f
        tv.gravity = Gravity.CENTER
        tv.setTextColor(Color.parseColor("#F5EBE8"))
        tv.setBackgroundColor(Color.parseColor("#120D12"))
        setContentView(tv)
    }
}
