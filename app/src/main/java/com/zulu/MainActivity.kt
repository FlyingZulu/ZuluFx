package com.zulu

import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {

    private lateinit var pad: ZuluPad
    private lateinit var vuMeter: VuMeterView
    private lateinit var bpmDial: BpmDialView
    private lateinit var padGrid: PadGridView
    private lateinit var zuLu: ZuLuSelector
    private lateinit var diagFader: DiagFaderView
    private lateinit var jitterFader: JitterFaderView
    private lateinit var btnA: Button
    private lateinit var btnB: Button
    private lateinit var btnRec: Button
    private lateinit var btnShift: Button
    private lateinit var btnStart: Button
    private lateinit var btnMute: Button
    private lateinit var btnMenu: Button
    private lateinit var btnTap: Button

    private val yellow = Color.rgb(245, 255, 90)
    private val pink = Color.rgb(255, 125, 205)
    private val red = Color.rgb(255, 90, 70)
    private val green = Color.rgb(140, 255, 150)
    private val silk = Color.rgb(165, 172, 185)
    private val amber = Color.rgb(255, 176, 0)

    private var playing = false
    private var muteOn = false
    private var shiftHeld = false
    private var shiftLocked = false
    private var recHeld = false
    private var blinkOn = true
    private var lastShiftUp = 0L
    private val taps = ArrayList<Long>()

    private val blinkHandler = Handler(Looper.getMainLooper())
    private val blinkRunnable = object : Runnable {
        override fun run() {
            blinkOn = !blinkOn
            refreshBlink()
            blinkHandler.postDelayed(this, 400)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val d = resources.displayMetrics.density
        val margin = (16 * d).toInt()

        pad = ZuluPad(this)
        vuMeter = VuMeterView(this)
        bpmDial = BpmDialView(this)
        padGrid = PadGridView(this)
        zuLu = ZuLuSelector(this)
        diagFader = DiagFaderView(this)
        jitterFader = JitterFaderView(this)

        btnA = makeButton("A MOM", yellow)
        btnB = makeButton("B MOM", pink)
        btnRec = makeButton("REC", red)
        btnShift = makeButton("SHIFT", silk)
        btnStart = makeButton("START", green)
        btnMute = makeButton("MUTE", silk)
        btnMenu = makeButton("\u2699", silk)
        btnTap = makeButton("\u25cf", amber)

        btnA.setOnClickListener { toggleAxis(true) }
        btnB.setOnClickListener { toggleAxis(false) }
        btnStart.setOnClickListener {
            playing = !playing
            btnStart.text = if (playing) "STOP" else "START"
        }

        btnTap.setOnClickListener {
            val now = System.currentTimeMillis()
            if (taps.isNotEmpty() && now - taps.last() > 2000) taps.clear()
            taps.add(now)
            if (taps.size >= 2) {
                val recent = taps.takeLast(4)
                var sum = 0L
                for (i in 1 until recent.size) sum += recent[i] - recent[i - 1]
                bpmDial.bpm = (60000.0 * (recent.size - 1) / sum).toFloat()
            }
            if (taps.size > 8) taps.removeAt(0)
        }

        btnShift.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (shiftLocked) {
                        shiftLocked = false
                        btnShift.text = "SHIFT"
                    } else {
                        shiftHeld = true
                        btnShift.text = "SHIFT \u25b2"
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (shiftHeld) {
                        val now = System.currentTimeMillis()
                        if (now - lastShiftUp < 300) {
                            shiftLocked = true
                            btnShift.text = "SHIFT \u2611"
                        }
                        lastShiftUp = now
                    }
                    shiftHeld = false
                }
                MotionEvent.ACTION_CANCEL -> shiftHeld = false
            }
            refreshShift()
            true
        }

        btnRec.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> recHeld = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> recHeld = false
            }
            refreshBlink()
            true
        }

        btnMute.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    muteOn = true
                    btnMute.text = "MUTE \u25cf"
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    muteOn = false
                    btnMute.text = "MUTE"
                }
            }
            true
        }

        padGrid.onPadTouch = { n ->
            if (shiftActive()) padGrid.activeBank = n
        }

        val row1 = LinearLayout(this)
        row1.orientation = LinearLayout.HORIZONTAL
        row1.gravity = Gravity.CENTER_VERTICAL
        row1.addView(vuMeter, LinearLayout.LayoutParams(0, (26 * d).toInt(), 1f))
        val dialParams = LinearLayout.LayoutParams((64 * d).toInt(), (46 * d).toInt())
        dialParams.setMargins((6 * d).toInt(), 0, 0, 0)
        row1.addView(bpmDial, dialParams)
        row1.addView(btnTap, wrapParams((4 * d).toInt()))
        row1.addView(btnStart, wrapParams((4 * d).toInt()))
        row1.addView(btnMenu, wrapParams((4 * d).toInt()))

        val row2 = LinearLayout(this)
        row2.orientation = LinearLayout.HORIZONTAL
        row2.addView(btnA, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row2.addView(btnB, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row2.addView(btnRec, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row2.addView(btnShift, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val row3 = LinearLayout(this)
        row3.addView(
            padGrid,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (84 * d).toInt())
        )

        val row4 = LinearLayout(this)
        row4.addView(
            zuLu,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (34 * d).toInt())
        )

        val row5 = LinearLayout(this)
        row5.orientation = LinearLayout.HORIZONTAL
        row5.addView(diagFader, LinearLayout.LayoutParams(0, (84 * d).toInt(), 1f))
        val muteParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        muteParams.setMargins((8 * d).toInt(), 0, (8 * d).toInt(), 0)
        row5.addView(btnMute, muteParams)
        row5.addView(jitterFader, LinearLayout.LayoutParams((34 * d).toInt(), (84 * d).toInt()))

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding((6 * d).toInt(), (4 * d).toInt(), (6 * d).toInt(), (4 * d).toInt())
        column.addView(row1)
        column.addView(row2, columnRowParams(d))
        column.addView(row3, columnRowParams(d))
        column.addView(row4, columnRowParams(d))
        column.addView(row5, columnRowParams(d))

        val content = LinearLayout(this)
        content.orientation = LinearLayout.HORIZONTAL
        content.setBackgroundColor(Color.rgb(18, 20, 25))
        content.addView(
            pad,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        )
        content.addView(
            column,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        val root = FrameLayout(this)
        root.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val edgeL = View(this)
        val edgeR = View(this)
        root.addView(
            edgeL,
            FrameLayout.LayoutParams(margin, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START)
        )
        root.addView(
            edgeR,
            FrameLayout.LayoutParams(margin, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.END)
        )

        if (Build.VERSION.SDK_INT >= 29) {
            val applyExclusion = {
                val h = min(root.height, (200 * d).toInt())
                val top = (root.height - h) / 2
                edgeL.systemGestureExclusionRects = listOf(Rect(0, top, margin, top + h))
                edgeR.systemGestureExclusionRects =
                    listOf(Rect(root.width - margin, top, root.width, top + h))
            }
            root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyExclusion() }
            root.post { applyExclusion() }
        }

        root.setOnApplyWindowInsetsListener { v, insets ->
            val cut = if (Build.VERSION.SDK_INT >= 28) insets.displayCutout else null
            val dl = cut?.safeInsetLeft ?: 0
            val dr = cut?.safeInsetRight ?: 0
            val dt = cut?.safeInsetTop ?: 0
            val db = cut?.safeInsetBottom ?: 0
            val lp = content.layoutParams as FrameLayout.LayoutParams
            lp.setMargins(
                max(margin, dl), max((4 * d).toInt(), dt),
                max(margin, dr), max((4 * d).toInt(), db)
            )
            content.layoutParams = lp
            insets
        }

        setContentView(root)
        blinkHandler.post(blinkRunnable)
    }

    private fun columnRowParams(d: Float): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        p.topMargin = (6 * d).toInt()
        return p
    }

    private fun wrapParams(marginStart: Int): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        p.setMargins(marginStart, 0, 0, 0)
        return p
    }

    private fun makeButton(label: String, color: Int): Button {
        val d = resources.displayMetrics.density
        val b = Button(this)
        b.text = label
        b.setTextColor(color)
        b.textSize = 12f
        b.isAllCaps = false
        b.minHeight = 0
        b.minimumHeight = 0
        b.minimumWidth = 0
        b.setPadding((10 * d).toInt(), (6 * d).toInt(), (10 * d).toInt(), (6 * d).toInt())
        b.typeface = markerFont(this)
        val bg = GradientDrawable()
        bg.setColor(Color.rgb(38, 42, 52))
        bg.setStroke((1 * d).toInt(), Color.rgb(75, 82, 98))
        bg.cornerRadius = 6 * d
        b.background = bg
        return b
    }

    private fun toggleAxis(axisA: Boolean) {
        if (axisA) {
            val on = pad.stateA != ZuluPad.AxisState.MOM
            pad.stateA = if (on) ZuluPad.AxisState.MOM else ZuluPad.AxisState.OFF
            btnA.text = if (on) "A MOM" else "A OFF"
            btnA.setTextColor(if (on) yellow else silk)
        } else {
            val on = pad.stateB != ZuluPad.AxisState.MOM
            pad.stateB = if (on) ZuluPad.AxisState.MOM else ZuluPad.AxisState.OFF
            btnB.text = if (on) "B MOM" else "B OFF"
            btnB.setTextColor(if (on) pink else silk)
        }
    }

    private fun shiftActive() = shiftHeld || shiftLocked

    private fun refreshShift() {
        padGrid.shiftMode = shiftActive()
        if (!shiftActive()) btnShift.alpha = 1f
    }

    private fun refreshBlink() {
        btnRec.alpha = if (recHeld && !blinkOn) 0.35f else 1f
        if (shiftLocked) btnShift.alpha = if (blinkOn) 1f else 0.55f
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        blinkHandler.removeCallbacks(blinkRunnable)
    }
}
