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
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class MainActivity : Activity() {

    private val engine = AudioEngine()
    private val levels = FloatArray(2)
    private val vuHandler = Handler(Looper.getMainLooper())
    private val vuRunnable = object : Runnable {
        override fun run() {
            engine.nativeGetLevels(levels)
            vuMeter.levelL = Math.sqrt(levels[0].toDouble()).toFloat()
            vuMeter.levelR = Math.sqrt(levels[1].toDouble()).toFloat()
            vuHandler.postDelayed(this, 50)
        }
    }

    private lateinit var pad: ZuluPad
    private lateinit var vuMeter: VuMeterView
    private lateinit var bpmDial: BpmDialView
    private lateinit var segAB: SegmentedABRecView
    private lateinit var padGrid: PadGridView
    private lateinit var zuLu: ZuLuSelector
    private lateinit var diagFader: DiagFaderView
    private lateinit var flyFader: FlyFaderView
    private lateinit var mixFader: MixFaderView
    private lateinit var glitchScope: GlitchScopeView
    private lateinit var btnStart: Button
    private lateinit var btnMute: Button
    private lateinit var btnShift: Button
    private lateinit var btnMenu: Button
    private lateinit var btnTap: Button
    private lateinit var btnFx: Button
    private lateinit var fxOverlay: FrameLayout

    private var fxOpen = false
    private var activeFx = -1
    private val fxButtons = ArrayList<Button>()

    private val fxSections = listOf(
        "FILTRE" to listOf("LPF", "HPF", "BPF", "NOTCH", "PHASER"),
        "MODULATION" to listOf("TREMOLO", "AUTOPAN", "FLANGER", "CHORUS", "RING"),
        "TEMPS" to listOf("DELAY", "ECHO", "REVERB", "FREEZE"),
        "TEXTURE" to listOf("CRUSH", "DRIVE", "LO-FI", "GATER"),
        "SYNTH" to listOf("SAW", "SQUARE", "FM", "NOISE")
    )

    private val yellow = Color.rgb(245, 255, 90)
    private val pink = Color.rgb(255, 125, 205)
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
        segAB = SegmentedABRecView(this)
        padGrid = PadGridView(this)
        zuLu = ZuLuSelector(this)
        diagFader = DiagFaderView(this)
        flyFader = FlyFaderView(this)
        mixFader = MixFaderView(this)
        glitchScope = GlitchScopeView(this)
        glitchScope.visibility = View.GONE

        pad.waveform = placeholderWaveform()
        pad.onAxisChange = { ax, ay, bx, by ->
            engine.nativeSetAx(ax)
            engine.nativeSetAy(ay)
            engine.nativeSetBx(bx)
            engine.nativeSetBy(by)
        }
        pad.onPresetTouch = { i -> engine.nativeSetFilterType(i % 4) }
        pad.onBendTouch = { down ->
            val bend = down && shiftLocked
            pad.bendActive = bend
            if (bend) {
                glitchScope.visibility = View.VISIBLE
                glitchScope.start()
            } else {
                glitchScope.stop()
            }
        }

        btnStart = makeButton("START", green)
        btnMute = makeButton("MUTE", silk)
        btnTap = makeButton("\u25cf", amber)
        btnShift = makeRoundButton("SHIFT", silk, 10f)
        btnMenu = makeRoundButton("\u2699", silk, 16f)
        btnFx = makeRoundButton("FX", amber, 12f)

        btnFx.setOnClickListener {
            if (fxOpen) closeFx() else openFx()
        }

        btnStart.setOnClickListener {
            if (!playing) {
                playing = engine.nativeStart(false)
                btnStart.text = if (playing) "STOP" else "ERREUR"
            } else {
                engine.nativeStop()
                playing = false
                btnStart.text = "START"
                vuMeter.levelL = 0f
                vuMeter.levelR = 0f
            }
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
                engine.nativeSetBpm(bpmDial.bpm)
            }
            if (taps.size > 8) taps.removeAt(0)
        }

        btnShift.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (shiftLocked) {
                        shiftLocked = false
                    } else {
                        shiftHeld = true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (shiftHeld) {
                        val now = System.currentTimeMillis()
                        if (now - lastShiftUp < 300) shiftLocked = true
                        lastShiftUp = now
                    }
                    shiftHeld = false
                }
                MotionEvent.ACTION_CANCEL -> shiftHeld = false
            }
            refreshShift()
            true
        }

        btnMute.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    muteOn = true
                    btnMute.text = "MUTE \u25cf"
                    engine.nativeSetMute(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    muteOn = false
                    btnMute.text = "MUTE"
                    engine.nativeSetMute(false)
                }
            }
            true
        }

        segAB.onSegmentDown = { i ->
            when (i) {
                0 -> toggleAxis(true)
                1 -> toggleAxis(false)
                2 -> recHeld = true
            }
            refreshBlink()
        }
        segAB.onSegmentUp = { i ->
            if (i == 2) recHeld = false
            refreshBlink()
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
        val fxParams = LinearLayout.LayoutParams((36 * d).toInt(), (36 * d).toInt())
        fxParams.setMargins((6 * d).toInt(), 0, 0, 0)
        row1.addView(btnFx, fxParams)
        val menuParams = LinearLayout.LayoutParams((36 * d).toInt(), (36 * d).toInt())
        menuParams.setMargins((6 * d).toInt(), 0, 0, 0)
        row1.addView(btnMenu, menuParams)

        val row2 = LinearLayout(this)
        row2.gravity = Gravity.CENTER_VERTICAL
        row2.addView(btnShift, LinearLayout.LayoutParams((48 * d).toInt(), (48 * d).toInt()))
        val segParams = LinearLayout.LayoutParams((160 * d).toInt(), (38 * d).toInt())
        segParams.setMargins((8 * d).toInt(), 0, 0, 0)
        row2.addView(segAB, segParams)

        val row3 = LinearLayout(this)
        val row3Params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (80 * d).toInt()
        )
        row3Params.topMargin = (4 * d).toInt()
        row3.addView(padGrid, row3Params)

        btnMenu.setOnClickListener {
            padGrid.padCount = when (padGrid.padCount) {
                8 -> 10
                10 -> 12
                else -> 8
            }
            row3Params.height =
                ((if (padGrid.padCount > 8) 96 else 80) * d).toInt()
            column.requestLayout()
        }

        val row4 = LinearLayout(this)
        row4.addView(
            zuLu,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (34 * d).toInt())
        )

        val row5 = FrameLayout(this)
        val faderRow = LinearLayout(this)
        faderRow.orientation = LinearLayout.HORIZONTAL
        faderRow.setPadding(0, (8 * d).toInt(), 0, (8 * d).toInt())
        faderRow.addView(
            diagFader,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        )
        val flyParams = LinearLayout.LayoutParams((34 * d).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
        flyParams.setMargins((4 * d).toInt(), 0, 0, 0)
        faderRow.addView(flyFader, flyParams)
        row5.addView(
            faderRow,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val muteParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START
        )
        muteParams.setMargins((12 * d).toInt(), (10 * d).toInt(), 0, 0)
        row5.addView(btnMute, muteParams)
        val mixParams = FrameLayout.LayoutParams(
            (120 * d).toInt(), (26 * d).toInt(), Gravity.BOTTOM or Gravity.END
        )
        mixParams.setMargins(0, 0, (44 * d).toInt(), (10 * d).toInt())
        row5.addView(mixFader, mixParams)
        wireFaders()

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding((6 * d).toInt(), (4 * d).toInt(), (6 * d).toInt(), (8 * d).toInt())
        column.addView(row1)
        column.addView(row2, columnRowParams(d))
        column.addView(row3, columnRowParams(d))
        column.addView(row4, columnRowParams(d))
        val row5Params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (106 * d).toInt()
        )
        row5Params.topMargin = (12 * d).toInt()
        column.addView(row5, row5Params)

        val content = LinearLayout(this)
        content.orientation = LinearLayout.HORIZONTAL
        content.setBackgroundColor(Color.rgb(18, 20, 25))
        val padParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT)

        val padPane = FrameLayout(this)
        padPane.addView(
            pad,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val scopeParams = FrameLayout.LayoutParams(
            (110 * d).toInt(), (64 * d).toInt(), Gravity.TOP or Gravity.END
        )
        scopeParams.setMargins(0, (26 * d).toInt(), (10 * d).toInt(), 0)
        padPane.addView(glitchScope, scopeParams)

        content.addView(padPane, padParams)
        content.addView(
            column,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        )

        content.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val side = v.height
            val padWidth = (side * 1.1f).toInt()
            if (side > 0 && padPane.width != padWidth) {
                padParams.width = padWidth
                content.requestLayout()
            }
        }

        buildFxPanel(d)

        val root = FrameLayout(this)
        root.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            fxOverlay,
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
        vuHandler.post(vuRunnable)
    }

    private fun columnRowParams(d: Float): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        p.topMargin = (8 * d).toInt()
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

    private fun buttonBackground(round: Boolean): GradientDrawable {
        val d = resources.displayMetrics.density
        val bg = GradientDrawable()
        bg.setColor(Color.rgb(38, 42, 52))
        bg.setStroke((1 * d).toInt(), Color.rgb(75, 82, 98))
        if (round) bg.shape = GradientDrawable.OVAL else bg.cornerRadius = 6 * d
        return bg
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
        b.background = buttonBackground(false)
        return b
    }

    private fun makeRoundButton(label: String, color: Int, textSize: Float): Button {
        val b = Button(this)
        b.text = label
        b.setTextColor(color)
        b.textSize = textSize
        b.isAllCaps = false
        b.minHeight = 0
        b.minimumHeight = 0
        b.minimumWidth = 0
        b.setPadding(0, 0, 0, 0)
        b.typeface = markerFont(this)
        b.background = buttonBackground(true)
        return b
    }

    private fun toggleAxis(axisA: Boolean) {
        if (axisA) {
            segAB.aOn = segAB.aOn != true
        } else {
            segAB.bOn = segAB.bOn != true
        }
        pad.stateA = if (segAB.aOn) ZuluPad.AxisState.MOM else ZuluPad.AxisState.OFF
        pad.stateB = if (segAB.bOn) ZuluPad.AxisState.MOM else ZuluPad.AxisState.OFF
    }

    private fun shiftActive() = shiftHeld || shiftLocked

    private fun refreshShift() {
        padGrid.shiftMode = shiftActive()
        if (!shiftActive()) {
            btnShift.alpha = 1f
            pad.bendActive = false
            glitchScope.stop()
        }
    }

    private fun buildFxPanel(d: Float) {
        fxOverlay = FrameLayout(this)
        fxOverlay.setBackgroundColor(Color.argb(190, 8, 9, 12))
        fxOverlay.visibility = View.GONE
        fxOverlay.setOnClickListener { closeFx() }

        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        val panelBg = GradientDrawable()
        panelBg.setColor(Color.rgb(24, 26, 32))
        panelBg.setStroke((1 * d).toInt(), Color.rgb(75, 82, 98))
        panelBg.cornerRadius = 8 * d
        panel.background = panelBg
        panel.setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())

        val titleRow = LinearLayout(this)
        titleRow.gravity = Gravity.CENTER_VERTICAL
        val title = TextView(this)
        title.text = "FX SELECT \u00b7 v${versionLabel()}"
        title.setTextColor(Color.rgb(236, 224, 196))
        title.textSize = 16f
        title.typeface = markerFont(this)
        titleRow.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val close = Button(this)
        close.text = "\u00d7"
        close.setTextColor(silk)
        close.textSize = 16f
        close.minHeight = 0
        close.minimumHeight = 0
        close.minimumWidth = 0
        close.setPadding((10 * d).toInt(), 0, (10 * d).toInt(), 0)
        close.typeface = markerFont(this)
        close.background = buttonBackground(false)
        close.setOnClickListener { closeFx() }
        titleRow.addView(close)
        panel.addView(titleRow)

        val scroll = android.widget.ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        scroll.addView(list)

        var globalIdx = 0
        for ((section, effects) in fxSections) {
            val header = TextView(this)
            header.text = section
            header.setTextColor(amber)
            header.textSize = 11f
            header.typeface = markerFont(this)
            header.setPadding(0, (10 * d).toInt(), 0, (4 * d).toInt())
            list.addView(header)

            var row: LinearLayout? = null
            for (fx in effects) {
                if (row == null || row.childCount == 4) {
                    row = LinearLayout(this)
                    list.addView(row)
                }
                val b = makeButton(fx, silk)
                val idx = globalIdx
                b.setOnClickListener {
                    activeFx = idx
                    pad.activePreset = idx % 9
                    refreshFxHighlight()
                }
                fxButtons.add(b)
                row.addView(b, wrapParams((4 * d).toInt()))
                globalIdx++
            }
        }

        val panelParams = FrameLayout.LayoutParams(
            (430 * d).toInt(), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        )
        fxOverlay.addView(panel, panelParams)
        fxOverlay.addView(scroll, FrameLayout.LayoutParams(0, 0))
    }

    private fun refreshFxHighlight() {
        for ((i, b) in fxButtons.withIndex()) b.setTextColor(if (i == activeFx) amber else silk)
    }

    private fun openFx() {
        fxOpen = true
        refreshFxHighlight()
        fxOverlay.visibility = View.VISIBLE
    }

    private fun closeFx() {
        fxOpen = false
        fxOverlay.visibility = View.GONE
    }

    private fun versionLabel(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    private fun placeholderWaveform(n: Int = 256): FloatArray {
        val out = FloatArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val beat = (t * 8) % 1f
            val env = Math.pow((1.0 - beat).toDouble(), 3.0).toFloat()
            out[i] = env * sin(t * 40f * Math.PI.toFloat()) * 0.8f
        }
        return out
    }

    private fun refreshBlink() {
        segAB.blinkOn = blinkOn
        segAB.recHeld = recHeld
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
        vuHandler.removeCallbacks(vuRunnable)
        if (playing) {
            engine.nativeStop()
            playing = false
        }
    }

    private fun wireFaders() {
        mixFader.onValueChange = { v -> engine.nativeSetMix(v) }
        flyFader.onValueChange = { v, st ->
            engine.nativeSetFly(v)
            engine.nativeSetStutter(st)
        }
    }
}
