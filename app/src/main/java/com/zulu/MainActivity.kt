package com.zulu

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout

class MainActivity : Activity() {

    private lateinit var pad: ZuluPad
    private lateinit var btnF: Button
    private lateinit var btnZ: Button
    private lateinit var btnStart: Button
    private lateinit var btnMute: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val d = resources.displayMetrics.density

        pad = ZuluPad(this)

        btnF = Button(this).apply { text = "F: ON" }
        btnZ = Button(this).apply { text = "Z: ON" }
        btnStart = Button(this).apply { text = "START" }
        btnMute = Button(this).apply { text = "MUTE" }

        btnF.setOnClickListener {
            if (pad.stateF == ZuluPad.AxisState.MOM) {
                pad.stateF = ZuluPad.AxisState.OFF
                btnF.text = "F: OFF"
            } else {
                pad.stateF = ZuluPad.AxisState.MOM
                btnF.text = "F: ON"
            }
        }

        btnZ.setOnClickListener {
            if (pad.stateZ == ZuluPad.AxisState.MOM) {
                pad.stateZ = ZuluPad.AxisState.OFF
                btnZ.text = "Z: OFF"
            } else {
                pad.stateZ = ZuluPad.AxisState.MOM
                btnZ.text = "Z: ON"
            }
        }

        btnStart.setOnClickListener {
            // TODO: logique START
        }

        btnMute.setOnClickListener {
            // TODO: logique MUTE
        }

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(
            (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt()
        )

        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        column.addView(btnF, btnParams)
        column.addView(btnZ, btnParams)
        column.addView(btnStart, btnParams)
        column.addView(btnMute, btnParams)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.HORIZONTAL
        root.setBackgroundColor(Color.rgb(11, 12, 16))

        val padParams = LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        )
        val columnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        root.addView(pad, padParams)
        root.addView(column, columnParams)

        setContentView(root)
    }
}
