package com.zulu

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(11, 12, 16))
        }

        val pad = ZuluPad(this)

        val side = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 12, 12, 12)
        }

        val title = TextView(this).apply {
            text = "ZULU\n\nA + B\n4 doigts : billard"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
        }

        val start = Button(this).apply {
            text = "START"
        }

        val mute = Button(this).apply {
            text = "MUTE"
        }

        side.addView(title)
        side.addView(start)
        side.addView(mute)

        layout.addView(
            pad,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        )

        layout.addView(
            side,
            LinearLayout.LayoutParams(
                150,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(layout)
    }
}
