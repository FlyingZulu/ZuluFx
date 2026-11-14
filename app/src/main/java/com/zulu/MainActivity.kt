package com.zulu

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_main)

        pad = findViewById(R.id.zuluPad)
        btnF = findViewById(R.id.btnF)
        btnZ = findViewById(R.id.btnZ)
        btnStart = findViewById(R.id.btnStart)
        btnMute = findViewById(R.id.btnMute)
        tvFx = findViewById(R.id.tvFx)
        tvFy = findViewById(R.id.tvFy)
        tvZx = findViewById(R.id.tvZx)
        tvZy = findViewById(R.id.tvZy)

        // Bascule AXIS F
        btnF.setOnClickListener {
            if (pad.stateF == ZuluPad.AxisState.MOM) {
                pad.stateF = ZuluPad.AxisState.OFF
                btnF.text = "F: OFF"
            } else {
                pad.stateF = ZuluPad.AxisState.MOM
                btnF.text = "F: ON"
            }
        }

        // Bascule AXIS Z
        btnZ.setOnClickListener {
            if (pad.stateZ == ZuluPad.AxisState.MOM) {
                pad.stateZ = ZuluPad.AxisState.OFF
                btnZ.text = "Z: OFF"
            } else {
                pad.stateZ = ZuluPad.AxisState.MOM
                btnZ.text = "Z: ON"
            }
        }

        // Bouton START
        btnStart.setOnClickListener {
            // TODO: logique START
        }

        // Bouton MUTE
        btnMute.setOnClickListener {
            // TODO: logique MUTE
        }

        // Mise à jour des textes
        updateTexts()
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun updateTexts() {
        tvFx.text = "Fx: ${pad.posFx.toInt()}"
        tvFy.text = "Fy: ${pad.posFy.toInt()}"
        tvZx.text = "Zx: ${pad.posZx.toInt()}"
        tvZy.text = "Zy: ${pad.posZy.toInt()}"
        handler.postDelayed({ updateTexts() }, 100)
    }
}
