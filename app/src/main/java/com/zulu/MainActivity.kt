package com.zulu

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout

class AudioEngine {
    external fun nativeStart(): Boolean
    external fun nativeStop()

    companion object {
        init {
            System.loadLibrary("zulu")
        }
    }
}

class MainActivity : Activity() {

    private val engine = AudioEngine()
    private var playing = false
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(11, 12, 16))
        }

        startButton = Button(this).apply {
            text = "START"
            textSize = 24f

            setOnClickListener {
                if (playing) {
                    engine.nativeStop()
                    playing = false
                    text = "START"
                } else {
                    playing = engine.nativeStart()
                    text = if (playing) "STOP" else "ERREUR AUDIO"
                }
            }
        }

        val buttonLayout = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        layout.addView(startButton, buttonLayout)

        setContentView(layout)
    }

    override fun onPause() {
        super.onPause()

        if (playing) {
            engine.nativeStop()
            playing = false
            startButton.text = "START"
        }
    }
}
