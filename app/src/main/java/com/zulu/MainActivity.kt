package com.zulu

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.net.Uri
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.concurrent.thread
import java.util.zip.ZipInputStream
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private val engine = AudioEngine()
    private val levels = FloatArray(2)
    private val vuHandler = Handler(Looper.getMainLooper())
    private val recEndHandler = Handler(Looper.getMainLooper())
    private val vuRunnable = object : Runnable {
        override fun run() {
            engine.nativeGetLevels(levels)
            vuMeter.levelL = Math.sqrt(levels[0].toDouble()).toFloat()
            vuMeter.levelR = Math.sqrt(levels[1].toDouble()).toFloat()
            vuHandler.postDelayed(this, 50)
        }
    }

    private lateinit var pad: ZuluPad
    private lateinit var vuMeter: BarVuView
    private lateinit var bpmLcd: BpmLcdView
    private lateinit var padGrid: PadGridView
    private lateinit var zuLuFader: VFaderView
    private lateinit var flyFader: FlyFaderView
    private lateinit var mixFader: VFaderView
    private lateinit var glitchScope: GlitchScopeView
    private lateinit var btnMute: Button
    private lateinit var btnShift: Button
    private lateinit var btnFx: Button
    private lateinit var btnF: Button
    private lateinit var btnZ: Button
    private lateinit var btnRec: Button
    private lateinit var btnZu: Button
    private lateinit var btnLu: Button
    private lateinit var fxOverlay: FrameLayout
    private lateinit var scaleOverlay: FrameLayout
    private lateinit var projOverlay: FrameLayout
    private lateinit var gridParams: LinearLayout.LayoutParams
    private val projSlotButtons = ArrayList<Button>()
    private lateinit var arpOverlay: FrameLayout
    private lateinit var padPane: FrameLayout

    private var fxOpen = false
    private var fxHeld = false
    private var fxConsumed = false
    private var activeFx = -1
    private val assignNames = listOf("CUTOFF", "RESO", "REV", "BITS", "VIT.", "LFO V.", "LFO P.", "DRIVE")
    private var assignXIdx = 0
    private var assignYIdx = 1
    private var btnAssignX: Button? = null
    private var btnAssignY: Button? = null
    private var loopLenSteps = 8
    private var loopWetA = false
    private var loopWetB = false
    private var activeScale = 0
    private var activeRoot = 9
    private var arpOn = false
    private var arpMode = 0
    private var arpRateIdx = 1
    private var arpOctIdx = 0
    private var arpGateIdx = 1
    private var arpOctJump = false
    private var arpSlicesIdx = 2
    private val arpButtons = ArrayList<Button>()

    private var activeFilter = 0
    private var padAxisX = 0.5f
    private val userPresets = arrayOfNulls<UserPreset>(9)

    private data class Snap(
        val ax: Float,
        val ay: Float,
        val bx: Float,
        val by: Float,
        val mix: Float,
        val fly: Float,
        val filter: Int,
        val assignX: Int,
        val assignY: Int
    )
    private val snapPads = arrayOfNulls<Snap>(14)
    private var activeZu = -1
    private var activeLu = -1
    private var autoZu: Snap? = null
    private var autoLu: Snap? = null
    private val stepPitch = Array(8) { FloatArray(14) }
    private val stepVel = Array(8) { FloatArray(14) { 1f } }
    private val bankSteps = IntArray(8)
    private var stepEditBank = -1
    private var stepEditor: StepEditorView? = null
    private var stepTitle: TextView? = null
    private lateinit var stepOverlay: FrameLayout
    private var zuDownAt = 0L
    private var luDownAt = 0L
    private lateinit var menuOverlay: FrameLayout
    private val menuPadButtons = ArrayList<Button>()
    private var menuMirrorBtn: Button? = null
    private val menuCurveButtons = ArrayList<Button>()
    private val menuFingerButtons = ArrayList<Button>()
    private var menuFreezeBtn: Button? = null
    private val menuBillardButtons = ArrayList<Button>()
    private val menuLofiButtons = ArrayList<Button>()
    private var recFlavor = 0
    private val menuProjButtons = ArrayList<Button>()
    private var morphCurve = 0
    private var fingerModeOpt = 0
    private var bendFreezeOpt = false
    private var synWaveIdx = 0
    private var synAtkIdx = 0
    private var synDecIdx = 1
    private var synOctIdx = 2
    private var synPitIdx = 2
    private var synTunIdx = 1
    private val lfoShapes = Array(2) { FloatArray(64) { 0.5f } }
    private val lfoOn = BooleanArray(2)
    private val lfoRateIdxArr = IntArray(2) { 2 }
    private val lfoDepthIdxArr = IntArray(2) { 2 }
    private val lfoAmountIdxArr = IntArray(2) { 3 }
    private val lfoTargets = IntArray(2) { -1 }
    private val lfoWaves = IntArray(2) { 0 }
    private var lfoSel = 0
    private var lfoDraw: LfoDrawView? = null
    private var importTargetBank = -1
    private val reqImport = 4242

    private data class UserPreset(
        val filter: Int,
        val scale: Int,
        val root: Int,
        val arpOn: Boolean,
        val arpMode: Int,
        val arpRate: Int,
        val arpOct: Int,
        val arpGate: Int,
        val arpOctJump: Boolean,
        val arpSlices: Int,
        val assignX: Int,
        val assignY: Int
    )
    private val fxButtons = ArrayList<Button>()

    private val fxSections = listOf(
        "FILTRE" to listOf("LPF", "HPF", "BPF", "NOTCH", "PHASER"),
        "MODULATION" to listOf("TREMOLO", "AUTOPAN", "FLANGER", "CHORUS", "RING"),
        "TEMPS" to listOf("DELAY", "ECHO", "REVERB", "FREEZE", "STOP"),
        "TEXTURE" to listOf("CRUSH", "DRIVE", "LO-FI", "GATER"),
        "SYNTH" to listOf("SAW", "SQUARE", "FM", "NOISE")
    )

    private val fxCodes = listOf(
        "F0", "F1", "F2", "F3", "X2",
        "X3", "X4", "X5", "X6", "X7",
        "X0", "X1", "P2", "X10", "X12",
        "P3", "P7", "X8", "X9",
        "G0", "G13", "G14", "G12"
    )
    private val fxActive = BooleanArray(13)
    private val paramActive = BooleanArray(8)
    private val paramDefaults = floatArrayOf(0f, 0f, 0.35f, 0.3f, 0f, 0f, 0f, 0.4f)
    private var genIdx = 0

    private val yellow = Color.rgb(245, 255, 90)
    private val pink = Color.rgb(255, 125, 205)
    private val green = Color.rgb(140, 255, 150)
    private val silk = Color.rgb(165, 172, 185)
    private val amber = Color.rgb(255, 176, 0)
    private val turquoise = Color.rgb(0, 229, 255)
    private val violet = Color.rgb(170, 140, 255)
    private val red = Color.rgb(255, 90, 70)

    private var playing = false
    private var simulLock = false
    private var resampling = false
    private var muteOn = false
    private val soloGroups = booleanArrayOf(false, false, false, false)
    private var shiftHeld = false
    private var shiftLocked = false
    private var recHeld = false
    private var blinkOn = true
    private var axisAOn = true
    private var axisBOn = true
    private var axisAHold = false
    private var axisBHold = false
    private var fDownAt = 0L
    private var zDownAt = 0L
    private var mirrored = false
    private lateinit var content: LinearLayout
    private var lastShiftUp = 0L

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
        val margin = (24 * d).toInt()

        pad = ZuluPad(this)
        vuMeter = BarVuView(this)
        bpmLcd = BpmLcdView(this)
        padGrid = PadGridView(this)
        padGrid.padCount = 12
        zuLuFader = VFaderView(this, "ZU/LU", blendMorph = true)
        flyFader = FlyFaderView(this)
        mixFader = VFaderView(this, "MIX", thumbColor = Color.rgb(90, 200, 90))
        mixFader.value = 1f
        glitchScope = GlitchScopeView(this)
        glitchScope.visibility = View.GONE

        pad.waveform = placeholderWaveform()
        pad.onAxisChange = { ax, ay, bx, by ->
            engine.nativeSetAx(ax)
            engine.nativeSetAy(ay)
            engine.nativeSetBx(bx)
            engine.nativeSetBy(by)
            padAxisX = ax
            if (arpOn && arpMode == 4) engine.nativeSetArpXy(ax, true)
        }
        pad.onPadTouchState = { t ->
            if (!t) engine.nativeSetArpXy(padAxisX, false)
        }
        pad.onPresetTouch = { i ->
            if (fxHeld) {
                fxConsumed = true
                saveUserPreset(i)
            } else {
                val p = userPresets[i]
                if (p != null) {
                    applyUserPreset(p)
                } else {
                    activeFilter = i % 4
                    engine.nativeSetFilterType(activeFilter)
                }
            }
        }
        pad.onPresetLongTouch = { i -> saveUserPreset(i) }
        pad.onArpTouch = {
            if (shiftActive()) {
                openArpPanel()
            } else {
                arpOn = !arpOn
                pushArp()
                pad.arpOn = arpOn
                pad.invalidate()
            }
        }
        pad.onBendTouch = { down ->
            val bend = down && shiftLocked
            pad.bendActive = bend
            if (bend) {
                if (activeFx < 0) {
                    val r = (0..3).random()
                    activeFilter = r
                    engine.nativeSetFilterType(r)
                    activeFx = r
                    assignXIdx = (0..6).random()
                    assignYIdx = (0..6).random()
                    engine.nativeSetAssign(0, assignXIdx)
                    engine.nativeSetAssign(1, assignYIdx)
                    refreshAssignLabels()
                    refreshFxHighlight()
                }
                glitchScope.visibility = View.VISIBLE
                glitchScope.start()
            } else {
                engine.nativeSetBend(false, 0.5f, 0.5f)
                glitchScope.stop()
            }
        }
        pad.onBendPos = { x, y ->
            if (pad.bendActive) engine.nativeSetBend(true, x, y)
        }

        btnMute = makeRoundButton("MUTE", silk, 10f)
        btnShift = makeRoundButton("SHIFT", silk, 10f)
        btnFx = makeRoundButton("FX", amber, 12f)
        btnF = makeButton("F", yellow)
        btnZ = makeButton("Z", pink)
        btnRec = makeRoundButton("REC", red, 10f)
        btnZu = makeButton("ZU", turquoise)
        btnLu = makeButton("LU", violet)

        btnZu.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    zuDownAt = System.currentTimeMillis()
                    btnZu.text = "ZU\u00b7CAP"
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    btnZu.text = "ZU"
                    if (System.currentTimeMillis() - zuDownAt >= 400) {
                        captureSnap(0)
                    } else if (e.actionMasked == MotionEvent.ACTION_UP) {
                        zuLuFader.value = 0f
                        applyMorph(0f)
                    }
                }
            }
            true
        }
        btnLu.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    luDownAt = System.currentTimeMillis()
                    btnLu.text = "LU\u00b7CAP"
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    btnLu.text = "LU"
                    if (System.currentTimeMillis() - luDownAt >= 400) {
                        captureSnap(1)
                    } else if (e.actionMasked == MotionEvent.ACTION_UP) {
                        zuLuFader.value = 1f
                        applyMorph(1f)
                    }
                }
            }
            true
        }

        btnFx.setOnClickListener {
            if (fxConsumed) {
                fxConsumed = false
            } else if (fxOpen) {
                closeFx()
            } else {
                openFx()
            }
        }
        btnFx.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    fxHeld = true
                    fxConsumed = false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> fxHeld = false
            }
            false
        }

        vuMeter.onPressed = { _ -> checkSimulStart() }
        bpmLcd.onPressed = { _ -> checkSimulStart() }

        bpmLcd.onBpmChange = { b -> engine.nativeSetBpm(b) }

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
                    if (shiftActive()) {
                        resampling = true
                        startResampleUI()
                    } else {
                        muteOn = true
                        btnMute.text = "MUTE \u25cf"
                        engine.nativeSetMute(true)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (resampling) {
                        resampling = false
                    } else {
                        muteOn = false
                        btnMute.text = "MUTE"
                        engine.nativeSetMute(false)
                    }
                }
            }
            true
        }

        btnF.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    fDownAt = System.currentTimeMillis()
                    if (fxHeld) loopWetA = true
                    refreshBlink()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    loopWetA = false
                    if (!fxHeld) {
                        if (System.currentTimeMillis() - fDownAt >= 400) {
                            axisAHold = !axisAHold
                            if (axisAOn) {
                                pad.stateA = if (axisAHold) ZuluPad.AxisState.HOLD else ZuluPad.AxisState.MOM
                            }
                        } else {
                            toggleAxis(true)
                        }
                    }
                    refreshBlink()
                }
            }
            true
        }
        btnZ.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    zDownAt = System.currentTimeMillis()
                    if (fxHeld) loopWetB = true
                    refreshBlink()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    loopWetB = false
                    if (!fxHeld) {
                        if (System.currentTimeMillis() - zDownAt >= 400) {
                            axisBHold = !axisBHold
                            if (axisBOn) {
                                pad.stateB = if (axisBHold) ZuluPad.AxisState.HOLD else ZuluPad.AxisState.MOM
                            }
                        } else {
                            toggleAxis(false)
                        }
                    }
                    refreshBlink()
                }
            }
            true
        }
        btnRec.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> recHeld = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> recHeld = false
            }
            refreshBlink()
            false
        }

        padGrid.onPadTouch = { n ->
            when {
                fxHeld -> {
                    fxConsumed = true
                    loopLenSteps = n
                    padGrid.activeBank = n
                }
                shiftActive() -> {
                    fxConsumed = true
                    startLoopRecording(n)
                }
                padGrid.padCount == 12 && n in 9..12 -> toggleSolo(n - 9)
                n - 1 < 14 && snapPads[n - 1] != null -> recallSnap(n - 1)
                n <= 8 -> {
                    if (engine.nativeBankHasLoop(n - 1)) {
                        engine.nativeToggleBank(n - 1)
                    } else {
                        startLoopRecording(n)
                    }
                }
                else -> engine.nativeJumpSlice(n, padGrid.padCount)
            }
        }

        padGrid.onPadLongTouch = { n ->
            if (!shiftActive() && !fxHeld && n in 1..8 && engine.nativeBankHasLoop(n - 1)) {
                openStepEditor(n - 1)
            }
        }

        val vuModule = LinearLayout(this)
        vuModule.orientation = LinearLayout.VERTICAL
        vuModule.gravity = Gravity.CENTER_HORIZONTAL
        vuModule.addView(vuMeter, LinearLayout.LayoutParams((48 * d).toInt(), (40 * d).toInt()))
        val lcdParams = LinearLayout.LayoutParams((110 * d).toInt(), (24 * d).toInt())
        lcdParams.topMargin = (4 * d).toInt()
        vuModule.addView(bpmLcd, lcdParams)

        val row1 = LinearLayout(this)
        row1.orientation = LinearLayout.HORIZONTAL
        row1.gravity = Gravity.CENTER_VERTICAL
        row1.addView(btnFx, LinearLayout.LayoutParams((36 * d).toInt(), (36 * d).toInt()))
        val sqParams = LinearLayout.LayoutParams((34 * d).toInt(), (34 * d).toInt())
        sqParams.setMargins((4 * d).toInt(), 0, 0, 0)
        row1.addView(btnF, sqParams)
        row1.addView(btnZ, sqParams)
        val recParams = LinearLayout.LayoutParams((40 * d).toInt(), (40 * d).toInt())
        recParams.setMargins((4 * d).toInt(), 0, 0, 0)
        row1.addView(btnRec, recParams)
        val vuWrap = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        vuWrap.setMargins((6 * d).toInt(), 0, 0, 0)
        row1.addView(vuModule, vuWrap)

        val zuLuBtns = LinearLayout(this)
        zuLuBtns.orientation = LinearLayout.HORIZONTAL
        val zuParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, (20 * d).toInt()
        )
        zuParams.setMargins((2 * d).toInt(), (6 * d).toInt(), 0, 0)
        zuLuBtns.addView(btnZu, zuParams)
        zuLuBtns.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val luParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, (20 * d).toInt()
        )
        luParams.setMargins(0, (6 * d).toInt(), (2 * d).toInt(), 0)
        zuLuBtns.addView(btnLu, luParams)

        wireFaders()

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding((6 * d).toInt(), (4 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
        column.addView(row1)
        gridParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (96 * d).toInt()
        )
        gridParams.setMargins(0, (6 * d).toInt(), 0, 0)
        column.addView(padGrid, gridParams)
        column.addView(zuLuBtns)
        column.addView(
            zuLuFader,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )
        val faderRow = LinearLayout(this)
        faderRow.orientation = LinearLayout.HORIZONTAL
        val flyParams = LinearLayout.LayoutParams(0, (40 * d).toInt(), 3f)
        flyParams.setMargins((3 * d).toInt(), 0, (3 * d).toInt(), 0)
        faderRow.addView(flyFader, flyParams)
        val mixParams = LinearLayout.LayoutParams(0, (40 * d).toInt(), 2f)
        mixParams.setMargins((3 * d).toInt(), 0, (3 * d).toInt(), 0)
        faderRow.addView(mixFader, mixParams)
        val faderRowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (40 * d).toInt()
        )
        faderRowParams.topMargin = (8 * d).toInt()
        column.addView(faderRow, faderRowParams)

        content = LinearLayout(this)
        content.orientation = LinearLayout.HORIZONTAL
        content.setBackgroundColor(Color.rgb(18, 20, 25))
        val padParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT)

        padPane = FrameLayout(this)

        val leftColumn = LinearLayout(this)
        leftColumn.orientation = LinearLayout.VERTICAL
        val padPaneParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        leftColumn.addView(padPane, padPaneParams)

        vuMeter.onLongPress = {
            if (!simulLock) {
                if (shiftActive()) {
                    mirrored = !mirrored
                    content.layoutDirection =
                        if (mirrored) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                } else {
                    openMenuPanel()
                }
            }
        }

        vuMeter.onDoubleTap = {
            if (!simulLock && shiftActive()) {
                openScalePanel()
            } else {
                applyPadCount(
                    when (padGrid.padCount) {
                        8 -> 10
                        10 -> 12
                        12 -> 14
                        else -> 8
                    }
                )
            }
        }

        val edgeColumn = LinearLayout(this)
        edgeColumn.orientation = LinearLayout.VERTICAL
        edgeColumn.setPadding((3 * d).toInt(), (6 * d).toInt(), (3 * d).toInt(), (6 * d).toInt())
        edgeColumn.addView(btnShift, LinearLayout.LayoutParams((40 * d).toInt(), (40 * d).toInt()))
        edgeColumn.addView(View(this), LinearLayout.LayoutParams(0, 0, 1f))
        edgeColumn.addView(btnMute, LinearLayout.LayoutParams((40 * d).toInt(), (40 * d).toInt()))
        padPane.addView(
            pad,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val scopeParams = FrameLayout.LayoutParams(
            (110 * d).toInt(), (64 * d).toInt(), Gravity.TOP or Gravity.RIGHT
        )
        scopeParams.setMargins(0, (26 * d).toInt(), (10 * d).toInt(), 0)
        padPane.addView(glitchScope, scopeParams)

        content.addView(leftColumn, padParams)
        content.addView(
            edgeColumn,
            LinearLayout.LayoutParams((46 * d).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
        )
        content.addView(
            column,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        )

        leftColumn.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val side = padPane.height
            if (side > 0 && padPane.width != side) {
                padParams.width = side
                leftColumn.requestLayout()
            }
        }

        buildFxPanel(d)
        buildScalePanel(d)
        buildArpPanel(d)
        buildProjPanel(d)
        buildStepPanel(d)
        buildMenuPanel(d)
        padPane.addView(
            arpOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        padPane.addView(
            stepOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
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
        root.addView(
            fxOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            scaleOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            projOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            menuOverlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val edgeL = View(this)
        val edgeR = View(this)
        root.addView(
            edgeL,
            FrameLayout.LayoutParams(margin, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.LEFT)
        )
        root.addView(
            edgeR,
            FrameLayout.LayoutParams(margin, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.RIGHT)
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
            axisAOn = !axisAOn
            pad.stateA = when {
                !axisAOn -> ZuluPad.AxisState.OFF
                axisAHold -> ZuluPad.AxisState.HOLD
                else -> ZuluPad.AxisState.MOM
            }
        } else {
            axisBOn = !axisBOn
            pad.stateB = when {
                !axisBOn -> ZuluPad.AxisState.OFF
                axisBHold -> ZuluPad.AxisState.HOLD
                else -> ZuluPad.AxisState.MOM
            }
        }
        updateAxisButtons()
    }

    private fun updateAxisButtons() {
        btnF.setTextColor(if (axisAOn) yellow else silk)
        btnF.text = if (axisAHold) "F\u00b7H" else "F"
        btnZ.setTextColor(if (axisBOn) pink else silk)
        btnZ.text = if (axisBHold) "Z\u00b7H" else "Z"
    }

    private fun shiftActive() = shiftHeld || shiftLocked

    private fun checkSimulStart() {
        if (!simulLock && vuMeter.pressed && bpmLcd.pressed) {
            simulLock = true
            bpmLcd.inputEnabled = false
            togglePlay()
        } else if (simulLock && !vuMeter.pressed && !bpmLcd.pressed) {
            simulLock = false
            bpmLcd.inputEnabled = true
        }
    }

    private fun toggleSolo(group: Int) {
        soloGroups[group] = !soloGroups[group]
        var mask = 0
        for (g in 0 until 4) if (soloGroups[g]) mask = mask or (1 shl g)
        engine.nativeSetSoloMask(mask)
        padGrid.soloMask = mask
    }

    private fun startResampleUI() {
        val bank = (0 until 8).firstOrNull { !engine.nativeBankHasLoop(it) } ?: return
        engine.nativeResample(bank, loopLenSteps)
        resetSteps(bank)
        padGrid.recPad = bank
        val ms = (loopLenSteps * 60000f / bpmLcd.bpm.coerceAtLeast(40f)).toLong()
        recEndHandler.removeCallbacksAndMessages(null)
        recEndHandler.postDelayed({ padGrid.recPad = -1 }, ms + 200)
    }

    private fun togglePlay() {
        if (!playing) {
            playing = engine.nativeStart(false)
        } else {
            engine.nativeStop()
            playing = false
            vuMeter.levelL = 0f
            vuMeter.levelR = 0f
        }
    }

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
                    val code = fxCodes.getOrNull(idx) ?: ""
                    if (code.length >= 2) {
                        val num = code.substring(1).toIntOrNull() ?: 0
                        when (code[0]) {
                            'F' -> {
                                activeFilter = num
                                engine.nativeSetFilterType(num)
                            }
                            'X' -> {
                                fxActive[num] = !fxActive[num]
                                engine.nativeSetFx(num, fxActive[num])
                            }
                            'P' -> {
                                paramActive[num] = !paramActive[num]
                                engine.nativeSetParam(
                                    num,
                                    if (paramActive[num]) paramDefaults[num] else 0f,
                                    paramActive[num]
                                )
                            }
                            'G' -> {
                                genIdx = num
                                engine.nativeSetGen(num)
                            }
                        }
                    }
                    pad.activePreset = idx % 9
                    refreshFxHighlight()
                }
                fxButtons.add(b)
                row.addView(b, wrapParams((4 * d).toInt()))
                globalIdx++
            }
        }

        val assignHeader = TextView(this)
        assignHeader.text = "ASSIGNATION AXES"
        assignHeader.setTextColor(amber)
        assignHeader.textSize = 11f
        assignHeader.typeface = markerFont(this)
        assignHeader.setPadding(0, (10 * d).toInt(), 0, (4 * d).toInt())
        list.addView(assignHeader)

        val assignRow = LinearLayout(this)
        val bax = makeButton("X : CUTOFF", yellow)
        bax.setOnClickListener {
            assignXIdx = (assignXIdx + 1) % assignNames.size
            engine.nativeSetAssign(0, assignXIdx)
            refreshAssignLabels()
        }
        val bay = makeButton("Y : RESO", pink)
        bay.setOnClickListener {
            assignYIdx = (assignYIdx + 1) % assignNames.size
            engine.nativeSetAssign(1, assignYIdx)
            refreshAssignLabels()
        }
        btnAssignX = bax
        btnAssignY = bay
        assignRow.addView(bax, wrapParams((4 * d).toInt()))
        assignRow.addView(bay, wrapParams((4 * d).toInt()))
        list.addView(assignRow)

        val panelParams = FrameLayout.LayoutParams(
            (430 * d).toInt(), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        )
        fxOverlay.addView(panel, panelParams)
        fxOverlay.addView(scroll, FrameLayout.LayoutParams(0, 0))
    }

    private fun refreshFxHighlight() {
        for ((i, b) in fxButtons.withIndex()) {
            val code = fxCodes.getOrNull(i) ?: ""
            val num = if (code.length >= 2) code.substring(1).toIntOrNull() ?: -1 else -1
            val on = when (code.getOrNull(0)) {
                'F' -> activeFilter == num
                'X' -> num in fxActive.indices && fxActive[num]
                'P' -> num in paramActive.indices && paramActive[num]
                'G' -> genIdx == num
                else -> false
            }
            b.setTextColor(if (on) amber else silk)
        }
    }

    private fun refreshAssignLabels() {
        btnAssignX?.text = "X : ${assignNames[assignXIdx]}"
        btnAssignY?.text = "Y : ${assignNames[assignYIdx]}"
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

    private fun buildScalePanel(d: Float) {
        scaleOverlay = FrameLayout(this)
        scaleOverlay.setBackgroundColor(Color.argb(190, 8, 9, 12))
        scaleOverlay.visibility = View.GONE
        scaleOverlay.setOnClickListener { closeScalePanel() }

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
        title.text = "GAMME"
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
        close.setOnClickListener { closeScalePanel() }
        titleRow.addView(close)
        panel.addView(titleRow)

        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        panel.addView(list)

        val scales = listOf("MIN PENTA", "MAJ PENTA", "HIRAJOSHI", "IN SEN", "IWATO", "CHROM")
        val scaleHeader = TextView(this)
        scaleHeader.text = "GAMME"
        scaleHeader.setTextColor(amber)
        scaleHeader.textSize = 11f
        scaleHeader.typeface = markerFont(this)
        scaleHeader.setPadding(0, (10 * d).toInt(), 0, (4 * d).toInt())
        list.addView(scaleHeader)
        var row: LinearLayout? = null
        for ((i, name) in scales.withIndex()) {
            if (row == null || row.childCount == 3) {
                row = LinearLayout(this)
                list.addView(row)
            }
            val b = makeButton(name, silk)
            b.setOnClickListener {
                activeScale = i
                engine.nativeSetScale(i)
                refreshScaleHighlight()
            }
            row.addView(b, wrapParams((4 * d).toInt()))
        }

        val roots = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val rootHeader = TextView(this)
        rootHeader.text = "FONDAMENTALE"
        rootHeader.setTextColor(amber)
        rootHeader.textSize = 11f
        rootHeader.typeface = markerFont(this)
        rootHeader.setPadding(0, (12 * d).toInt(), 0, (4 * d).toInt())
        list.addView(rootHeader)
        var rootRow: LinearLayout? = null
        for ((i, name) in roots.withIndex()) {
            if (rootRow == null || rootRow.childCount == 4) {
                rootRow = LinearLayout(this)
                list.addView(rootRow)
            }
            val b = makeButton(name, silk)
            b.setOnClickListener {
                activeRoot = i
                engine.nativeSetRoot(i)
                refreshScaleHighlight()
            }
            rootRow.addView(b, wrapParams((4 * d).toInt()))
        }

        val panelParams = FrameLayout.LayoutParams(
            (430 * d).toInt(), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        )
        scaleOverlay.addView(panel, panelParams)
    }

    private fun refreshScaleHighlight() {
        val panel = scaleOverlay.getChildAt(0) as? LinearLayout ?: return
        val list = panel.getChildAt(1) as? LinearLayout ?: return
        var scaleBtnIdx = 0
        var rootBtnIdx = 0
        for (r in 0 until list.childCount) {
            val rowView = list.getChildAt(r) as? LinearLayout ?: continue
            for (c in 0 until rowView.childCount) {
                val b = rowView.getChildAt(c) as? Button ?: continue
                if (r < 2) {
                    b.setTextColor(if (scaleBtnIdx == activeScale) amber else silk)
                    scaleBtnIdx++
                } else {
                    b.setTextColor(if (rootBtnIdx == activeRoot) amber else silk)
                    rootBtnIdx++
                }
            }
        }
    }

    private fun openScalePanel() {
        refreshScaleHighlight()
        scaleOverlay.visibility = View.VISIBLE
    }

    private fun closeScalePanel() {
        scaleOverlay.visibility = View.GONE
    }

    private fun projDir(): File = File(filesDir, "projects")

    private fun projFile(slot: Int): File = File(projDir(), "proj_$slot.json")

    private fun bankFile(slot: Int, bank: Int): File =
        File(projDir(), "proj_${slot}_b$bank.pcm")

    private fun buildProjPanel(d: Float) {
        projOverlay = FrameLayout(this)
        projOverlay.setBackgroundColor(Color.argb(190, 8, 9, 12))
        projOverlay.visibility = View.GONE
        projOverlay.setOnClickListener { closeProjPanel() }

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
        title.text = "PROJET"
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
        close.setPadding(0, 0, 0, 0)
        close.background = null
        close.setOnClickListener { closeProjPanel() }
        titleRow.addView(close, LinearLayout.LayoutParams((30 * d).toInt(), (30 * d).toInt()))
        panel.addView(titleRow)

        val hint = TextView(this)
        hint.text = "TAP = CHARGER \u00b7 APPUI LONG = SAUVER"
        hint.setTextColor(Color.rgb(120, 128, 146))
        hint.textSize = 10f
        panel.addView(hint)

        for (s in 1..8) {
            val b = Button(this)
            b.text = "PROJ $s"
            b.setTextColor(silk)
            b.textSize = 14f
            b.minHeight = 0
            b.minimumHeight = 0
            b.setPadding((8 * d).toInt(), (6 * d).toInt(), (8 * d).toInt(), (6 * d).toInt())
            val bg = GradientDrawable()
            bg.setColor(Color.rgb(16, 18, 24))
            bg.setStroke((1 * d).toInt(), Color.rgb(60, 66, 82))
            bg.cornerRadius = 6 * d
            b.background = bg
            b.setOnClickListener { loadProject(s) }
            b.setOnLongClickListener { saveProject(s); true }
            projSlotButtons.add(b)
            panel.addView(
                b,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (6 * d).toInt() }
            )
        }

        projOverlay.addView(
            panel,
            FrameLayout.LayoutParams(
                (240 * d).toInt(),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
    }

    private fun refreshProjSlots() {
        for (s in 1..8) {
            val b = projSlotButtons[s - 1]
            b.text = if (projFile(s).exists()) "PROJ $s \u25cf" else "PROJ $s"
        }
    }

    private fun openProjPanel() {
        refreshProjSlots()
        projOverlay.visibility = View.VISIBLE
    }

    private fun closeProjPanel() {
        projOverlay.visibility = View.GONE
    }

    private fun saveProject(slot: Int) {
        val dir = projDir()
        dir.mkdirs()
        try {
            val json = JSONObject()
            json.put("version", 1)
            json.put("bpm", bpmLcd.bpm)
            json.put("padCount", padGrid.padCount)
            json.put("loopLenSteps", loopLenSteps)
            json.put("filter", activeFilter)
            json.put("scale", activeScale)
            json.put("root", activeRoot)
            json.put("assignX", assignXIdx)
            json.put("assignY", assignYIdx)
            json.put("mix", mixFader.value)
            json.put("fly", flyFader.value)
            json.put("soloMask", soloGroups.fold(0) { m, on -> (m shl 1) or if (on) 1 else 0 })
            val presets = JSONArray()
            for (p in userPresets) {
                if (p == null) {
                    presets.put(JSONObject.NULL)
                } else {
                    presets.put(
                        JSONObject()
                            .put("filter", p.filter)
                            .put("scale", p.scale)
                            .put("root", p.root)
                            .put("arpOn", p.arpOn)
                            .put("arpMode", p.arpMode)
                            .put("arpRate", p.arpRate)
                            .put("arpOct", p.arpOct)
                            .put("arpGate", p.arpGate)
                            .put("arpOctJump", p.arpOctJump)
                            .put("arpSlices", p.arpSlices)
                            .put("assignX", p.assignX)
                            .put("assignY", p.assignY)
                    )
                }
            }
            json.put("presets", presets)
            val banks = JSONArray()
            for (b in 0 until 8) {
                val frames = engine.nativeBankFrames(b)
                banks.put(frames)
                bankFile(slot, b).delete()
                if (frames > 0) {
                    val data = FloatArray(frames * 2)
                    if (engine.nativeCopyBank(b, data) > 0) {
                        val bb = ByteBuffer.allocate(frames * 8).order(ByteOrder.LITTLE_ENDIAN)
                        for (f in data) bb.putFloat(f)
                        bankFile(slot, b).writeBytes(bb.array())
                    }
                }
            }
            json.put("banks", banks)
            val stepsArr = JSONArray()
            for (b in 0 until 8) {
                stepsArr.put(
                    JSONObject()
                        .put("count", bankSteps[b])
                        .put("pitch", JSONArray(stepPitch[b].toList()))
                        .put("vel", JSONArray(stepVel[b].toList()))
                )
            }
            json.put("steps", stepsArr)
            val snaps = JSONArray()
            for (s in snapPads) {
                if (s == null) {
                    snaps.put(JSONObject.NULL)
                } else {
                    snaps.put(
                        JSONObject()
                            .put("ax", s.ax.toDouble())
                            .put("ay", s.ay.toDouble())
                            .put("bx", s.bx.toDouble())
                            .put("by", s.by.toDouble())
                            .put("mix", s.mix.toDouble())
                            .put("fly", s.fly.toDouble())
                            .put("filter", s.filter)
                            .put("assignX", s.assignX)
                            .put("assignY", s.assignY)
                    )
                }
            }
            json.put("snaps", snaps)
            val lfos = JSONArray()
            for (i in 0 until 2) {
                lfos.put(
                    JSONObject()
                        .put("on", lfoOn[i])
                        .put("rate", lfoRateIdxArr[i])
                        .put("depth", lfoDepthIdxArr[i])
                        .put("amount", lfoAmountIdxArr[i])
                        .put("target", lfoTargets[i])
                        .put("wave", lfoWaves[i])
                        .put("shape", JSONArray(lfoShapes[i].toList()))
                )
            }
            json.put("lfos", lfos)
            json.put("activeZu", activeZu)
            json.put("activeLu", activeLu)
            json.put("recFlavor", recFlavor)
            projFile(slot).writeText(json.toString())
            refreshProjSlots()
        } catch (e: Exception) {
        }
    }

    private fun loadProject(slot: Int) {
        val f = projFile(slot)
        if (!f.exists()) return
        try {
            val json = JSONObject(f.readText())
            bpmLcd.bpm = json.getDouble("bpm").toFloat()
            engine.nativeSetBpm(bpmLcd.bpm)
            applyPadCount(json.optInt("padCount", 12))
            loopLenSteps = json.optInt("loopLenSteps", 8)
            padGrid.activeBank = loopLenSteps
            activeFilter = json.optInt("filter", 0)
            engine.nativeSetFilterType(activeFilter)
            activeScale = json.optInt("scale", 0)
            activeRoot = json.optInt("root", 9)
            engine.nativeSetScale(activeScale)
            engine.nativeSetRoot(activeRoot)
            recFlavor = json.optInt("recFlavor", 0)
            engine.nativeSetRecFlavor(recFlavor)
            assignXIdx = json.optInt("assignX", 0)
            assignYIdx = json.optInt("assignY", 1)
            engine.nativeSetAssign(0, assignXIdx)
            engine.nativeSetAssign(1, assignYIdx)
            refreshAssignLabels()
            mixFader.value = json.getDouble("mix").toFloat()
            engine.nativeSetMix(mixFader.value)
            flyFader.setValue(json.getDouble("fly").toFloat())
            engine.nativeSetFly(flyFader.value)
            val sm = json.optInt("soloMask", 0)
            for (g in 0 until 4) soloGroups[g] = (sm and (1 shl g)) != 0
            engine.nativeSetSoloMask(sm)
            padGrid.soloMask = sm
            val presets = json.getJSONArray("presets")
            for (i in 0 until min(9, presets.length())) {
                if (presets.isNull(i)) {
                    userPresets[i] = null
                    pad.userPresets[i] = false
                } else {
                    val o = presets.getJSONObject(i)
                    userPresets[i] = UserPreset(
                        filter = o.getInt("filter"),
                        scale = o.getInt("scale"),
                        root = o.getInt("root"),
                        arpOn = o.getBoolean("arpOn"),
                        arpMode = o.getInt("arpMode"),
                        arpRate = o.getInt("arpRate"),
                        arpOct = o.getInt("arpOct"),
                        arpGate = o.getInt("arpGate"),
                        arpOctJump = o.getBoolean("arpOctJump"),
                        arpSlices = o.optInt("arpSlices", 2),
                        assignX = o.optInt("assignX", 0),
                        assignY = o.optInt("assignY", 1)
                    )
                    pad.userPresets[i] = true
                }
            }
            pad.invalidate()
            pushArp()
            refreshScaleHighlight()
            val banks = json.getJSONArray("banks")
            for (b in 0 until min(8, banks.length())) {
                val frames = banks.getInt(b)
                if (frames <= 0) continue
                val bf = bankFile(slot, b)
                if (!bf.exists() || bf.length() < frames * 8L) continue
                val bytes = bf.readBytes()
                val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
                val data = FloatArray(frames * 2)
                for (i in data.indices) data[i] = bb.float
                engine.nativeLoadBank(b, data, frames)
            }
            val stepsArr = json.optJSONArray("steps")
            if (stepsArr != null) {
                for (b in 0 until min(8, stepsArr.length())) {
                    val o = stepsArr.getJSONObject(b)
                    bankSteps[b] = o.optInt("count", 0)
                    val pa = o.optJSONArray("pitch")
                    val va = o.optJSONArray("vel")
                    for (s in 0 until 14) {
                        if (pa != null && s < pa.length()) stepPitch[b][s] = pa.getDouble(s).toFloat()
                        if (va != null && s < va.length()) stepVel[b][s] = va.getDouble(s).toFloat()
                    }
                    if (bankSteps[b] > 0) {
                        engine.nativeSetStepCount(b, bankSteps[b])
                        for (s in 0 until 14) {
                            engine.nativeSetStep(b, s, stepPitch[b][s], stepVel[b][s])
                        }
                    }
                }
            }
            val snaps = json.optJSONArray("snaps")
            if (snaps != null) {
                for (i in 0 until min(14, snaps.length())) {
                    if (snaps.isNull(i)) {
                        snapPads[i] = null
                    } else {
                        val o = snaps.getJSONObject(i)
                        snapPads[i] = Snap(
                            ax = o.getDouble("ax").toFloat(),
                            ay = o.getDouble("ay").toFloat(),
                            bx = o.getDouble("bx").toFloat(),
                            by = o.getDouble("by").toFloat(),
                            mix = o.getDouble("mix").toFloat(),
                            fly = o.getDouble("fly").toFloat(),
                            filter = o.getInt("filter"),
                            assignX = o.optInt("assignX", 0),
                            assignY = o.optInt("assignY", 1)
                        )
                    }
                }
            }
            val lfos = json.optJSONArray("lfos")
            if (lfos != null) {
                for (i in 0 until min(2, lfos.length())) {
                    val o = lfos.getJSONObject(i)
                    lfoOn[i] = o.optBoolean("on", false)
                    lfoRateIdxArr[i] = o.optInt("rate", 2)
                    lfoDepthIdxArr[i] = o.optInt("depth", 2)
                    lfoAmountIdxArr[i] = o.optInt("amount", 3)
                    lfoTargets[i] = o.optInt("target", -1)
                    lfoWaves[i] = o.optInt("wave", 0)
                    val sh = o.optJSONArray("shape")
                    if (sh != null && sh.length() == 64) {
                        for (j in 0 until 64) lfoShapes[i][j] = sh.getDouble(j).toFloat()
                    }
                }
                pushLfo(0)
                pushLfo(1)
                lfoDraw?.setShape(lfoShapes[lfoSel])
            }
            activeZu = json.optInt("activeZu", -1)
            activeLu = json.optInt("activeLu", -1)
            refreshSnapGrid()
            applyMorph(zuLuFader.value)
            closeProjPanel()
        } catch (e: Exception) {
        }
    }

    private fun applyPadCount(n: Int) {
        val d = resources.displayMetrics.density
        padGrid.padCount = n
        gridParams.height =
            ((when (n) {
                14 -> 112
                12, 10 -> 96
                else -> 80
            }) * d).toInt()
        padGrid.requestLayout()
    }

    private fun buildArpPanel(d: Float) {
        arpOverlay = FrameLayout(this)
        arpOverlay.setBackgroundColor(Color.argb(225, 8, 9, 12))
        arpOverlay.visibility = View.GONE

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
        title.text = "ARPEGGIATEUR"
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
        close.setOnClickListener { closeArpPanel() }
        titleRow.addView(close)
        panel.addView(titleRow)

        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        val listScroll = ScrollView(this)
        listScroll.addView(list)

        fun header(t: String) {
            val h = TextView(this)
            h.text = t
            h.setTextColor(amber)
            h.textSize = 11f
            h.typeface = markerFont(this)
            h.setPadding(0, (10 * d).toInt(), 0, (4 * d).toInt())
            list.addView(h)
        }

        fun row(labels: List<String>, kind: String, onClick: (Int) -> Unit) {
            val r = LinearLayout(this)
            for ((i, l) in labels.withIndex()) {
                val b = makeButton(l, silk)
                b.tag = kind to i
                b.setOnClickListener { onClick(i) }
                arpButtons.add(b)
                r.addView(b, wrapParams((4 * d).toInt()))
            }
            list.addView(r)
        }

        row(listOf("ON"), "on") { _ ->
            arpOn = !arpOn
            pad.arpOn = arpOn
            pad.invalidate()
            pushArp()
        }
        header("MODE")
        row(listOf("UP", "DOWN", "UP-DN", "RND", "XY"), "mode") { i ->
            arpMode = i
            pushArp()
        }
        header("VITESSE")
        row(listOf("1/4", "1/8", "1/16", "1/32"), "rate") { i ->
            arpRateIdx = i
            pushArp()
        }
        header("OCTAVES")
        row(listOf("x1", "x2", "x3", "x4"), "oct") { i ->
            arpOctIdx = i
            pushArp()
        }
        header("GATE")
        row(listOf("25%", "50%", "75%", "100%"), "gate") { i ->
            arpGateIdx = i
            pushArp()
        }
        header("SLICES (BOUCLES)")
        row(listOf("2", "4", "8", "12", "16"), "slices") { i ->
            arpSlicesIdx = i
            pushArp()
        }
        header("BONUS")
        row(listOf("OCT JUMP"), "jump") { _ ->
            arpOctJump = !arpOctJump
            pushArp()
        }
        header("SYNTH\u00c9 \u00b7 TONE")
        row(listOf("SUIT", "SAW", "SQR", "SIN", "TRI"), "swave") { i ->
            synWaveIdx = i
            pushSynth()
        }
        header("SYNTH\u00c9 \u00b7 ATTAQUE (MS)")
        row(listOf("0", "25", "50", "100", "250"), "satk") { i ->
            synAtkIdx = i
            pushSynth()
        }
        header("SYNTH\u00c9 \u00b7 D\u00c9CLIN")
        row(listOf("x0.5", "x1", "x2", "x4"), "sdec") { i ->
            synDecIdx = i
            pushSynth()
        }
        header("SYNTH\u00c9 \u00b7 OCTAVE")
        row(listOf("-2", "-1", "0", "+1", "+2"), "soct") { i ->
            synOctIdx = i
            pushSynth()
        }
        header("SYNTH\u00c9 \u00b7 PITCH (DEMIS-TONS)")
        row(listOf("-12", "-5", "0", "+5", "+12"), "spit") { i ->
            synPitIdx = i
            pushSynth()
        }
        header("SYNTH\u00c9 \u00b7 TUNE (CENTS)")
        row(listOf("-50", "0", "+50"), "stun") { i ->
            synTunIdx = i
            pushSynth()
        }

        header("LFO CUSTOM")
        val lfoTabs = LinearLayout(this)
        for ((i, l) in listOf("LFO A", "LFO B").withIndex()) {
            val b = makeButton(l, silk)
            b.tag = "ltab" to i
            b.setOnClickListener {
                lfoSel = i
                lfoDraw?.setShape(lfoShapes[i])
                refreshArpHighlight()
            }
            arpButtons.add(b)
            lfoTabs.addView(b, wrapParams((4 * d).toInt()))
        }
        list.addView(lfoTabs)

        header("FORME")
        row(listOf("SIN", "TRI", "SAW", "SQR", "DESSIN"), "lfw") { i ->
            lfoWaves[lfoSel] = i
            pushLfo(lfoSel)
        }

        val draw = LfoDrawView(this)
        draw.setShape(lfoShapes[lfoSel])
        draw.onShapeChange = { s ->
            lfoShapes[lfoSel] = s
            pushLfo(lfoSel)
        }
        lfoDraw = draw
        list.addView(draw, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (140 * d).toInt()))

        header("VITESSE (HZ)")
        row(listOf("0.25", "0.5", "1", "2", "4", "8"), "lfov") { i ->
            lfoRateIdxArr[lfoSel] = i
            pushLfo(lfoSel)
        }
        header("PROF")
        row(listOf("25%", "50%", "75%", "100%"), "lfod") { i ->
            lfoDepthIdxArr[lfoSel] = i
            pushLfo(lfoSel)
        }
        header("MONTANT")
        row(listOf("25%", "50%", "75%", "100%"), "lfoa") { i ->
            lfoAmountIdxArr[lfoSel] = i
            pushLfo(lfoSel)
        }
        header("CIBLE")
        fun lfoTargetRow(labels: List<String>, targets: List<Int>) {
            val r = LinearLayout(this)
            for (t in labels.withIndex()) {
                val b = makeButton(t.value, silk)
                b.tag = "lfot" to targets[t.index]
                b.setOnClickListener {
                    lfoTargets[lfoSel] = targets[t.index]
                    pushLfo(lfoSel)
                }
                arpButtons.add(b)
                r.addView(b, wrapParams((4 * d).toInt()))
            }
            list.addView(r)
        }
        lfoTargetRow(listOf("RIEN", "CUT", "RES", "REV"), listOf(-1, 0, 1, 2))
        lfoTargetRow(listOf("BIT", "VIT", "L-V", "L-P", "DRI"), listOf(3, 4, 5, 6, 7))

        val lfoRow = LinearLayout(this)
        val bLfoOn = makeButton("ACTIF", silk)
        bLfoOn.tag = "lfoon" to 0
        bLfoOn.setOnClickListener {
            lfoOn[lfoSel] = !lfoOn[lfoSel]
            pushLfo(lfoSel)
        }
        val bFlat = makeButton("PLAT", silk)
        bFlat.setOnClickListener {
            lfoShapes[lfoSel] = FloatArray(64) { 0.5f }
            lfoDraw?.setShape(lfoShapes[lfoSel])
            pushLfo(lfoSel)
        }
        lfoRow.addView(bLfoOn, wrapParams((4 * d).toInt()))
        lfoRow.addView(bFlat, wrapParams((4 * d).toInt()))
        list.addView(lfoRow)

        panel.addView(listScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val panelParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER
        )
        panelParams.setMargins((10 * d).toInt(), (16 * d).toInt(), (10 * d).toInt(), (16 * d).toInt())
        arpOverlay.addView(panel, panelParams)
    }

    private fun pushLfo(idx: Int) {
        engine.nativeSetCustomLfo(
            idx, lfoShapes[idx], lfoOn[idx], lfoRateIdxArr[idx],
            floatArrayOf(0.25f, 0.5f, 0.75f, 1f)[lfoDepthIdxArr[idx]],
            floatArrayOf(0.25f, 0.5f, 0.75f, 1f)[lfoAmountIdxArr[idx]],
            lfoTargets[idx], lfoWaves[idx]
        )
        refreshArpHighlight()
    }

    private fun pushArp() {
        engine.nativeSetArp(arpOn, arpMode, arpRateIdx, arpOctIdx + 1, arpGateIdx, arpOctJump)
        engine.nativeSetArpSlices(intArrayOf(2, 4, 8, 12, 16)[arpSlicesIdx])
        refreshArpHighlight()
    }

    private fun pushSynth() {
        engine.nativeSetSynth(
            synWaveIdx - 1,
            floatArrayOf(0f, 0.05f, 0.1f, 0.2f, 0.5f)[synAtkIdx],
            floatArrayOf(0.5f, 1f, 2f, 4f)[synDecIdx],
            synOctIdx - 2,
            floatArrayOf(-12f, -5f, 0f, 5f, 12f)[synPitIdx],
            floatArrayOf(-0.5f, 0f, 0.5f)[synTunIdx]
        )
        refreshArpHighlight()
    }

    private fun refreshArpHighlight() {
        for (b in arpButtons) {
            val tag = b.tag as? Pair<*, *> ?: continue
            val kind = tag.first as? String ?: continue
            val idx = (tag.second as? Int) ?: continue
            val lit = when (kind) {
                "on" -> arpOn
                "mode" -> arpOn && idx == arpMode
                "rate" -> idx == arpRateIdx
                "oct" -> idx == arpOctIdx
                "gate" -> idx == arpGateIdx
                "slices" -> idx == arpSlicesIdx
                "jump" -> arpOctJump
                "swave" -> idx == synWaveIdx
                "satk" -> idx == synAtkIdx
                "sdec" -> idx == synDecIdx
                "soct" -> idx == synOctIdx
                "spit" -> idx == synPitIdx
                "stun" -> idx == synTunIdx
                "ltab" -> idx == lfoSel
                "lfw" -> idx == lfoWaves[lfoSel]
                "lfov" -> idx == lfoRateIdxArr[lfoSel]
                "lfod" -> idx == lfoDepthIdxArr[lfoSel]
                "lfoa" -> idx == lfoAmountIdxArr[lfoSel]
                "lfot" -> idx == lfoTargets[lfoSel]
                "lfoon" -> lfoOn[lfoSel]
                "lfooff" -> !lfoOn[lfoSel]
                else -> false
            }
            b.setTextColor(if (lit) amber else silk)
        }
    }

    private fun openArpPanel() {
        refreshArpHighlight()
        arpOverlay.visibility = View.VISIBLE
    }

    private fun closeArpPanel() {
        arpOverlay.visibility = View.GONE
    }

    private fun saveUserPreset(slot: Int) {
        if (slot < 0 || slot > 8) return
        userPresets[slot] = UserPreset(
            filter = activeFilter,
            scale = activeScale,
            root = activeRoot,
            arpOn = arpOn,
            arpMode = arpMode,
            arpRate = arpRateIdx,
            arpOct = arpOctIdx,
            arpGate = arpGateIdx,
            arpOctJump = arpOctJump,
            arpSlices = arpSlicesIdx,
            assignX = assignXIdx,
            assignY = assignYIdx
        )
        pad.userPresets[slot] = true
        pad.invalidate()
    }

    private fun applyUserPreset(p: UserPreset) {
        activeFilter = p.filter
        engine.nativeSetFilterType(p.filter)
        activeScale = p.scale
        activeRoot = p.root
        engine.nativeSetScale(p.scale)
        engine.nativeSetRoot(p.root)
        arpOn = p.arpOn
        arpMode = p.arpMode
        arpRateIdx = p.arpRate
        arpOctIdx = p.arpOct
        arpGateIdx = p.arpGate
        arpOctJump = p.arpOctJump
        arpSlicesIdx = p.arpSlices
        assignXIdx = p.assignX
        assignYIdx = p.assignY
        engine.nativeSetAssign(0, p.assignX)
        engine.nativeSetAssign(1, p.assignY)
        pad.arpOn = arpOn
        pushArp()
        refreshScaleHighlight()
        refreshAssignLabels()
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
        padGrid.blinkOn = blinkOn
        btnRec.setTextColor(if (recHeld && blinkOn) Color.WHITE else red)
        if (shiftLocked) btnShift.alpha = if (blinkOn) 1f else 0.55f
    }

    private fun startLoopRecording(bank: Int) {
        val wet = loopWetA || loopWetB
        engine.nativeRecordBank(bank - 1, loopLenSteps, wet)
        resetSteps(bank - 1)
        padGrid.recPad = bank - 1
        val ms = (loopLenSteps * 60000f / bpmLcd.bpm.coerceAtLeast(40f)).toLong()
        recEndHandler.removeCallbacksAndMessages(null)
        recEndHandler.postDelayed({ padGrid.recPad = -1 }, ms + 200)
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
        recEndHandler.removeCallbacksAndMessages(null)
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
        zuLuFader.onValueChange = { v -> applyMorph(v) }
    }

    private fun resetSteps(b: Int) {
        if (b < 0 || b > 7) return
        bankSteps[b] = loopLenSteps
        stepPitch[b] = FloatArray(14)
        stepVel[b] = FloatArray(14) { 1f }
        engine.nativeSetStepCount(b, loopLenSteps)
    }

    private fun liveSnap() = Snap(
        ax = pad.ax, ay = pad.ay, bx = pad.bx, by = pad.by,
        mix = mixFader.value, fly = flyFader.value,
        filter = activeFilter, assignX = assignXIdx, assignY = assignYIdx
    )

    private fun captureSnap(side: Int) {
        if (side == 0 && activeZu < 0 && autoZu == null) autoLu = liveSnap()
        if (side == 1 && activeLu < 0 && autoLu == null) autoZu = liveSnap()
        val s = liveSnap()
        val n = min(padGrid.padCount, 14)
        val zone = if (side == 0) 0 until n / 2 else n / 2 until n
        val free = zone.firstOrNull { snapPads[it] == null }
        val idx = free
            ?: (if (side == 0) activeZu.takeIf { it >= 0 } ?: zone.last()
                else activeLu.takeIf { it >= 0 } ?: zone.first())
        snapPads[idx] = s
        if (side == 0) activeZu = idx else activeLu = idx
        refreshSnapGrid()
        applyMorph(zuLuFader.value)
    }

    private fun recallSnap(idx: Int) {
        val isZu = idx < min(padGrid.padCount, 14) / 2
        if (isZu) activeZu = idx else activeLu = idx
        refreshSnapGrid()
        applyMorph(zuLuFader.value)
    }

    private fun refreshSnapGrid() {
        val n = min(padGrid.padCount, 14)
        val sides = IntArray(14)
        for (i in 0 until n) {
            if (snapPads[i] != null) sides[i] = if (i < n / 2) 1 else 2
        }
        padGrid.snapSides = sides
        padGrid.activeSnap = intArrayOf(activeZu, activeLu)
        pad.ghostZu = activeZu.takeIf { it >= 0 }?.let { snapPads[it] }
            ?.let { floatArrayOf(it.ax, it.ay) }
        pad.ghostLu = activeLu.takeIf { it >= 0 }?.let { snapPads[it] }
            ?.let { floatArrayOf(it.bx, it.by) }
        pad.invalidate()
    }

    private fun applyMorph(tIn: Float) {
        val tRaw = tIn.coerceIn(0f, 1f)
        val t = when (morphCurve) {
            1 -> sqrt(tRaw)
            2 -> tRaw * tRaw
            3 -> tRaw * tRaw * (3f - 2f * tRaw)
            else -> tRaw
        }
        val zu = activeZu.takeIf { it >= 0 }?.let { snapPads[it] } ?: autoZu ?: return
        val lu = activeLu.takeIf { it >= 0 }?.let { snapPads[it] } ?: autoLu ?: return
        fun l(a: Float, b: Float) = a + (b - a) * t
        val ax = l(zu.ax, lu.ax)
        val ay = l(zu.ay, lu.ay)
        val bx = l(zu.bx, lu.bx)
        val by = l(zu.by, lu.by)
        val mx = l(zu.mix, lu.mix)
        val fy = l(zu.fly, lu.fly)
        engine.nativeSetAx(ax)
        engine.nativeSetAy(ay)
        engine.nativeSetBx(bx)
        engine.nativeSetBy(by)
        engine.nativeSetMix(mx)
        engine.nativeSetFly(fy)
        engine.nativeSetStutter(fy >= 0.75f)
        pad.setCursor(ax, ay, bx, by)
        mixFader.value = mx
        flyFader.setValue(fy)
        val src = if (t >= 0.5f) lu else zu
        activeFilter = src.filter
        engine.nativeSetFilterType(activeFilter)
        assignXIdx = src.assignX
        assignYIdx = src.assignY
        engine.nativeSetAssign(0, assignXIdx)
        engine.nativeSetAssign(1, assignYIdx)
        refreshAssignLabels()
        refreshFxHighlight()
    }

    private fun buildMenuPanel(d: Float) {
        menuOverlay = FrameLayout(this)
        menuOverlay.setBackgroundColor(Color.argb(190, 8, 9, 12))
        menuOverlay.visibility = View.GONE
        menuOverlay.setOnClickListener { menuOverlay.visibility = View.GONE }

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
        title.text = "MENU"
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
        close.setOnClickListener { menuOverlay.visibility = View.GONE }
        titleRow.addView(close)
        panel.addView(titleRow)

        fun header(t: String) {
            val h = TextView(this)
            h.text = t
            h.setTextColor(amber)
            h.textSize = 11f
            h.typeface = markerFont(this)
            h.setPadding(0, (10 * d).toInt(), 0, (4 * d).toInt())
            panel.addView(h)
        }

        header("PROJETS \u00b7 TAP = CHARGER \u00b7 LONG = SAUVER")
        for (r in 0 until 2) {
            val projRow = LinearLayout(this)
            for (c in 0 until 4) {
                val s = r * 4 + c + 1
                val b = makeButton("$s", silk)
                b.setOnClickListener {
                    loadProject(s)
                    refreshProjSlots()
                    refreshMenu()
                }
                b.setOnLongClickListener {
                    saveProject(s)
                    refreshMenu()
                    true
                }
                menuProjButtons.add(b)
                projRow.addView(
                    b,
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { marginStart = (4 * d).toInt() }
                )
            }
            panel.addView(projRow)
        }

        val bImport = makeButton("IMPORTER \u00b7 WAV / KOALA", silk)
        bImport.setOnClickListener { pickSampleFile(-1) }
        panel.addView(bImport, wrapParams((4 * d).toInt()))

        header("LIVE")
        val liveRow = LinearLayout(this)
        val bScale = makeButton("GAMME", silk)
        bScale.setOnClickListener { menuOverlay.visibility = View.GONE; openScalePanel() }
        val bMirror = makeButton("MIROIR", silk)
        bMirror.setOnClickListener {
            mirrored = !mirrored
            content.layoutDirection =
                if (mirrored) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            refreshMenu()
        }
        menuMirrorBtn = bMirror
        liveRow.addView(bScale, wrapParams((4 * d).toInt()))
        liveRow.addView(bMirror, wrapParams((4 * d).toInt()))
        panel.addView(liveRow)

        header("PADS")
        val padsRow = LinearLayout(this)
        for (n in listOf(8, 10, 12, 14)) {
            val b = makeButton("$n", silk)
            b.setOnClickListener { applyPadCount(n); refreshMenu() }
            menuPadButtons.add(b)
            padsRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(padsRow)

        header("REC \u00b7 SAVEUR LO-FI")
        val lofiRow = LinearLayout(this)
        for ((i, lbl) in listOf("OFF", "SP", "K7", "M\u00c9TAL").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                recFlavor = i
                engine.nativeSetRecFlavor(recFlavor)
                refreshMenu()
            }
            menuLofiButtons.add(b)
            lofiRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(lofiRow)

        header("SC\u00c8NES")
        val scRow = LinearLayout(this)
        val bSnap = makeButton("SNAPSHOT", amber)
        val bMotion = makeButton("MOTION (BIENT\u00d4T)", Color.rgb(90, 96, 108))
        scRow.addView(bSnap, wrapParams((4 * d).toInt()))
        scRow.addView(bMotion, wrapParams((4 * d).toInt()))
        panel.addView(scRow)

        header("BILLARD")
        val billardRow = LinearLayout(this)
        for ((i, lbl) in listOf("\u221e", "0.5", "1", "2", "4").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener { pad.billardFriction = i; refreshMenu() }
            menuBillardButtons.add(b)
            billardRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(billardRow)

        header("FINITIONS")
        val curveRow = LinearLayout(this)
        for ((i, lbl) in listOf("LIN", "D\u00c9BUT", "FIN", "S").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener { morphCurve = i; refreshMenu() }
            menuCurveButtons.add(b)
            curveRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(curveRow)

        val fingerRow = LinearLayout(this)
        for ((i, lbl) in listOf("ORDRE", "INVERS\u00c9", "ZONES").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                fingerModeOpt = i
                pad.fingerMode = fingerModeOpt
                refreshMenu()
            }
            menuFingerButtons.add(b)
            fingerRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(fingerRow)

        val bendRow = LinearLayout(this)
        val bFreeze = makeButton("BEND : JEU FIG\u00c9", silk)
        bFreeze.setOnClickListener {
            bendFreezeOpt = !bendFreezeOpt
            pad.bendFreeze = bendFreezeOpt
            refreshMenu()
        }
        menuFreezeBtn = bFreeze
        bendRow.addView(bFreeze, wrapParams((4 * d).toInt()))
        panel.addView(bendRow)

        val scroll = android.widget.ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        scroll.addView(panel)
        menuOverlay.addView(
            scroll,
            FrameLayout.LayoutParams(
                (340 * d).toInt(),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
    }

    private fun refreshMenu() {
        val counts = listOf(8, 10, 12, 14)
        for ((i, b) in menuPadButtons.withIndex()) {
            b.setTextColor(if (counts[i] == padGrid.padCount) amber else silk)
        }
        menuMirrorBtn?.setTextColor(if (mirrored) amber else silk)
        for ((i, b) in menuCurveButtons.withIndex()) {
            b.setTextColor(if (i == morphCurve) amber else silk)
        }
        for ((i, b) in menuFingerButtons.withIndex()) {
            b.setTextColor(if (i == fingerModeOpt) amber else silk)
        }
        menuFreezeBtn?.setTextColor(if (bendFreezeOpt) amber else silk)
        for ((i, b) in menuBillardButtons.withIndex()) {
            b.setTextColor(if (i == pad.billardFriction) amber else silk)
        }
        for ((i, b) in menuLofiButtons.withIndex()) {
            b.setTextColor(if (i == recFlavor) amber else silk)
        }
        for (s in 1..8) {
            menuProjButtons[s - 1].setTextColor(if (projFile(s).exists()) amber else silk)
        }
    }

    private fun openMenuPanel() {
        refreshMenu()
        menuOverlay.visibility = View.VISIBLE
    }

    private fun buildStepPanel(d: Float) {
        stepOverlay = FrameLayout(this)
        stepOverlay.setBackgroundColor(Color.argb(225, 8, 9, 12))
        stepOverlay.visibility = View.GONE

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
        title.text = "PAS"
        title.setTextColor(Color.rgb(236, 224, 196))
        title.textSize = 16f
        title.typeface = markerFont(this)
        stepTitle = title
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
        close.setOnClickListener { stepOverlay.visibility = View.GONE }
        titleRow.addView(close)
        panel.addView(titleRow)

        val hint = TextView(this)
        hint.text = "HAUT = PITCH \u00b7 BAS = VELOCITE"
        hint.setTextColor(Color.rgb(120, 128, 146))
        hint.textSize = 10f
        panel.addView(hint)

        val bImp = makeButton("IMPORTER SAMPLE", silk)
        bImp.setOnClickListener {
            if (stepEditBank >= 0) pickSampleFile(stepEditBank)
        }
        panel.addView(
            bImp,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = (8 * d).toInt() }
        )

        val editor = StepEditorView(this)
        editor.onStepChange = { s, p, v ->
            if (stepEditBank >= 0) engine.nativeSetStep(stepEditBank, s, p, v)
        }
        stepEditor = editor
        panel.addView(
            editor,
            LinearLayout.LayoutParams((320 * d).toInt(), (230 * d).toInt()).apply {
                topMargin = (8 * d).toInt()
            }
        )

        stepOverlay.addView(
            panel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
    }

    private fun openStepEditor(bank: Int) {
        val e = stepEditor ?: return
        stepEditBank = bank
        e.bank = bank
        e.stepCount = bankSteps[bank].coerceAtLeast(1)
        e.pitch = stepPitch[bank]
        e.vel = stepVel[bank]
        e.invalidate()
        stepTitle?.text = "PAS \u00b7 BANQUE ${bank + 1}"
        stepOverlay.visibility = View.VISIBLE
    }

    private fun pickSampleFile(targetBank: Int) {
        importTargetBank = targetBank
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "application/zip", "application/octet-stream"))
        }
        startActivityForResult(intent, reqImport)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != reqImport || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        Toast.makeText(this, "IMPORT...", Toast.LENGTH_SHORT).show()
        thread {
            val msg = importSamples(uri, importTargetBank)
            runOnUiThread {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                refreshMenu()
            }
        }
    }

    private fun importSamples(uri: Uri, targetBank: Int): String {
        return try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return "FICHIER ILLISIBLE"
            val name = queryName(uri).lowercase()
            val isZip = bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
            val wavs: List<ByteArray> = if (isZip || name.endsWith(".koala")) koalaWavs(bytes) else listOf(bytes)
            if (wavs.isEmpty()) return "AUCUN WAV TROUVE"
            var loaded = 0
            var bank = targetBank
            for (w in wavs) {
                val pcm = decodeWav(w) ?: continue
                if (pcm.size < 9600) continue
                if (bank !in 0..7) {
                    bank = (0..7).firstOrNull { !engine.nativeBankHasLoop(it) } ?: break
                }
                resetSteps(bank)
                engine.nativeLoadBank(bank, pcm, pcm.size / 2)
                loaded++
                bank = -1
            }
            if (loaded == 0) "ECHEC IMPORT (FORMAT ?)" else "IMPORTE $loaded BANQUE(S)"
        } catch (e: Exception) {
            "ERREUR IMPORT"
        }
    }

    private fun queryName(uri: Uri): String {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) c.getString(i) ?: "sample" else "sample"
            } ?: "sample"
        } catch (e: Exception) {
            "sample"
        }
    }

    private fun koalaWavs(bytes: ByteArray): List<ByteArray> {
        val out = ArrayList<ByteArray>()
        try {
            val z = ZipInputStream(ByteArrayInputStream(bytes))
            var e = z.nextEntry
            while (e != null && out.size < 8) {
                if (!e.isDirectory && e.name.lowercase().endsWith(".wav")) {
                    val buf = ByteArrayOutputStream()
                    z.copyTo(buf)
                    out.add(buf.toByteArray())
                }
                e = z.nextEntry
            }
            z.close()
        } catch (e: Exception) {
        }
        return out
    }

    private fun le16(b: ByteArray, p: Int): Int =
        (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8)

    private fun le16s(b: ByteArray, p: Int): Int {
        val v = le16(b, p)
        return if (v >= 0x8000) v - 0x10000 else v
    }

    private fun le32(b: ByteArray, p: Int): Int =
        (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8) or
            ((b[p + 2].toInt() and 0xFF) shl 16) or ((b[p + 3].toInt() and 0xFF) shl 24)

    private fun decodeWav(b: ByteArray): FloatArray? {
        if (b.size < 44) return null
        if (!(b[0] == 'R'.code.toByte() && b[1] == 'I'.code.toByte() &&
                b[2] == 'F'.code.toByte() && b[3] == 'F'.code.toByte())
        ) return null
        var fmt = -1
        var ch = 0
        var sr = 0
        var bits = 0
        var dataStart = -1
        var dataLen = 0
        var pos = 12
        while (pos + 8 <= b.size) {
            val id = String(b, pos, 4, Charsets.US_ASCII)
            val len = le32(b, pos + 4)
            if (len < 0) break
            when (id) {
                "fmt " -> {
                    if (pos + 24 <= b.size) {
                        fmt = le16(b, pos + 8)
                        ch = le16(b, pos + 10)
                        sr = le32(b, pos + 12)
                        bits = le16(b, pos + 22)
                    }
                }
                "data" -> {
                    dataStart = pos + 8
                    dataLen = min(len, b.size - dataStart)
                }
            }
            pos += 8 + len + (len and 1)
        }
        if (dataStart < 0 || ch < 1 || sr < 8000 || bits == 0) return null
        val bytesPer = bits / 8
        val frames = min(dataLen / (bytesPer * ch), 2880000)
        if (frames <= 0) return null
        val st = FloatArray(frames * 2)
        for (f in 0 until frames) {
            for (c in 0 until 2) {
                val src = if (c < ch) c else 0
                val p = dataStart + (f * ch + src) * bytesPer
                val v = when {
                    fmt == 3 && bits == 32 -> Float.fromBits(le32(b, p))
                    bits == 16 -> le16s(b, p) / 32768f
                    bits == 24 -> {
                        val x = (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8) or
                            ((b[p + 2].toInt() and 0xFF) shl 16)
                        val s = if (x and 0x800000 != 0) x or (0xFF shl 24) else x
                        s / 8388608f
                    }
                    bits == 32 -> le32(b, p) / 2147483648f
                    bits == 8 -> ((b[p].toInt() and 0xFF) - 128) / 128f
                    else -> return null
                }
                st[f * 2 + c] = v
            }
        }
        if (sr == 48000) return st
        val nOut = ((frames.toLong() * 48000L) / sr).toInt()
        val out = FloatArray(nOut * 2)
        for (i in 0 until nOut) {
            val t = i.toDouble() * sr / 48000.0
            val i0 = t.toInt().coerceIn(0, frames - 1)
            val i1 = min(i0 + 1, frames - 1)
            val fr = (t - i0).toFloat()
            out[i * 2] = st[i0 * 2] + (st[i1 * 2] - st[i0 * 2]) * fr
            out[i * 2 + 1] = st[i0 * 2 + 1] + (st[i1 * 2 + 1] - st[i0 * 2 + 1]) * fr
        }
        return out
    }
}
