package com.zulu

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var pad: ZuluPad
    private lateinit var btnF: Button
    private lateinit var btnZ: Button
    private lateinit var btnStart: Button
    private lateinit var btnMute: Button
    private lateinit var tvFx: TextView
    private lateinit var tvFy: TextView
    private lateinit var tvZx: TextView
    private lateinit var tvZy: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val d = resources.displayMetrics.density

        pad = ZuluPad(this)

        btnF = Button(this).apply { text = "F: ON" }
        btnZ = Button(this).apply { text = "Z: ON" }
        btnStart = Button(this).apply { text = "START" }
        btnMute = Button(this).apply { text = "MUTE" }

        tvFx = makeText(d, Color.rgb(245, 255, 90))
        tvFy = makeText(d, Color.rgb(245, 255, 90))
        tvZx = makeText(d, Color.rgb(255, 125, 205))
        tvZy = makeText(d, Color.rgb(255, 125, 205))

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
        column.gravity = Gravity.TOP
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
        column.addView(tvFx)
        column.addView(tvFy)
        column.addView(tvZx)
        column.addView(tvZy)

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
        updateTexts()
    }

    private fun makeText(d: Float, color: Int): TextView {
        val t = TextView(this)
        t.textSize = 14f
        t.setTextColor(color)
        return t
    }

    private fun updateTexts() {
        if (isDestroyed || isFinishing) return
        runOnUiThread {
            tvFx.text = "Fx: ${(pad.fx * 100).toInt()}"
            tvFy.text = "Fy: ${(pad.fy * 100).toInt()}"
            tvZx.text = "Zx: ${(pad.zx * 100).toInt()}"
            tvZy.text = "Zy: ${(pad.zy * 100).toInt()}"
        }
        Handler(Looper.getMainLooper()).postDelayed({
            updateTexts()
        }, 100)
    }
}
