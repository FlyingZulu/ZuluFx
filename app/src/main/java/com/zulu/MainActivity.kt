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
            text = "ZULU"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
        }

        val buttonA = Button(this)
        val buttonB = Button(this)

        fun updateButtons() {
            buttonA.text = if (pad.stateA == ZuluPad.AxisState.MOM) {
                "A MOM"
            } else {
                "A OFF"
            }

            buttonB.text = if (pad.stateB == ZuluPad.AxisState.MOM) {
                "B MOM"
            } else {
                "B OFF"
            }
        }

        buttonA.setOnClickListener {
            pad.stateA = if (pad.stateA == ZuluPad.AxisState.MOM) {
                ZuluPad.AxisState.OFF
            } else {
                ZuluPad.AxisState.MOM
            }
            updateButtons()
        }

        buttonB.setOnClickListener {
            pad.stateB = if (pad.stateB == ZuluPad.AxisState.MOM) {
                ZuluPad.AxisState.OFF
            } else {
                ZuluPad.AxisState.MOM
            }
            updateButtons()
        }

        val start = Button(this).apply {
            text = "START"
        }

        val mute = Button(this).apply {
            text = "MUTE"
        }

        updateButtons()

        side.addView(title)
        side.addView(buttonA)
        side.addView(buttonB)
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
