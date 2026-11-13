package com.zulu

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "ZULU\n\nPremier démarrage réussi"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(11, 12, 16))
        }

        setContentView(title)
    }
}
