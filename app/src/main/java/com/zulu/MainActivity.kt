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
        super.onCreate(savedInstance
