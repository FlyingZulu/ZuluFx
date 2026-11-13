package com.zulu

import android.app.Activity
import android.content.pm.ActivityInfo
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
    private val inLevels = FloatArray(2)
    private var vuSlow = 0f
    private val vuHandler = Handler(Looper.getMainLooper())
    private val recEndHandler = Handler(Looper.getMainLooper())
    private val vuRunnable = object : Runnable {
        override fun run() {
            engine.nativeGetLevels(levels)
            engine.nativeGetInLevels(inLevels)
            val lL = Math.sqrt(levels[0].toDouble()).toFloat()
            val lR = Math.sqrt(levels[1].toDouble()).toFloat()
            vuMeter.levelL = lL
            vuMeter.levelR = lR
            vuMeter.inLevelL = Math.sqrt(inLevels[0].toDouble()).toFloat()
            vuMeter.inLevelR = Math.sqrt(inLevels[1].toDouble()).toFloat()
            val lvl = maxOf(lL, lR)
            vuSlow += 0.06f * (lvl - vuSlow)
            pad.setAudio(lvl, (lvl - vuSlow).coerceAtLeast(0f) * 3f)
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
    private lateinit var btnUndo: Button
    private lateinit var btnArp: Button
    private lateinit var btnPlay: Button
    private lateinit var fxOverlay: FrameLayout
    private lateinit var scaleOverlay: FrameLayout
    private lateinit var projOverlay: FrameLayout
    private lateinit var gridParams: LinearLayout.LayoutParams
    private val projSlotButtons = ArrayList<Button>()
    private lateinit var arpOverlay: FrameLayout
    private lateinit var padPane: FrameLayout

    private var fxOpen = false
    private var fxHeld = false
    private val fxHeldClear = Runnable { fxHeld = false }
    private var editFxMode = false
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
    private var btnPunch: Button? = null

    private data class Snap(
        val ax: Float,
        val ay: Float,
        val bx: Float,
        val by: Float,
        val mix: Float,
        val filter: Int,
        val assignX: Int,
        val assignY: Int,
        val punch: BooleanArray? = null,
        val punchCol2: Int = 0,
        val motA: FloatArray? = null,
        val motB: FloatArray? = null,
        val motLen: Int = 0,
        val axMulti: List<Int>? = null,
        val ayMulti: List<Int>? = null,
        val axCurve: IntArray? = null,
        val ayCurve: IntArray? = null,
        val masterFxList: List<Int>? = null
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
    private var menuLoopLbl: Button? = null
    private var menuMirrorBtn: Button? = null
    private val menuCurveButtons = ArrayList<Button>()
    private val menuFingerButtons = ArrayList<Button>()
    private val menuArtButtons = ArrayList<Button>()
    private val menuStyleButtons = ArrayList<Button>()
    private val menuEcoButtons = ArrayList<Button>()
    private var ecoOpt = false
    private var bgArtEcoPrev = 0
    private var lastDrumZone = -1
    private var padTouchDown = false
    private var bgArtOpt = 1
    private var menuFreezeBtn: Button? = null
    private val menuBillardButtons = ArrayList<Button>()
    private val menuLofiButtons = ArrayList<Button>()
    private val menuStretchButtons = ArrayList<Button>()
    private val bankStretch = IntArray(8)
    private var recFlavor = 0
    private var menuPunchBtn: Button? = null
    private var punchMode = false
    private var punchView: PunchPadView? = null
    private var punchCol = 0
    private var paramView: ParamPageView? = null
    private var paramFx = -1
    private val fxParamInit = BooleanArray(62)
    private val fxParamVals = Array(62) { FloatArray(8) }
    private val fxParamAssign = Array(62) { IntArray(8) }
    private val fxParamDefs = mapOf(
        27 to listOf("TEMPS", "FB", "TONAL", "SAT"),
        28 to listOf("GRAVE", "AIGU", "DRIVE", "HUM"),
        29 to listOf("WOW", "DROP", "SAT", "SOUFFLE"),
        30 to listOf("PLAGE", "VITESSE", "LISSAGE", "NIVEAU"),
        31 to listOf("VITESSE", "GRAVE", "AIGU", "RESO"),
        32 to listOf("ACCORD", "AMORTI", "EXCIT", "NIVEAU"),
        33 to listOf("POSITION", "PITCH", "MODE", "NIVEAU"),
        34 to listOf("GRAIN", "PITCH", "SPRAY", "NIVEAU"),
        35 to listOf("DENSITE", "FOLIE", "REVERSE", "NIVEAU"),
        36 to listOf("SLICES", "RANDOM", "PITCH", "NIVEAU"),
        37 to listOf("GATE", "PITCH", "ORDRE", "NIVEAU"),
        38 to listOf("DENSITE", "TAILLE", "NUAGE", "NIVEAU"),
        39 to listOf("POINT", "TAILLE", "VITESSE", "NIVEAU"),
        40 to listOf("GRAVE", "MED", "AIGU", "NIVEAU"),
        41 to listOf("SEUIL", "RATIO", "REPRISE", "NIVEAU"),
        42 to listOf("DRIVE", "TONAL", "NIVEAU"),
        43 to listOf("GRAVE", "AIGU", "GATE", "NIVEAU"),
        44 to listOf("FUZZ", "TONAL", "NIVEAU"),
        45 to listOf("GAIN", "TONAL", "NIVEAU"),
        0 to listOf("TEMPS", "FB", "TONAL", "NIVEAU"),
        1 to listOf("TEMPS", "FB", "TONAL", "NIVEAU"),
        2 to listOf("VITESSE", "GRAVE", "AIGU", "NIVEAU"),
        3 to listOf("VITESSE", "PROF", "FORME", "NIVEAU"),
        4 to listOf("VITESSE", "FORME", "PROF", "NIVEAU"),
        5 to listOf("VITESSE", "PROF", "FB", "NIVEAU"),
        6 to listOf("VITESSE", "PROF", "SPREAD", "NIVEAU"),
        7 to listOf("FREQUENCE", "TIMBRE", "PROF", "NIVEAU"),
        8 to listOf("WOW", "FLUTTER", "SOUFFLE", "NIVEAU"),
        9 to listOf("VITESSE", "FENETRE", "FIGURE", "NIVEAU"),
        10 to listOf("TAILLE", "POINT", "TONAL", "NIVEAU"),
        11 to listOf("SEUIL", "GAIN", "REPRISE", "NIVEAU"),
        12 to listOf("VITESSE", "RELANCE", "PROF", "NIVEAU"),
        13 to listOf("DUREE", "POINT", "TONAL", "NIVEAU"),
        14 to listOf("TAILLE", "POINT", "TONAL", "NIVEAU"),
        15 to listOf("ACCORD", "AMORTI", "RICHESSE", "NIVEAU"),
        16 to listOf("TENSION", "RICHESSE", "ROULIS", "NIVEAU"),
        17 to listOf("GAIN", "ASYM", "REPLI", "NIVEAU"),
        18 to listOf("VITESSE", "PLAGE", "SENS", "NIVEAU"),
        19 to listOf("TAILLE", "PITCH", "ESPACE", "NIVEAU"),
        20 to listOf("TAILLE", "FOLIE", "SAUT", "NIVEAU"),
        21 to listOf("DUREE", "GRAVE", "REVERB", "NIVEAU"),
        22 to listOf("DRIVE", "CHALEUR", "GRAIN", "NIVEAU"),
        23 to listOf("VITESSE", "DRIVE", "TROU", "NIVEAU"),
        24 to listOf("DUREE", "GRAVE", "REVERB", "NIVEAU"),
        25 to listOf("DUREE", "COURBE", "NUAGE", "NIVEAU"),
        26 to listOf("PUNCH", "PROF", "FORME", "NIVEAU"),
        46 to listOf("TUNE", "ATTAQUE", "RELEASE", "NIVEAU"),
        51 to listOf("TUNE", "DECAY", "CLICK", "NIVEAU"),
        52 to listOf("TON", "DECAY", "GRAINS", "NIVEAU"),
        53 to listOf("TON", "DECAY", "PITCH", "NIVEAU"),
        58 to listOf("TUNE", "ATTAQUE", "RELEASE", "NIVEAU"),
        59 to listOf("TUNE", "ATTAQUE", "RELEASE", "NIVEAU"),
        60 to listOf("TUNE", "ATTAQUE", "RELEASE", "NIVEAU"),
        61 to listOf("CUT", "GLIDE", "RESO", "NIVEAU"),
        62 to listOf("TON", "DECAY", "MORDANT", "NIVEAU")
    )
    private val fxParamDefaults = mapOf(
        27 to floatArrayOf(0.45f, 0.8f, 0.22f, 0.2f),
        28 to floatArrayOf(0.38f, 0.44f, 0.42f, 0.33f),
        29 to floatArrayOf(0.5f, 0.56f, 0.15f, 0.33f),
        30 to floatArrayOf(0.5f, 0.14f, 0.16f, 0.71f),
        31 to floatArrayOf(0f, 0.5f, 0.4f, 0.5f),
        32 to floatArrayOf(0.33f, 0.76f, 0.33f, 0.63f),
        33 to floatArrayOf(0f, 0.5f, 0f, 0.6f),
        34 to floatArrayOf(0.3f, 0.55f, 0.2f, 0.6f),
        35 to floatArrayOf(0.4f, 0.4f, 0.3f, 0.6f),
        36 to floatArrayOf(0.4f, 0.5f, 0.5f, 0.6f),
        37 to floatArrayOf(0.4f, 0.5f, 0f, 0.6f),
        38 to floatArrayOf(0.4f, 0.3f, 0.25f, 0.6f),
        39 to floatArrayOf(0.3f, 0.3f, 0.5f, 0.6f),
        40 to floatArrayOf(0.65f, 0.5f, 0.6f, 0.6f),
        41 to floatArrayOf(0.35f, 0.35f, 0.4f, 0.6f),
        42 to floatArrayOf(0.35f, 0.5f, 0.6f),
        43 to floatArrayOf(0.3f, 0.7f, 0.2f, 0.6f),
        44 to floatArrayOf(0.45f, 0.5f, 0.55f),
        45 to floatArrayOf(0.4f, 0.55f, 0.5f),
        0 to floatArrayOf(0.27f, 0.42f, 0.5f, 0.5f),
        1 to floatArrayOf(0.5f, 0.6f, 0.5f, 0.5f),
        2 to floatArrayOf(0.33f, 0.5f, 0.5f, 0.5f),
        3 to floatArrayOf(0.5f, 0.5f, 0f, 0.5f),
        4 to floatArrayOf(0.5f, 0f, 0.5f, 0.5f),
        5 to floatArrayOf(0.5f, 0.33f, 0.5f, 0.5f),
        6 to floatArrayOf(0.5f, 0.33f, 0.5f, 0.5f),
        7 to floatArrayOf(0.5f, 0f, 0.5f, 0.5f),
        8 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        9 to floatArrayOf(0.25f, 0.2f, 0f, 0.5f),
        10 to floatArrayOf(1f, 0f, 0.5f, 0.5f),
        11 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        12 to floatArrayOf(0.5f, 0.5f, 1f, 0.5f),
        13 to floatArrayOf(0.25f, 0f, 0.5f, 0.5f),
        14 to floatArrayOf(0.5f, 1f, 0.5f, 0.5f),
        15 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        16 to floatArrayOf(0.5f, 0.5f, 0f, 0.5f),
        17 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        18 to floatArrayOf(0.5f, 0.5f, 0f, 0.5f),
        19 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        20 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        21 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        22 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        23 to floatArrayOf(0.25f, 0.5f, 0.5f, 0.5f),
        24 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        25 to floatArrayOf(0.5f, 0.5f, 0f, 0.5f),
        26 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        46 to floatArrayOf(0.5f, 0f, 0.5f, 0.5f),
        51 to floatArrayOf(0.5f, 0.5f, 0.2f, 0.5f),
        52 to floatArrayOf(0.5f, 0.5f, 0.33f, 0.5f),
        53 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f),
        58 to floatArrayOf(0.5f, 0f, 0.5f, 0.5f),
        59 to floatArrayOf(0.5f, 0f, 0.5f, 0.5f),
        60 to floatArrayOf(0.5f, 0f, 0.5f, 0.5f),
        61 to floatArrayOf(0.5f, 0.35f, 0.5f, 0.5f),
        62 to floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)
    )
    private var sendPadBank = -1
    private var sendPadAt = 0L
    private var sendComboStart = true
    private var muteHeld = false
    private var muteComboAt = 0L
    private var globalSaveArmed = false
    private var globalSaveAt = 0L
    private val bankFxMask = Array(8) { BooleanArray(46) }
    private val bankOn = BooleanArray(8)
    private val rackBackup = BooleanArray(46)
    private var rackBackedUp = false
    private var heldFxSlot = -1
    private var heldBankPad = -1
    private val presetBtns = ArrayList<Button>()
    private var arpBtn: Button? = null
    private val bankFxSend = BooleanArray(8) { true }
    private var fHeld = false
    private var zHeld = false
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
        val assignY: Int,
        val fxOn: BooleanArray,
        val fxParams: Array<FloatArray>,
        val fxAssign: Array<IntArray>,
        val lfoTg: IntArray
    )
    private val fxButtons = ArrayList<Button>()
    private val fxButtonCols = ArrayList<Int>()

    private val fxCatCols = listOf(
        Color.rgb(0, 240, 255),
        Color.rgb(185, 110, 255),
        Color.rgb(70, 255, 130),
        Color.rgb(255, 190, 0),
        Color.rgb(255, 70, 205),
        Color.rgb(236, 224, 196),
        Color.rgb(235, 255, 40),
        Color.rgb(255, 120, 80),
        Color.rgb(0, 255, 170),
        Color.rgb(190, 190, 255)
    )

    private val fxSections = listOf(
        "FILTRE" to listOf("LPF", "HPF", "BPF", "NOTCH", "PHASER"),
        "MODULATION" to listOf("TREMOLO", "AUTOPAN", "FLANGER", "CHORUS", "RING", "TALK", "WARP"),
        "TEMPS" to listOf("DELAY", "ECHO", "DUB", "REVERB", "FREEZE", "STOP", "REPEAT", "REVERSE", "SCRATCH"),
        "TEXTURE" to listOf("CRUSH", "DRIVE", "LO-FI", "TAPE", "GATER", "FOLD", "MEGA", "FUZZ", "DISTO"),
        "RESONANCE" to listOf("RESO", "SPRING", "RINGS"),
        "GRANULAIRE" to listOf("GRAIN", "JUNGLE"),
        "TRANSITIONS" to listOf("TRANSIT", "COMBUSTOR", "TURNADO", "FALL", "SWELL", "PUMP"),
        "SYNTH" to listOf("SAW", "SQUARE", "FM", "NOISE", "303", "KICK", "SNARE", "HAT", "PERC"),
        "LECTURE SAMPLE" to listOf("SPLY", "GRAN", "JGL", "RND", "SLICR", "CLOUD", "FRZ"),
        "STUDIO" to listOf("EQ3", "COMP", "WARM", "CLEAN")
    )

    private val fxCodes = listOf(
        "F0", "F1", "F2", "F3", "X2",
        "X3", "X4", "X5", "X6", "X7", "X31", "X30",
        "X0", "X1", "X27", "P2", "X10", "X12", "X13", "X14", "X18",
        "P3", "P7", "X8", "X29", "X9", "X17", "X28", "X44", "X45",
        "X15", "X16", "X32",
        "X19", "X20",
        "X21", "X22", "X23", "X24", "X25", "X26",
        "G0", "G13", "G14", "G12", "G15", "G5", "G6", "G7", "G16",
        "X33", "X34", "X35", "X36", "X37", "X38", "X39",
        "X40", "X41", "X42", "X43"
    )
    private val fxActive = BooleanArray(46)
    private val paramActive = BooleanArray(8)
    private val paramDefaults = floatArrayOf(0f, 0f, 0.35f, 0.3f, 0f, 0f, 0f, 0.4f)
    private var genIdx = 0

    private val yellow = Color.rgb(235, 255, 40)
    private val pink = Color.rgb(255, 70, 205)
    private val green = Color.rgb(70, 255, 130)
    private val cream = Color.rgb(236, 224, 196)
    private val silk = Color.rgb(165, 172, 185)
    private val amber = Color.rgb(255, 190, 0)
    private val turquoise = Color.rgb(0, 240, 255)
    private val violet = Color.rgb(185, 110, 255)
    private val red = Color.rgb(255, 70, 55)

    private var playing = false
    private var simulLock = false
    private var sceneMode = 0
    private var bSnapBtn: Button? = null
    private var bMotionBtn: Button? = null
    private var verticalUi = false
    private val menuOrientButtons = ArrayList<Button>()
    private val menuSnapButtons = ArrayList<Button>()
    private val menuInButtons = ArrayList<Button>()
    private var inProfileSel = 0
    private val menuEqButtons = ArrayList<Button>()
    private var masterEqOn = false
    private val menuBoostInButtons = ArrayList<Button>()
    private val menuBoostOutButtons = ArrayList<Button>()
    private var inBoostSel = 0
    private var outBoostSel = 0
    private val menuLoudButtons = ArrayList<Button>()
    private var outLoudOn = false
    private val menuNetButtons = ArrayList<Button>()
    private var netModeSel = 0
    private val menuNetChaosButtons = ArrayList<Button>()
    private var netChaosSel = 1
    private val menuNetModButtons = ArrayList<Button>()
    private var netModSel = 1
    private val menuNetAliasButtons = ArrayList<Button>()
    private var netAliasSel = 0
    private val menuNetSpeedButtons = ArrayList<Button>()
    private var netSpeedSel = 1
    private val masterFx = HashSet<Int>()
    private val snapFxOverride = HashMap<Int, Boolean>()
    private val projNames = HashMap<Int, String>()
    private val projCols = HashMap<Int, Int>()
    private var projPage = 0
    private var projPageLabel: TextView? = null
    private var lastProjTapSlot = -1
    private var lastProjTapAt = 0L
    private val bankLabels = Array(8) { "" }
    private val bankRecAt = LongArray(8)
    private var showSampleLog = true
    private var sampleLogView: TextView? = null
    private val menuLogButtons = ArrayList<Button>()
    private val menuNameButtons = ArrayList<Button>()
    private val axisMulti = Array(2) { ArrayList<Int>() }
    private val axisCurve = Array(2) { IntArray(10) }
    private var assignArm = -1
    private var assignBlinkOn = false
    private val curveNames = listOf("NORM", "INV", "S&H", "BOUILLON")
    private var snapTransp = true
    private var snapHold = false
    private val snapHoldOld = HashMap<String, Float>()
    private val snapHoldFx = HashMap<Int, FloatArray>()
    private var fRecTap = false
    private var zRecTap = false
    private var sceneSide = 0
    private var toolPage = 0
    private var playerBank = 0
    private var recArmed = false
    private var recArmPad = -1
    private var recSnapSide = -1
    private var recActive = false
    private var recMotion = false
    private var motionSrcA = false
    private var motionSrcB = false
    private var motionPlaying = -1
    private var motionPhase = 0f
    private var playAx = 0.5f
    private var playAy = 0.5f
    private var playBx = 0.5f
    private var playBy = 0.5f
    private val motA = ArrayList<Float>()
    private val motB = ArrayList<Float>()
    private val motionHandler = Handler(Looper.getMainLooper())
    private val seqHandler = Handler(Looper.getMainLooper())
    private val midiLoops = Array(8) { ArrayList<FloatArray>() }
    private val midiPlaying = BooleanArray(8)
    private val midiStartMs = LongArray(8)
    private val midiLastPos = FloatArray(8) { -1f }
    private var recMidiEngines = true
    private var recStartMs = 0L
    private var recLenMs = 0L
    private var recResampleFx = false
    private var lenEntry = false
    private var lenFirst = 0
    private var masterVol = 1f
    private var inVol = 1f
    private val lenHandler = Handler(Looper.getMainLooper())
    private val vuOverlayHandler = Handler(Looper.getMainLooper())
    private val punchLatched = BooleanArray(9)
    private var lastEditBank = -1
    private var lastEditAt = 0L
    private lateinit var drumGrid: DrumGridView
    private lateinit var modeBadge: ModeBadgeView
    private lateinit var inKnob: LineInKnob
    private val menuDrumButtons = ArrayList<Button>()
    private val menuMidiButtons = ArrayList<Button>()
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
        verticalUi = getSharedPreferences("zulu", MODE_PRIVATE).getBoolean("verticalUi", false)
        snapTransp = getSharedPreferences("zulu", MODE_PRIVATE).getBoolean("snapTransp", true)
        showSampleLog = getSharedPreferences("zulu", MODE_PRIVATE).getBoolean("sampleLog", true)
        requestedOrientation =
            if (verticalUi) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
        mixFader = VFaderView(this, "MIX", thumbColor = Color.rgb(70, 255, 130))
        mixFader.value = 1f
        glitchScope = GlitchScopeView(this)
        glitchScope.visibility = View.GONE

        pad.waveform = placeholderWaveform()
        pad.onAxisChange = { ax, ay, bx, by ->
            if (!(snapHold && snapTransp)) {
                engine.nativeSetAx(ax)
                engine.nativeSetAy(ay)
                engine.nativeSetBx(bx)
                engine.nativeSetBy(by)
                if (arpOn && arpMode == 4) engine.nativeSetArpXy(ax, true)
            }
            if (padTouchDown && (genIdx == 5 || genIdx == 6 || genIdx == 7 || genIdx == 16)) {
                val col = (ax.coerceIn(0f, 0.999f) * 3f).toInt()
                val row = (ay.coerceIn(0f, 0.999f) * 3f).toInt()
                val zone = row * 3 + col
                if (zone != lastDrumZone) {
                    lastDrumZone = zone
                    engine.nativeGenHit(genIdx, ax, ay)
                }
            }
            padAxisX = ax
        }
        pad.onPadTouchState = { t ->
            padTouchDown = t
            engine.nativeSetGenTouch(t)
            if (!t) {
                engine.nativeSetArpXy(padAxisX, false)
                lastDrumZone = -1
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
        pad.shiftCheck = { shiftActive() }
        pad.onFrictionCycle = {
            pad.billardFriction = (pad.billardFriction + 1) % 5
            val lbl = listOf("\u221e", "0.5 s", "1 s", "2 s", "4 s")[pad.billardFriction]
            Toast.makeText(this, "INERTIE $lbl", Toast.LENGTH_SHORT).show()
            refreshMenu()
        }

        btnMute = makeRoundButton("MUTE", silk, 10f)
        btnShift = makeRoundButton("SHIFT", silk, 10f)
        btnFx = makeRoundButton("FX", amber, 12f)
        btnF = makeButton("F", yellow)
        btnZ = makeButton("Z", pink)
        btnRec = makeRoundButton("REC", red, 10f)
        btnZu = makeButton("ZU", turquoise)
        btnLu = makeButton("LU", violet)
        btnUndo = makeButton("UNDO", silk)
        btnUndo.setOnClickListener {
            engine.nativeUndo()
            Toast.makeText(this, "UNDO REC", Toast.LENGTH_SHORT).show()
        }
        btnArp = makeRoundButton("OUTILS", pink, 7f)
        btnArp.setOnClickListener { arpTap() }
        btnPlay = makeRoundButton("PLAY", green, 10f)
        btnPlay.setOnClickListener { togglePlay() }

        btnZu.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    btnZu.text = "ZU\u00b7CAP"
                    if (recArmed) {
                        recSnapSide = 0
                        recArmPad = -1
                        padGrid.recPad = -1
                        Toast.makeText(this, "SNAPSHOT ZU : CHOISIR PAD", Toast.LENGTH_SHORT).show()
                    } else {
                        sceneSide = 0
                        refreshSceneSide()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> btnZu.text = "ZU"
            }
            true
        }
        btnLu.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    btnLu.text = "LU\u00b7CAP"
                    if (recArmed) {
                        recSnapSide = 1
                        recArmPad = -1
                        padGrid.recPad = -1
                        Toast.makeText(this, "SNAPSHOT LU : CHOISIR PAD", Toast.LENGTH_SHORT).show()
                    } else {
                        sceneSide = 1
                        refreshSceneSide()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> btnLu.text = "LU"
            }
            true
        }

        btnFx.setOnClickListener {
            if (paramView?.visibility == View.VISIBLE) {
                closeParamPage()
            } else if (fxConsumed) {
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
                    blinkHandler.removeCallbacks(fxHeldClear)
                    fxHeld = true
                    fxConsumed = false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    blinkHandler.postDelayed(fxHeldClear, 1500)
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
                        val now = System.currentTimeMillis()
                        if (now - muteComboAt < 700) {
                            globalSaveArmed = true
                            globalSaveAt = now
                            resampling = false
                            engine.nativeCancelRec()
                            Toast.makeText(this, "SAVE GLOBAL : PAD 1-4", Toast.LENGTH_SHORT).show()
                        } else {
                            resampling = true
                            startResampleUI()
                        }
                        muteComboAt = now
                        muteHeld = true
                    } else {
                        muteOn = true
                        btnMute.text = "MUTE \u25cf"
                        engine.nativeSetMute(true)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    muteHeld = false
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
                    fHeld = true
                    if (recArmed) {
                        if (padGrid.padMode == 1) {
                            motionSrcA = !motionSrcA
                            Toast.makeText(this, "MOTION AXE F " + (if (motionSrcA) "ON" else "OFF"), Toast.LENGTH_SHORT).show()
                        } else {
                            loopWetA = !loopWetA
                            Toast.makeText(this, "REC WET F " + (if (loopWetA) "ON" else "OFF"), Toast.LENGTH_SHORT).show()
                        }
                        fRecTap = true
                        refreshBlink()
                    } else if (fxOpen) {
                        fRecTap = true
                        if (assignArm == 0) exitAssignMode() else armAssign(0)
                        refreshBlink()
                    } else {
                        fRecTap = false
                        if (shiftActive() && sendPadBank >= 0 &&
                            System.currentTimeMillis() - sendPadAt < 900
                        ) {
                            applySendCombo(sendPadBank, 1)
                        }
                        fDownAt = System.currentTimeMillis()
                        if (fxHeld) loopWetA = true
                        refreshBlink()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    fHeld = false
                    loopWetA = false
                    if (!fxHeld && !fRecTap) {
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
                    zHeld = true
                    if (recArmed) {
                        if (padGrid.padMode == 1) {
                            motionSrcB = !motionSrcB
                            Toast.makeText(this, "MOTION AXE Z " + (if (motionSrcB) "ON" else "OFF"), Toast.LENGTH_SHORT).show()
                        } else {
                            loopWetB = !loopWetB
                            Toast.makeText(this, "REC WET Z " + (if (loopWetB) "ON" else "OFF"), Toast.LENGTH_SHORT).show()
                        }
                        zRecTap = true
                        refreshBlink()
                    } else if (fxOpen) {
                        zRecTap = true
                        if (assignArm == 1) exitAssignMode() else armAssign(1)
                        refreshBlink()
                    } else {
                        zRecTap = false
                        if (shiftActive() && sendPadBank >= 0 &&
                            System.currentTimeMillis() - sendPadAt < 900
                        ) {
                            applySendCombo(sendPadBank, 2)
                        }
                        zDownAt = System.currentTimeMillis()
                        if (fxHeld) loopWetB = true
                        refreshBlink()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    zHeld = false
                    loopWetB = false
                    if (!fxHeld && !zRecTap) {
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
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    recHeld = false
                    if (e.actionMasked == MotionEvent.ACTION_UP) recTap()
                }
            }
            refreshBlink()
            false
        }

        padGrid.onPadTouch = { n ->
            val bottom = padGrid.padCount - 3
            when {
                globalSaveArmed && System.currentTimeMillis() - globalSaveAt < 1500 &&
                    n in bottom..(bottom + 3) -> {
                    globalSaveArmed = false
                    val g = n - bottom + 1
                    saveProject(g, true)
                    Toast.makeText(this, "GLOBAL $g SAUV\u00c9", Toast.LENGTH_SHORT).show()
                }
                shiftActive() && muteHeld && n in bottom..(bottom + 3) -> {
                    val g = n - bottom + 1
                    loadProject(g, true)
                    Toast.makeText(this, "GLOBAL $g CHARG\u00c9", Toast.LENGTH_SHORT).show()
                }
                heldFxSlot >= 0 && n in 1..8 -> {
                    assignBankFx(n - 1)
                }
                fxHeld -> {
                    fxConsumed = true
                    loopLenSteps = n
                    padGrid.activeBank = n
                }
                lenEntry && n in 1..9 -> lenDigit(n)
                recActive && recMotion && n == recArmPad -> recStop()
                recActive && !recMotion && n == recArmPad -> recStop()
                recArmed && n >= 1 && n <= min(padGrid.padCount, 14) -> armRecDest(n)
                shiftActive() -> {
                    fxConsumed = true
                    startLoopRecording(n)
                    sendPadBank = n - 1
                    sendComboStart = bankFxSend[sendPadBank]
                    sendPadAt = System.currentTimeMillis()
                }
                padGrid.padCount == 12 && n in 9..12 -> toggleSolo(n - 9)
                padGrid.padMode != 2 && n - 1 < 14 && snapPads[n - 1] != null -> recallSnap(n - 1)
                n <= 8 -> {
                    playerBank = n - 1
                    if (engine.nativeBankHasLoop(n - 1)) {
                        engine.nativeToggleBank(n - 1)
                        engine.nativeSetPlayerBank(n - 1)
                        bankOn[n - 1] = !bankOn[n - 1]
                        refreshBankFx()
                        pushSampleLog("B$n \u00b7 ${bankDisplayName(n - 1)}")
                    } else if (midiLoops[n - 1].isNotEmpty()) {
                        midiPlaying[n - 1] = !midiPlaying[n - 1]
                        bankOn[n - 1] = midiPlaying[n - 1]
                        if (midiPlaying[n - 1]) {
                            midiStartMs[n - 1] = System.currentTimeMillis()
                            midiLastPos[n - 1] = -1f
                        }
                    } else {
                        startLoopRecording(n)
                        bankOn[n - 1] = true
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
        padGrid.onPadDown = { n ->
            heldBankPad = n
            if (recArmed && !recActive && n == recArmPad) {
                lenHandler.postDelayed({
                    if (heldBankPad == n && recArmed && !recActive && !lenEntry) {
                        lenEntry = true
                        lenFirst = 0
                        Toast.makeText(
                            this,
                            "LONGUEUR ? PADS 1-9 (1 puis 4 = 14, max 19)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }, 2000)
            }
        }
        padGrid.onPadUp = { heldBankPad = -1 }

        val vuModule = LinearLayout(this)
        vuModule.orientation = LinearLayout.VERTICAL
        vuModule.gravity = Gravity.CENTER_HORIZONTAL
        vuModule.addView(vuMeter, LinearLayout.LayoutParams((96 * d).toInt(), (52 * d).toInt()))
        val lcdParams = LinearLayout.LayoutParams((96 * d).toInt(), (22 * d).toInt())
        lcdParams.topMargin = (4 * d).toInt()
        vuModule.addView(bpmLcd, lcdParams)

        val row1 = LinearLayout(this)
        row1.orientation = LinearLayout.HORIZONTAL
        row1.gravity = Gravity.CENTER_VERTICAL
        row1.addView(btnFx, LinearLayout.LayoutParams((36 * d).toInt(), (36 * d).toInt()))
        row1.addView(btnArp, LinearLayout.LayoutParams((34 * d).toInt(), (34 * d).toInt()))
        val sqParams = LinearLayout.LayoutParams((34 * d).toInt(), (34 * d).toInt())
        sqParams.setMargins((4 * d).toInt(), 0, 0, 0)
        row1.addView(btnF, sqParams)
        row1.addView(btnZ, sqParams)
        val recParams = LinearLayout.LayoutParams((40 * d).toInt(), (40 * d).toInt())
        recParams.setMargins((4 * d).toInt(), 0, 0, 0)
        row1.addView(btnRec, recParams)
        row1.addView(btnUndo, LinearLayout.LayoutParams((34 * d).toInt(), (34 * d).toInt()))
        inKnob = LineInKnob(this)
        inKnob.onValueChange = { v ->
            inVol = v
            engine.nativeSetInGain(v)
        }
        val knobParams = LinearLayout.LayoutParams((24 * d).toInt(), (30 * d).toInt())
        knobParams.setMargins((4 * d).toInt(), 0, 0, 0)
        row1.addView(inKnob, knobParams)
        row1.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val vuWrap = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        vuWrap.setMargins((4 * d).toInt(), 0, 0, 0)
        row1.addView(vuModule, vuWrap)
        val playParams = LinearLayout.LayoutParams((34 * d).toInt(), (34 * d).toInt())
        playParams.setMargins((6 * d).toInt(), 0, 0, 0)
        row1.addView(btnPlay, playParams)

        val zuLuBtns = LinearLayout(this)
        zuLuBtns.orientation = LinearLayout.HORIZONTAL
        val zuParams = LinearLayout.LayoutParams(
            (64 * d).toInt(), (36 * d).toInt()
        )
        zuParams.setMargins((2 * d).toInt(), (6 * d).toInt(), 0, 0)
        zuLuBtns.addView(btnZu, zuParams)
        zuLuBtns.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val luParams = LinearLayout.LayoutParams(
            (64 * d).toInt(), (36 * d).toInt()
        )
        luParams.setMargins(0, (6 * d).toInt(), (2 * d).toInt(), 0)
        zuLuBtns.addView(btnLu, luParams)

        wireFaders()

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding((6 * d).toInt(), (4 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
        gridParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (96 * d).toInt()
        )
        gridParams.setMargins(0, (6 * d).toInt(), 0, 0)
        column.addView(padGrid, gridParams)
        val slog = TextView(this)
        slog.setTextColor(silk)
        slog.textSize = 9f
        slog.typeface = markerFont(this)
        slog.maxLines = 1
        slog.ellipsize = android.text.TextUtils.TruncateAt.START
        slog.setPadding((4 * d).toInt(), 0, (4 * d).toInt(), 0)
        slog.visibility = if (showSampleLog) View.VISIBLE else View.GONE
        sampleLogView = slog
        column.addView(
            slog,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
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
        val catView = PunkCatView(this)
        val catParams = LinearLayout.LayoutParams((26 * d).toInt(), (26 * d).toInt())
        catParams.setMargins((4 * d).toInt(), 0, (2 * d).toInt(), 0)
        faderRow.addView(catView, catParams)
        val faderRowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (40 * d).toInt()
        )
        faderRowParams.topMargin = (8 * d).toInt()
        column.addView(faderRow, faderRowParams)

        content = LinearLayout(this)
        content.orientation = LinearLayout.HORIZONTAL
        content.background = ZebraBgDrawable()
        val padParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)

        padPane = FrameLayout(this)

        val presetRow = LinearLayout(this)
        presetRow.orientation = LinearLayout.HORIZONTAL
        for (i in 0 until 9) {
            val b = makeButton((i + 1).toString(), cream)
            b.textSize = 12f
            b.setOnClickListener { presetTap(i) }
            b.setOnLongClickListener { presetLongTap(i); true }
            b.setOnTouchListener { _, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> heldFxSlot = i
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> heldFxSlot = -1
                }
                false
            }
            presetBtns.add(b)
            val bp = LinearLayout.LayoutParams(0, (26 * d).toInt(), 1f)
            bp.setMargins((2 * d).toInt(), 0, (2 * d).toInt(), 0)
            presetRow.addView(b, bp)
        }
        val pb = makeButton("PUNCH", yellow)
        pb.textSize = 11f
        pb.setOnClickListener { setPunchMode(!punchMode) }
        btnPunch = pb
        val pbp = LinearLayout.LayoutParams(0, (26 * d).toInt(), 1f)
        pbp.setMargins((2 * d).toInt(), 0, (2 * d).toInt(), 0)
        presetRow.addView(pb, pbp)

        val leftColumn = LinearLayout(this)
        leftColumn.orientation = LinearLayout.VERTICAL
        val presetRowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        presetRowParams.setMargins((4 * d).toInt(), (4 * d).toInt(), (4 * d).toInt(), 0)
        leftColumn.addView(presetRow, presetRowParams)
        val padPaneParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )
        padPaneParams.setMargins(0, (4 * d).toInt(), 0, 0)

        vuMeter.onLongPress = {
            if (!simulLock) openMenuPanel()
        }

        vuMeter.onVolDelta = { dv ->
            masterVol = (masterVol + dv).coerceIn(0f, 2f)
            engine.nativeSetMasterVolume(masterVol)
            showVuOverlay("VOL " + ((masterVol * 50).toInt().coerceIn(0, 100)).toString() + "%")
        }
        vuMeter.onInDelta = { dxv ->
            inVol = (inVol + dxv).coerceIn(0f, 2f)
            engine.nativeSetInGain(inVol)
            inKnob.setVal(inVol)
            showVuOverlay("IN " + ((inVol * 50).toInt().coerceIn(0, 100)).toString() + "%")
        }

        val edgeColumn = LinearLayout(this)
        edgeColumn.orientation = LinearLayout.VERTICAL
        edgeColumn.setPadding((3 * d).toInt(), (6 * d).toInt(), (3 * d).toInt(), (6 * d).toInt())
        edgeColumn.addView(View(this), LinearLayout.LayoutParams(0, 0, 1f))
        modeBadge = ModeBadgeView(this)
        modeBadge.onBadgeTap = {
            padGrid.padMode = (padGrid.padMode + 1) % 3
            sceneMode = if (padGrid.padMode == 1) 1 else 0
            modeBadge.mode = padGrid.padMode
            Toast.makeText(
                this,
                when (padGrid.padMode) {
                    1 -> "MODE MOTION"
                    2 -> "MODE SAMPLES"
                    else -> "MODE SC\u00c8NE"
                },
                Toast.LENGTH_SHORT
            ).show()
            refreshMenu()
        }
        edgeColumn.addView(modeBadge, LinearLayout.LayoutParams((40 * d).toInt(), (20 * d).toInt()))
        val badgeGap = View(this)
        edgeColumn.addView(badgeGap, LinearLayout.LayoutParams(0, (6 * d).toInt()))
        edgeColumn.addView(btnShift, LinearLayout.LayoutParams((40 * d).toInt(), (40 * d).toInt()))
        val shiftGap = View(this)
        edgeColumn.addView(
            shiftGap,
            LinearLayout.LayoutParams(0, (8 * d).toInt())
        )
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

        val punch = PunchPadView(this)
        punch.visibility = View.GONE
        punch.onPunchDown = { z ->
            if (shiftActive()) {
                val fx = punchFx[punchCol][z]
                if (fx >= 0) {
                    openParamPage(fx)
                } else {
                    Toast.makeText(this, "PAS D'\u00c9DIT ICI", Toast.LENGTH_SHORT).show()
                }
            } else {
                punchApply(z, true)
            }
        }
        punch.onPunchUp = { z -> punchApply(z, false) }
        punch.onExit = { setPunchMode(false) }
        punch.shiftCheck = { shiftActive() }
        punch.onCollectionChange = { c -> setPunchCollection(c) }
        punchView = punch
        padPane.addView(
            punch,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val ppage = ParamPageView(this)
        ppage.visibility = View.GONE
        ppage.onParam = { k, v ->
            val fx = paramFx
            if (fx >= 0) {
                if (snapHold && snapQuietFor(fx)) {
                    if (!snapHoldFx.containsKey(fx)) snapHoldFx[fx] = fxParamVals[fx].copyOf()
                    fxParamVals[fx][k] = v
                } else {
                    fxParamVals[fx][k] = v
                    engine.nativeSetFxParam(fx, k, v)
                }
            }
        }
        ppage.onAssignCycle = { k -> cycleParamAssign(k) }
        ppage.onExit = { closeParamPage() }
        ppage.shiftCheck = { shiftActive() }
        ppage.onMasterToggle = {
            val fx = paramFx
            if (fx >= 0) {
                if (masterFx.contains(fx)) {
                    masterFx.remove(fx)
                    Toast.makeText(this, "FX RETIRE DU MASTER", Toast.LENGTH_SHORT).show()
                } else {
                    masterFx.add(fx)
                    Toast.makeText(this, "FX ROUTE AU MASTER", Toast.LENGTH_SHORT).show()
                }
                ppage.masterOn = masterFx.contains(fx)
                ppage.refresh()
                refreshFxHighlight()
            }
        }
        ppage.onSnapToggle = {
            val fx = paramFx
            if (fx >= 0) {
                val next = when (snapFxOverride[fx]) { null -> 1; true -> 2; else -> 0 }
                if (next == 0) snapFxOverride.remove(fx) else snapFxOverride[fx] = (next == 1)
                ppage.snapMode = next
                ppage.refresh()
                Toast.makeText(
                    this,
                    "SNAP : " + if (next == 1) "SILENCE" else if (next == 2) "SON" else "AUTO (MENU)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        paramView = ppage
        padPane.addView(
            ppage,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        drumGrid = DrumGridView(this)
        drumGrid.visibility = View.GONE
        drumGrid.onDrum = { n -> drumHit(n) }
        drumGrid.onExit = {
            toolPage = 0
            drumGrid.visibility = View.GONE
        }
        padPane.addView(
            drumGrid,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        if (verticalUi) {
            content.addView(
                padPane,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.1f
                )
            )
            content.addView(
                row1,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            val lower = LinearLayout(this)
            lower.orientation = LinearLayout.HORIZONTAL
            val lowerCol = LinearLayout(this)
            lowerCol.orientation = LinearLayout.VERTICAL
            lowerCol.setPadding((6 * d).toInt(), (4 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            lowerCol.addView(presetRow, presetRowParams)
            lowerCol.addView(padGrid, gridParams)
            lowerCol.addView(zuLuBtns)
            lowerCol.addView(
                zuLuFader,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
            )
            lowerCol.addView(faderRow, faderRowParams)
            lower.addView(
                lowerCol,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            )
            lower.addView(
                edgeColumn,
                LinearLayout.LayoutParams((46 * d).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
            )
            content.addView(
                lower,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.4f
                )
            )
        } else {
            column.addView(row1)
            leftColumn.addView(padPane, padPaneParams)
            content.addView(leftColumn, padParams)
            content.addView(
                edgeColumn,
                LinearLayout.LayoutParams((46 * d).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
            )
            content.addView(
                column,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            )
        }

        buildFxPanel(d)
        buildScalePanel(d)
        buildArpPanel(d)
        buildProjPanel(d)
        buildStepPanel(d)
        buildMenuPanel(d)
        refreshPresetRow()
        engine.nativeSetInGain(inKnob.value)
        refreshSceneSide()
        seqHandler.postDelayed(seqTick, 20)
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

        playing = engine.nativeStart(false)
        if (!playing) {
            Toast.makeText(this, "AUDIO : demarrage impossible", Toast.LENGTH_LONG).show()
        }
        for (f in 0 until 62) {
            val d = fxParamDefaults[f] ?: continue
            for (k in d.indices) engine.nativeSetFxParam(f, k, d[k])
        }
        refreshPlayBtn()
        if (globalFile(0).exists()) {
            loadProject(0, global = true)
            Toast.makeText(this, "TEMPLATE DE BASE CHARGE", Toast.LENGTH_SHORT).show()
        }
    }

    private var swipeX0 = 0f
    private var swipeY0 = 0f

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        if (fxOpen || paramView?.visibility == View.VISIBLE) {
            val loc = IntArray(2)
            btnFx.getLocationOnScreen(loc)
            val dd = resources.displayMetrics.density
            if (e.actionMasked == MotionEvent.ACTION_DOWN &&
                e.rawX >= loc[0] - 6 * dd && e.rawX <= loc[0] + btnFx.width + 6 * dd &&
                e.rawY >= loc[1] - 6 * dd && e.rawY <= loc[1] + btnFx.height + 6 * dd) {
                closeParamPage()
                if (fxOpen) closeFx()
                return true
            }
        }
        if (::projOverlay.isInitialized && projOverlay.visibility == View.VISIBLE) {
            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                swipeX0 = e.x
                swipeY0 = e.y
            } else if (e.actionMasked == MotionEvent.ACTION_UP) {
                val dx = e.x - swipeX0
                val dy = e.y - swipeY0
                val dd = resources.displayMetrics.density
                if (Math.abs(dx) > 80 * dd && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                    if (dx < 0 && projPage < 3) {
                        projPage++
                        refreshProjSlots()
                    } else if (dx > 0 && projPage > 0) {
                        projPage--
                        refreshProjSlots()
                    }
                }
            }
        }
        return super.dispatchTouchEvent(e)
    }

    private fun metaFile(): File = File(projDir(), "meta.json")

    private fun saveProjMeta() {
        try {
            val json = JSONObject()
            val names = JSONObject()
            for ((k, v) in projNames) names.put(k.toString(), v)
            val cols = JSONObject()
            for ((k, v) in projCols) cols.put(k.toString(), v)
            json.put("names", names)
            json.put("cols", cols)
            metaFile().writeText(json.toString())
        } catch (e: Exception) {
        }
    }

    private fun loadProjMeta() {
        try {
            val f = metaFile()
            if (!f.exists()) return
            val json = JSONObject(f.readText())
            json.optJSONObject("names")?.let { nm ->
                for (key in nm.keys()) projNames[key.toInt()] = nm.getString(key)
            }
            json.optJSONObject("cols")?.let { cl ->
                for (key in cl.keys()) projCols[key.toInt()] = cl.getInt(key)
            }
        } catch (e: Exception) {
        }
    }

    private fun bankDisplayName(b: Int): String {
        val custom = bankLabels[b]
        if (custom.isNotEmpty()) return custom
        val frames = engine.nativeBankFrames(b)
        if (frames <= 0) return ""
        val dur = String.format(java.util.Locale.US, "%.1f", frames / 48000f)
        val t = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.US).format(java.util.Date(bankRecAt[b]))
        return "REC $t \u00b7 ${dur}s"
    }

    private fun pushSampleLog(msg: String) {
        sampleLogView?.text = msg
    }

    private fun askSampleName(b: Int) {
        val dd = resources.displayMetrics.density
        val et = android.widget.EditText(this)
        et.setText(bankLabels[b])
        et.setSingleLine(true)
        val wrap = android.widget.FrameLayout(this)
        wrap.setPadding((48 * dd).toInt(), (16 * dd).toInt(), (48 * dd).toInt(), 0)
        wrap.addView(et)
        android.app.AlertDialog.Builder(this)
            .setTitle("NOM SAMPLE B${b + 1}")
            .setView(wrap)
            .setPositiveButton("OK") { _, _ ->
                bankLabels[b] = et.text.toString().trim()
                pushSampleLog("B${b + 1} \u00b7 ${bankDisplayName(b)}")
            }
            .setNegativeButton("ANNULER", null)
            .show()
    }

    private fun askProjName(slot: Int) {
        val dd = resources.displayMetrics.density
        val et = android.widget.EditText(this)
        et.setText(projNames[slot] ?: "")
        et.setSingleLine(true)
        val cols = intArrayOf(cream, yellow, green, turquoise, pink, violet)
        var sel = projCols[slot] ?: 0
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding((48 * dd).toInt(), (16 * dd).toInt(), (48 * dd).toInt(), 0)
        row.addView(et)
        val swatches = ArrayList<Button>()
        val swRow = LinearLayout(this)
        for (c in cols) {
            val sb = Button(this)
            sb.text = "\u25a0"
            sb.setTextColor(c)
            sb.background = null
            sb.setOnClickListener {
                sel = c
                for (i2 in 0 until swatches.size) swatches[i2].alpha = if (cols[i2] == sel) 1f else 0.4f
            }
            swatches.add(sb)
            swRow.addView(sb, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        for (i2 in 0 until swatches.size) swatches[i2].alpha = if (cols[i2] == sel) 1f else 0.4f
        row.addView(swRow)
        android.app.AlertDialog.Builder(this)
            .setTitle("PROJET $slot")
            .setView(row)
            .setPositiveButton("OK") { _, _ ->
                val nm = et.text.toString().trim()
                if (nm.isEmpty()) projNames.remove(slot) else projNames[slot] = nm
                projCols[slot] = sel
                saveProjMeta()
                refreshProjSlots()
            }
            .setNegativeButton("ANNULER", null)
            .show()
    }

    private fun panicReset() {
        recEndHandler.removeCallbacksAndMessages(null)
        motionHandler.removeCallbacksAndMessages(null)
        if (recActive) engine.nativeStopRec()
        recActive = false
        recMotion = false
        recArmed = false
        recSnapSide = -1
        recArmPad = -1
        padGrid.recArmed = false
        padGrid.recPad = -1
        stopMotionPlayback()
        for (b in 0 until 8) {
            if (bankOn[b]) {
                bankOn[b] = false
                engine.nativeToggleBank(b)
            }
            midiPlaying[b] = false
        }
        for (f in 0 until 46) {
            if (fxActive[f]) {
                fxActive[f] = false
                engine.nativeSetFx(f, false)
            }
        }
        for (f in 0 until 62) fxParamInit[f] = false
        for (f in 0 until 62) {
            val df = fxParamDefaults[f]
            for (k in 0 until 8) engine.nativeSetFxParam(f, k, if (df != null && k < df.size) df[k] else 0f)
        }
        paramActive.fill(false)
        for (pp in 0 until 8) engine.nativeSetParam(pp, 0f, false)
        activeFilter = 0
        engine.nativeSetFilterType(0)
        assignXIdx = 0
        assignYIdx = 1
        engine.nativeSetAssign(0, 0)
        engine.nativeSetAssign(1, 1)
        refreshAssignLabels()
        axisMulti[0].clear()
        axisMulti[1].clear()
        pushAxisMulti(0)
        pushAxisMulti(1)
        exitAssignMode()
        punchLatched.fill(false)
        for (g in 0 until 4) soloGroups[g] = false
        engine.nativeSetSoloMask(0)
        padGrid.soloMask = 0
        if (muteOn) {
            muteOn = false
            btnMute.text = "MUTE"
        }
        engine.nativeSetMute(false)
        arpOn = false
        engine.nativeSetArp(false, arpMode, arpRateIdx, arpOctIdx, arpGateIdx, arpOctJump)
        for (i in 0 until 2) {
            lfoOn[i] = false
            engine.nativeSetCustomLfo(
                i, lfoShapes[i], false, lfoRateIdxArr[i],
                lfoDepthIdxArr[i].toFloat(), lfoAmountIdxArr[i].toFloat(),
                lfoTargets[i], lfoWaves[i]
            )
        }
        activeZu = -1
        activeLu = -1
        refreshSnapGrid()
        refreshBlink()
        refreshFxHighlight()
        refreshBankFx()
        refreshPlayBtn()
        Toast.makeText(this, "PANIQUE : TOUT EST CALME", Toast.LENGTH_SHORT).show()
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
        btnF.text = when {
            !axisAOn -> "F OFF"
            axisAHold -> "F HOLD"
            else -> "F MOM"
        }
        btnF.textSize = 9f
        btnF.setTextColor(when {
            !axisAOn -> silk
            axisAHold -> amber
            else -> yellow
        })
        btnZ.text = when {
            !axisBOn -> "Z OFF"
            axisBHold -> "Z HOLD"
            else -> "Z MOM"
        }
        btnZ.textSize = 9f
        btnZ.setTextColor(when {
            !axisBOn -> silk
            axisBHold -> amber
            else -> pink
        })
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
        recActive = true
        recMotion = false
        recArmPad = bank + 1
        padGrid.recPad = bank
        val ms = (loopLenSteps * 60000f / bpmLcd.bpm.coerceAtLeast(40f)).toLong()
        recEndHandler.removeCallbacksAndMessages(null)
        recEndHandler.postDelayed({ padGrid.recPad = -1 }, ms + 200)
    }

    private fun togglePlay() {
        if (!playing) {
            playing = engine.nativeStart(false)
            Toast.makeText(
                this,
                if (playing) "MOTEUR ON" else "MOTEUR : ECHEC",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            engine.nativeStop()
            playing = false
            vuMeter.levelL = 0f
            vuMeter.levelR = 0f
            Toast.makeText(this, "MOTEUR OFF", Toast.LENGTH_SHORT).show()
        }
        refreshPlayBtn()
    }

    private fun refreshPlayBtn() {
        btnPlay.text = if (playing) "STOP" else "PLAY"
        btnPlay.setTextColor(if (playing) red else green)
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

        val editB = makeButton("EDIT", silk)
        editB.textSize = 11f
        editB.setOnClickListener {
            editFxMode = !editFxMode
            editB.setTextColor(if (editFxMode) amber else silk)
        }
        panel.addView(
            editB,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * d).toInt() }
        )

        val scroll = android.widget.ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        scroll.addView(list)

        var globalIdx = 0
        var catIdx = 0
        for ((section, effects) in fxSections) {
            val catCol = fxCatCols[catIdx % fxCatCols.size]
            catIdx++
            val header = TextView(this)
            header.text = section
            header.setTextColor(catCol)
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
                val b = makeButton(fx, catCol)
                val bg = GradientDrawable()
                bg.setColor(Color.rgb(16, 18, 24))
                bg.setStroke((1 * d).toInt(), catCol)
                bg.cornerRadius = 6 * d
                b.background = bg
                val idx = globalIdx
                fxButtonCols.add(catCol)
                b.setOnClickListener {
                    activeFx = idx
                    val code = fxCodes.getOrNull(idx) ?: ""
                    if (code.length >= 2) {
                        val num = code.substring(1).toIntOrNull() ?: 0
                        when (code[0]) {
                            'F' -> {
                                activeFilter = num
                                if (!(snapHold && snapTransp)) engine.nativeSetFilterType(num)
                            }
                            'X' -> {
                                if ((shiftActive() || editFxMode) && fxParamDefs.containsKey(num)) {
                                    openParamPage(num)
                                } else {
                                    fxActive[num] = !fxActive[num]
                                    if (!(snapHold && snapQuietFor(num))) engine.nativeSetFx(num, fxActive[num])
                                }
                            }
                            'P' -> {
                                paramActive[num] = !paramActive[num]
                                if (!(snapHold && snapTransp)) engine.nativeSetParam(
                                    num,
                                    if (paramActive[num]) paramDefaults[num] else 0f,
                                    paramActive[num]
                                )
                            }
                            'G' -> {
                                if ((shiftActive() || editFxMode) && fxParamDefs.containsKey(46 + num)) {
                                    openParamPage(46 + num)
                                } else {
                                    genIdx = num
                                    engine.nativeSetGen(num)
                                }
                            }
                        }
                    }
                    pad.activePreset = idx % 9
                    refreshFxHighlight()
                }
                fxButtons.add(b)
                val gp = LinearLayout.LayoutParams(0, (34 * d).toInt(), 1f)
                gp.setMargins((3 * d).toInt(), (3 * d).toInt(), (3 * d).toInt(), (3 * d).toInt())
                row.addView(b, gp)
                globalIdx++
            }
        }

        val hint = TextView(this)
        hint.text = "TAP = ON/OFF \u00b7 SHIFT+TAP = R\u00c9GLAGES \u00b7 SYNTH = JOUER SUR LE PAD"
        hint.setTextColor(Color.rgb(120, 128, 146))
        hint.textSize = 10f
        hint.typeface = markerFont(this)
        hint.setPadding(0, (8 * d).toInt(), 0, 0)
        panel.addView(hint)

        panel.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (300 * d).toInt()
            ).apply { topMargin = (4 * d).toInt() }
        )

        val panelParams = FrameLayout.LayoutParams(
            (430 * d).toInt(), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER
        )
        fxOverlay.addView(panel, panelParams)
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
            val master = code.getOrNull(0) == 'X' && num in masterFx
            b.setTextColor(if (on) amber else if (master) violet else fxButtonCols.getOrElse(i) { silk })
        }
        refreshPresetRow()
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
        exitAssignMode()
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

    private fun globalFile(i: Int): File = File(projDir(), "global_$i.json")

    private fun stateFile(slot: Int, global: Boolean): File =
        if (global) globalFile(slot) else projFile(slot)

    private fun bankFile(slot: Int, bank: Int, global: Boolean = false): File =
        File(projDir(), if (global) "global_${slot}_b$bank.pcm" else "proj_${slot}_b$bank.pcm")

    private fun buildProjPanel(d: Float) {
        loadProjMeta()
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
        val prevB = Button(this)
        prevB.text = "\u2039"
        prevB.setTextColor(silk)
        prevB.textSize = 16f
        prevB.minHeight = 0
        prevB.minimumHeight = 0
        prevB.setPadding(0, 0, 0, 0)
        prevB.background = null
        prevB.setOnClickListener { if (projPage > 0) { projPage--; refreshProjSlots() } }
        titleRow.addView(prevB, LinearLayout.LayoutParams((30 * d).toInt(), (30 * d).toInt()))
        val pageLbl = TextView(this)
        pageLbl.setTextColor(Color.rgb(120, 128, 146))
        pageLbl.textSize = 10f
        pageLbl.typeface = markerFont(this)
        projPageLabel = pageLbl
        titleRow.addView(pageLbl, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        val nextB = Button(this)
        nextB.text = "\u203a"
        nextB.setTextColor(silk)
        nextB.textSize = 16f
        nextB.minHeight = 0
        nextB.minimumHeight = 0
        nextB.setPadding(0, 0, 0, 0)
        nextB.background = null
        nextB.setOnClickListener { if (projPage < 3) { projPage++; refreshProjSlots() } }
        titleRow.addView(nextB, LinearLayout.LayoutParams((30 * d).toInt(), (30 * d).toInt()))
        titleRow.addView(close, LinearLayout.LayoutParams((30 * d).toInt(), (30 * d).toInt()))
        panel.addView(titleRow)

        val hint = TextView(this)
        hint.text = "TAP = CHARGER \u00b7 APUI LONG = SAUVER \u00b7 DOUBLE-TAP = NOMMER \u00b7 SWIPE = PAGE"
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
            b.setOnClickListener {
                val slot = projPage * 8 + s
                val now = System.currentTimeMillis()
                if (lastProjTapSlot == slot && now - lastProjTapAt < 400) {
                    lastProjTapAt = 0
                    askProjName(slot)
                } else {
                    lastProjTapSlot = slot
                    lastProjTapAt = now
                    loadProject(slot)
                }
            }
            b.setOnLongClickListener { saveProject(projPage * 8 + s); true }
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
        for (i in 0 until 8) {
            val slot = projPage * 8 + i + 1
            val b = projSlotButtons[i]
            val exists = projFile(slot).exists()
            val name = projNames[slot]
            b.text = (if (exists) "\u25cf " else "") + (name ?: "PROJ $slot")
            val col = projCols[slot]
            b.setTextColor(when {
                col != null -> col
                exists -> amber
                else -> silk
            })
        }
        projPageLabel?.text = "PAGE ${projPage + 1}/4"
    }

    private fun openProjPanel() {
        refreshProjSlots()
        projOverlay.visibility = View.VISIBLE
    }

    private fun closeProjPanel() {
        projOverlay.visibility = View.GONE
    }

    private fun saveProject(slot: Int, global: Boolean = false) {
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
                            .put("fxOn", JSONArray(p.fxOn.map { if (it) 1 else 0 }))
                            .put("fxP", JSONArray(p.fxParams.map { JSONArray(it.toList()) }))
                            .put("fxA", JSONArray(p.fxAssign.map { JSONArray(it.toList()) }))
                            .put("lfoTg", JSONArray(p.lfoTg.toList()))
                    )
                }
            }
            json.put("presets", presets)
            val banks = JSONArray()
            for (b in 0 until 8) {
                val frames = engine.nativeBankFrames(b)
                banks.put(frames)
                bankFile(slot, b, global).delete()
                if (frames > 0) {
                    val data = FloatArray(frames * 2)
                    if (engine.nativeCopyBank(b, data) > 0) {
                        val bb = ByteBuffer.allocate(frames * 8).order(ByteOrder.LITTLE_ENDIAN)
                        for (f in data) bb.putFloat(f)
                        bankFile(slot, b, global).writeBytes(bb.array())
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
                    val o = JSONObject()
                        .put("ax", s.ax.toDouble())
                        .put("ay", s.ay.toDouble())
                        .put("bx", s.bx.toDouble())
                        .put("by", s.by.toDouble())
                        .put("mix", s.mix.toDouble())
                        .put("filter", s.filter)
                        .put("assignX", s.assignX)
                        .put("assignY", s.assignY)
                        .put("punchCol", s.punchCol2)
                    if (s.punch != null) {
                        o.put("punch", JSONArray(s.punch.map { if (it) 1 else 0 }))
                    }
                    if (s.motA != null) o.put("motA", JSONArray(s.motA.map { it.toDouble() }))
                    if (s.motB != null) o.put("motB", JSONArray(s.motB.map { it.toDouble() }))
                    if (s.motLen > 0) o.put("motLen", s.motLen)
                    snaps.put(o)
                }
            }
            json.put("snaps", snaps)
            val mloops = JSONArray()
            for (b in 0 until 8) {
                val arr = JSONArray()
                for (e in midiLoops[b]) arr.put(JSONArray(e.toList()))
                mloops.put(arr)
            }
            json.put("midiloops", mloops)
            json.put("recMidi", recMidiEngines)
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
            json.put("punchCol", punchCol)
            json.put("fxSend", JSONArray(bankFxSend.toList()))
            json.put("stretch", JSONArray(bankStretch.toList()))
            json.put("bankNames", JSONArray(bankLabels.toList()))
            projFile(slot).writeText(json.toString())
            refreshProjSlots()
        } catch (e: Exception) {
        }
    }

    private fun loadProject(slot: Int, global: Boolean = false) {
        val f = stateFile(slot, global)
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
            val fsArr = json.optJSONArray("fxSend")
            if (fsArr != null) {
                for (b in 0 until min(8, fsArr.length())) {
                    bankFxSend[b] = fsArr.getBoolean(b)
                    engine.nativeSetBankFxSend(b, bankFxSend[b])
                }
            }
            val stArr = json.optJSONArray("stretch")
            if (stArr != null) {
                for (b in 0 until min(8, stArr.length())) {
                    bankStretch[b] = stArr.optInt(b, 0).coerceIn(0, 3)
                    engine.nativeSetStretchMode(b, bankStretch[b])
                }
            }
            val bnArr = json.optJSONArray("bankNames")
            if (bnArr != null) {
                for (b in 0 until min(8, bnArr.length())) bankLabels[b] = bnArr.getString(b)
            }
            for (b in 0 until 8) bankRecAt[b] = System.currentTimeMillis()
            punchCol = json.optInt("punchCol", 0).coerceIn(0, punchLabels.size - 1)
            punchView?.setCollectionIndex(punchCol)
            punchView?.setLabels(punchLabels[punchCol])
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
                        assignY = o.optInt("assignY", 1),
                        fxOn = BooleanArray(46) { f ->
                            val a = o.optJSONArray("fxOn")
                            a != null && f < a.length() && a.optInt(f, 0) == 1
                        },
                        fxParams = Array(62) { f ->
                            val a = o.optJSONArray("fxP")
                            FloatArray(8) { k ->
                                if (a != null && f < a.length())
                                    a.optJSONArray(f)?.optDouble(k, 0.0)?.toFloat() ?: 0f
                                else 0f
                            }
                        },
                        fxAssign = Array(62) { f ->
                            val a = o.optJSONArray("fxA")
                            IntArray(8) { k ->
                                if (a != null && f < a.length())
                                    a.optJSONArray(f)?.optInt(k, 0) ?: 0
                                else 0
                            }
                        },
                        lfoTg = IntArray(2) { l ->
                            val a = o.optJSONArray("lfoTg")
                            if (a != null && l < a.length()) a.optInt(l, -1) else -1
                        }
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
                val bf = bankFile(slot, b, global)
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
                        val pa = o.optJSONArray("punch")
                        val pArr = if (pa != null) BooleanArray(pa.length()) { k -> pa.optInt(k, 0) == 1 } else null
                        fun farr(name: String): FloatArray? {
                            val a2 = o.optJSONArray(name) ?: return null
                            return FloatArray(a2.length()) { k -> a2.getDouble(k).toFloat() }
                        }
                        snapPads[i] = Snap(
                            ax = o.getDouble("ax").toFloat(),
                            ay = o.getDouble("ay").toFloat(),
                            bx = o.getDouble("bx").toFloat(),
                            by = o.getDouble("by").toFloat(),
                            mix = o.getDouble("mix").toFloat(),
                            filter = o.getInt("filter"),
                            assignX = o.optInt("assignX", 0),
                            assignY = o.optInt("assignY", 1),
                            punch = pArr,
                            punchCol2 = o.optInt("punchCol", 0),
                            motA = farr("motA"),
                            motB = farr("motB"),
                            motLen = o.optInt("motLen", 0)
                        )
                    }
                }
            }
            val mloops = json.optJSONArray("midiloops")
            if (mloops != null) {
                for (b in 0 until min(8, mloops.length())) {
                    midiLoops[b].clear()
                    val arr = mloops.optJSONArray(b) ?: continue
                    for (k in 0 until arr.length()) {
                        val e = arr.optJSONArray(k) ?: continue
                        if (e.length() >= 5) {
                            midiLoops[b].add(FloatArray(5) { t -> e.getDouble(t).toFloat() })
                        }
                    }
                }
            }
            recMidiEngines = json.optBoolean("recMidi", true)
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
            assignY = assignYIdx,
            fxOn = fxActive.copyOf(),
            fxParams = Array(62) { fxParamVals[it].copyOf() },
            fxAssign = Array(62) { fxParamAssign[it].copyOf() },
            lfoTg = lfoTargets.copyOf()
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
        for (f in 0 until 62) {
            if (f < 46 && fxActive[f] != p.fxOn[f]) {
                fxActive[f] = p.fxOn[f]
                engine.nativeSetFx(f, fxActive[f])
            }
            for (k in 0 until 8) {
                fxParamVals[f][k] = p.fxParams[f][k]
                engine.nativeSetFxParam(f, k, fxParamVals[f][k])
            }
        }
        for (f in 0 until 62) for (k in 0 until 8) fxParamAssign[f][k] = p.fxAssign[f][k]
        var xT = -1
        val lT = intArrayOf(-1, -1)
        for (f in 0 until 62) for (k in 0 until 8) {
            when (p.fxAssign[f][k]) {
                1 -> if (xT < 0) xT = 8 + f * 8 + k
                2 -> if (lT[0] < 0) lT[0] = 8 + f * 8 + k
                3 -> if (lT[1] < 0) lT[1] = 8 + f * 8 + k
            }
        }
        if (xT >= 0) engine.nativeSetAssign(0, xT)
        for (l in 0..1) {
            if (lT[l] >= 0) {
                lfoTargets[l] = lT[l]
                pushLfo(l)
            }
        }
        pad.arpOn = arpOn
        pushArp()
        refreshScaleHighlight()
        refreshAssignLabels()
        refreshFxHighlight()
        paramView?.refresh()
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
        btnRec.setTextColor(if ((recHeld || recArmed || recActive) && blinkOn) Color.WHITE else red)
        if (shiftLocked) btnShift.alpha = if (blinkOn) 1f else 0.55f
    }

    private val recEndRunnable = Runnable {
        if (recActive) {
            if (recMotion) finalizeMotionRec() else {
                recActive = false
                val b = recArmPad - 1
                if (b in 0 until 8) pushSampleLog("B${b + 1} \u00b7 ${bankDisplayName(b)}")
            }
            recMotion = false
            padGrid.recPad = -1
            refreshBlink()
        }
    }

    private fun startLoopRecording(bank: Int) {
        val wet = loopWetA || loopWetB || recResampleFx
        recResampleFx = false
        engine.nativeRecordBank(bank - 1, loopLenSteps, wet)
        resetSteps(bank - 1)
        recActive = true
        recMotion = false
        recArmPad = bank
        recStartMs = System.currentTimeMillis()
        bankRecAt[bank - 1] = recStartMs
        recLenMs = (loopLenSteps * 60000f / bpmLcd.bpm.coerceAtLeast(40f)).toLong()
        padGrid.recPad = bank - 1
        recEndHandler.removeCallbacksAndMessages(null)
        recEndHandler.postDelayed(recEndRunnable, recLenMs + 200)
        refreshBlink()
    }

    private fun recTap() {
        if (lenEntry) {
            lenEntry = false
            lenFirst = 0
            lenHandler.removeCallbacksAndMessages(null)
        }
        when {
            recActive -> {
                if (recMotion) finalizeMotionRec() else stopSampleRec()
                recArmed = false
                recSnapSide = -1
                recArmPad = -1
                padGrid.recArmed = false
            }
            recSnapSide >= 0 && recArmPad > 0 -> {
                captureSnap(recSnapSide, recArmPad - 1)
                endSnapHold()
                recArmed = false
                recSnapSide = -1
                recArmPad = -1
                padGrid.recArmed = false
                padGrid.recPad = -1
            }
            recArmPad > 0 -> {
                recArmed = false
                padGrid.recArmed = false
                if (padGrid.padMode == 1) startMotionRec() else startLoopRecording(recArmPad)
            }
            else -> {
                recArmed = !recArmed
                recArmPad = -1
                recSnapSide = -1
                endSnapHold()
                motionSrcA = false
                motionSrcB = false
                recResampleFx = recArmed && shiftActive()
                padGrid.recArmed = recArmed
                padGrid.recPad = -1
                Toast.makeText(
                    this,
                    if (recArmed) "REC : " + when {
                        recResampleFx -> "RESAMPLING FX \u2014 PAD + LONGUEUR"
                        padGrid.padMode == 1 -> "F/Z + PAD"
                        else -> "PAD (F/Z = WET)"
                    }
                    else "REC OFF",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        refreshBlink()
    }

    private fun armRecDest(n: Int) {
        if (recSnapSide >= 0) {
            val n2 = min(padGrid.padCount, 14)
            val zone = if (recSnapSide == 0) 0 until n2 / 2 else n2 / 2 until n2
            if (n - 1 in zone) {
                recArmPad = n
                padGrid.recPad = n - 1
                beginSnapHold()
                Toast.makeText(this, "SNAPSHOT PAD $n \u2014 REC = CAPTURER", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "C\u00d4T\u00c9 " + (if (recSnapSide == 0) "ZU" else "LU") + " UNIQUEMENT",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            recArmPad = n
            padGrid.recPad = n - 1
            val src = when {
                padGrid.padMode == 1 -> "MOTION"
                loopWetA || loopWetB -> "WET"
                else -> "DRY"
            }
            Toast.makeText(this, "PAD $n ARM\u00c9 ($src) \u2014 REC = LANCER", Toast.LENGTH_SHORT).show()
        }
        refreshBlink()
    }

    private fun stopSampleRec() {
        engine.nativeStopRec()
        recActive = false
        recMotion = false
        padGrid.recPad = -1
        recEndHandler.removeCallbacksAndMessages(null)
        val b = recArmPad - 1
        if (b in 0 until 8) pushSampleLog("B${b + 1} \u00b7 ${bankDisplayName(b)}")
        Toast.makeText(this, "REC STOP", Toast.LENGTH_SHORT).show()
        refreshBlink()
    }

    private fun recStop() {
        if (recMotion) finalizeMotionRec() else stopSampleRec()
        recArmed = false
        recSnapSide = -1
        endSnapHold()
        recArmPad = -1
        padGrid.recArmed = false
    }

    private fun startMotionRec() {
        if (!motionSrcA && !motionSrcB) {
            motionSrcA = true
            motionSrcB = true
        }
        motA.clear()
        motB.clear()
        recActive = true
        recMotion = true
        recStartMs = System.currentTimeMillis()
        recLenMs = (loopLenSteps * 60000f / bpmLcd.bpm.coerceAtLeast(40f)).toLong()
        padGrid.recPad = recArmPad - 1
        recEndHandler.removeCallbacksAndMessages(null)
        recEndHandler.postDelayed(recEndRunnable, recLenMs + 200)
        motionHandler.postDelayed(motionCaptureTick, 40)
        refreshBlink()
    }

    private val motionCaptureTick = object : Runnable {
        override fun run() {
            if (!recActive || !recMotion) return
            if (motionSrcA) {
                motA.add(pad.ax)
                motA.add(pad.ay)
            }
            if (motionSrcB) {
                motB.add(pad.bx)
                motB.add(pad.by)
            }
            motionHandler.postDelayed(this, 40)
        }
    }

    private fun finalizeMotionRec() {
        val idx = recArmPad - 1
        if (idx in snapPads.indices) {
            val a = if (motionSrcA && motA.size >= 4) motA.toFloatArray() else null
            val b = if (motionSrcB && motB.size >= 4) motB.toFloatArray() else null
            snapPads[idx] = liveSnap().copy(motA = a, motB = b, motLen = loopLenSteps)
            val n = min(padGrid.padCount, 14)
            if (idx < n / 2) activeZu = idx else activeLu = idx
            refreshSnapGrid()
        }
        recActive = false
        recMotion = false
        motionSrcA = false
        motionSrcB = false
        motionHandler.removeCallbacks(motionCaptureTick)
        Toast.makeText(this, "MOTION ENREGISTR\u00c9E", Toast.LENGTH_SHORT).show()
        refreshBlink()
    }

    private fun startMotionPlayback(idx: Int) {
        val s = snapPads[idx] ?: return
        motionPlaying = idx
        motionPhase = 0f
        playAx = s.ax
        playAy = s.ay
        playBx = s.bx
        playBy = s.by
        motionHandler.removeCallbacks(motionPlayTick)
        motionHandler.postDelayed(motionPlayTick, 40)
    }

    private fun stopMotionPlayback() {
        motionPlaying = -1
        motionHandler.removeCallbacks(motionPlayTick)
    }

    private val motionPlayTick = object : Runnable {
        override fun run() {
            if (motionPlaying < 0) return
            val s = snapPads[motionPlaying]
            if (s == null || ((s.motA == null || s.motA!!.size < 4) && (s.motB == null || s.motB!!.size < 4))) {
                motionPlaying = -1
                return
            }
            val durMs = s.motLen.coerceAtLeast(1) * 60000f / bpmLcd.bpm.coerceAtLeast(40f)
            val adv = 40f / durMs
            motionPhase += adv
            val a = s.motA
            if (a != null && a.size >= 4) {
                val pts = a.size / 2
                val ph = motionPhase % 1f * pts
                val i = ph.toInt().coerceAtMost(pts - 1)
                val j = (i + 1) % pts
                val f = ph - i
                playAx = (a[i * 2] + (a[j * 2] - a[i * 2]) * f).coerceIn(0f, 1f)
                playAy = (a[i * 2 + 1] + (a[j * 2 + 1] - a[i * 2 + 1]) * f).coerceIn(0f, 1f)
                engine.nativeSetAx(playAx)
                engine.nativeSetAy(playAy)
            }
            val b = s.motB
            if (b != null && b.size >= 4) {
                val pts = b.size / 2
                val ph = motionPhase % 1f * pts
                val i = ph.toInt().coerceAtMost(pts - 1)
                val j = (i + 1) % pts
                val f = ph - i
                playBx = (b[i * 2] + (b[j * 2] - b[i * 2]) * f).coerceIn(0f, 1f)
                playBy = (b[i * 2 + 1] + (b[j * 2 + 1] - b[i * 2 + 1]) * f).coerceIn(0f, 1f)
                engine.nativeSetBx(playBx)
                engine.nativeSetBy(playBy)
            }
            pad.setCursor(playAx, playAy, playBx, playBy)
            motionHandler.postDelayed(this, 40)
        }
    }

    private fun bankLenMs(b: Int): Long {
        val steps = if (bankSteps[b] > 0) bankSteps[b] else loopLenSteps
        return (steps * 60000f / bpmLcd.bpm.coerceAtLeast(40f)).toLong()
    }

    private val seqTick = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            for (b in 0 until 8) {
                if (!midiPlaying[b]) continue
                val loop = midiLoops[b]
                if (loop.isEmpty()) {
                    midiPlaying[b] = false
                    continue
                }
                val len = bankLenMs(b).coerceAtLeast(200).toFloat()
                val pos = ((now - midiStartMs[b]) % len.toLong()).toFloat()
                val last = midiLastPos[b]
                for (e in loop) {
                    val ep = e[0]
                    val hit = last < 0f || (ep > last && ep <= pos) || (pos < last && (ep > last || ep <= pos))
                    if (hit) {
                        when (e[1].toInt()) {
                            0 -> engine.nativeDrumHit(b, -1, 0, e[3])
                            1 -> engine.nativeDrumHit(b, e[2].toInt(), e[4].toInt(), 0f)
                            else -> engine.nativeDrumHit(b, -1, e[4].toInt(), e[3])
                        }
                    }
                }
                midiLastPos[b] = pos
            }
            seqHandler.postDelayed(this, 20)
        }
    }

    private fun drumHit(n: Int) {
        val count = drumGrid.padCount
        val pitch = n - count / 2f
        when (drumGrid.drumMode) {
            0 -> engine.nativeDrumHit(playerBank, -1, 0, pitch)
            1 -> engine.nativeDrumHit(playerBank, n, count, 0f)
            else -> engine.nativeDrumHit(playerBank, -1, count, pitch)
        }
        if (recActive && !recMotion && recMidiEngines && recArmPad in 1..8 && recLenMs > 0) {
            val step = recLenMs / loopLenSteps.coerceAtLeast(1) / 4f
            val pos = (System.currentTimeMillis() - recStartMs) % recLenMs
            val q = (Math.round(pos / step) * step).toFloat() % recLenMs.toFloat()
            midiLoops[recArmPad - 1].add(
                floatArrayOf(q, drumGrid.drumMode.toFloat(), n.toFloat(), pitch, count.toFloat())
            )
        }
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
        motionHandler.removeCallbacksAndMessages(null)
        seqHandler.removeCallbacksAndMessages(null)
        if (playing) {
            engine.nativeStop()
            playing = false
        }
    }

    private fun wireFaders() {
        mixFader.onValueChange = { v -> if (!(snapHold && snapTransp)) engine.nativeSetMix(v) }
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
        mix = mixFader.value,
        filter = activeFilter, assignX = assignXIdx, assignY = assignYIdx,
        punch = punchLatched.copyOf(), punchCol2 = punchCol,
        axMulti = if (axisMulti[0].isEmpty()) null else ArrayList(axisMulti[0]),
        ayMulti = if (axisMulti[1].isEmpty()) null else ArrayList(axisMulti[1]),
        axCurve = if (axisMulti[0].isEmpty()) null else axisCurve[0].copyOf(),
        ayCurve = if (axisMulti[1].isEmpty()) null else axisCurve[1].copyOf(),
        masterFxList = if (masterFx.isEmpty()) null else ArrayList(masterFx)
    )

    private fun beginSnapHold() {
        if (snapHold) return
        snapHold = true
        snapHoldOld.clear()
        snapHoldFx.clear()
        snapHoldOld["ax"] = pad.ax
        snapHoldOld["ay"] = pad.ay
        snapHoldOld["bx"] = pad.bx
        snapHoldOld["by"] = pad.by
        snapHoldOld["mix"] = mixFader.value
        snapHoldOld["filter"] = activeFilter.toFloat()
        snapHoldOld["axi"] = assignXIdx.toFloat()
        snapHoldOld["ayi"] = assignYIdx.toFloat()
    }

    private fun endSnapHold() {
        if (!snapHold) return
        snapHold = false
        snapHoldOld["ax"]?.let { pad.ax = it }
        snapHoldOld["ay"]?.let { pad.ay = it }
        snapHoldOld["bx"]?.let { pad.bx = it }
        snapHoldOld["by"]?.let { pad.by = it }
        padAxisX = pad.ax
        pad.invalidate()
        snapHoldOld["mix"]?.let {
            mixFader.value = it
            mixFader.invalidate()
        }
        snapHoldOld["filter"]?.let {
            activeFilter = it.toInt()
            refreshFxHighlight()
        }
        snapHoldOld["axi"]?.let {
            assignXIdx = it.toInt()
            refreshAssignLabels()
        }
        snapHoldOld["ayi"]?.let {
            assignYIdx = it.toInt()
            refreshAssignLabels()
        }
        for ((fx, arr) in snapHoldFx) {
            fxParamVals[fx] = arr
            val pv = paramView
            val defs = fxParamDefs[fx]
            if (paramFx == fx && pv != null && defs != null && pv.visibility == View.VISIBLE) {
                pv.setContent(defs, fxParamVals[fx], fxParamAssign[fx])
            }
        }
        snapHoldFx.clear()
        snapHoldOld.clear()
    }

    private fun captureSnap(side: Int, idx: Int) {
        if (idx !in snapPads.indices) return
        snapPads[idx] = liveSnap()
        if (side == 0) activeZu = idx else activeLu = idx
        refreshSnapGrid()
        applyMorph(zuLuFader.value)
        Toast.makeText(
            this,
            "SNAPSHOT " + (if (side == 0) "ZU " else "LU ") + (idx + 1) + " CAPTUR\u00c9",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun recallSnap(idx: Int) {
        val n = min(padGrid.padCount, 14)
        val isZu = idx < n / 2
        if (motionPlaying == idx) {
            stopMotionPlayback()
            return
        }
        if (isZu) activeZu = idx else activeLu = idx
        val s = snapPads[idx]
        refreshSnapGrid()
        applyMorph(zuLuFader.value)
        if (s != null) {
            applyRouting(s)
            applyPunch(s)
            if ((s.motA != null && s.motA!!.size >= 4) || (s.motB != null && s.motB!!.size >= 4)) {
                startMotionPlayback(idx)
            }
        }
    }

    private fun applyPunch(s: Snap) {
        val pch = s.punch ?: return
        val col = s.punchCol2.coerceIn(0, punchLabels.size - 1)
        for (z in 0 until 9) {
            if (z < pch.size && punchLatched[z] != pch[z]) {
                val saved = punchCol
                punchCol = col
                punchApply(z, pch[z])
                punchCol = saved
            }
        }
    }

    private fun applyRouting(s: Snap) {
        val am = s.axMulti
        if (am != null) {
            s.axCurve?.let { axisCurve[0] = it.copyOf() }
            axisMulti[0].clear()
            axisMulti[0].addAll(am)
            pushAxisMulti(0)
        }
        val ay = s.ayMulti
        if (ay != null) {
            s.ayCurve?.let { axisCurve[1] = it.copyOf() }
            axisMulti[1].clear()
            axisMulti[1].addAll(ay)
            pushAxisMulti(1)
        }
        s.masterFxList?.let {
            masterFx.clear()
            masterFx.addAll(it)
        }
    }

    private fun refreshSceneSide() {
        btnZu.setTextColor(if (sceneSide == 0) turquoise else silk)
        btnLu.setTextColor(if (sceneSide == 1) violet else silk)
    }

    private fun setVerticalUi(v: Boolean) {
        verticalUi = v
        getSharedPreferences("zulu", MODE_PRIVATE).edit().putBoolean("verticalUi", v).apply()
        requestedOrientation =
            if (v) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        recreate()
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
        engine.nativeSetAx(ax)
        engine.nativeSetAy(ay)
        engine.nativeSetBx(bx)
        engine.nativeSetBy(by)
        engine.nativeSetMix(mx)
        pad.setCursor(ax, ay, bx, by)
        mixFader.value = mx
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
        title.text = "MENU \u00b7 v${versionLabel()}"
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
        val bPunch = makeButton("PUNCH 9", silk)
        bPunch.setOnClickListener {
            setPunchMode(!punchMode)
            menuOverlay.visibility = View.GONE
        }
        menuPunchBtn = bPunch
        liveRow.addView(bScale, wrapParams((4 * d).toInt()))
        liveRow.addView(bMirror, wrapParams((4 * d).toInt()))
        liveRow.addView(bPunch, wrapParams((4 * d).toInt()))
        panel.addView(liveRow)

        header("PUNCH 9 \u00b7 MAINTENIR = EFFET \u00b7 SHIFT+TAP = SORTIR")

        header("PADS")
        val padsRow = LinearLayout(this)
        for (n in listOf(8, 10, 12, 14)) {
            val b = makeButton("$n", silk)
            b.setOnClickListener { applyPadCount(n); refreshMenu() }
            menuPadButtons.add(b)
            padsRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(padsRow)

        header("TEMPS LOOP (REC 1-19)")
        val loopRow = LinearLayout(this)
        val bHalf = makeButton("\u00f72", silk)
        bHalf.setOnClickListener { loopMul(0); refreshMenu() }
        val bDbl = makeButton("\u00d72", silk)
        bDbl.setOnClickListener { loopMul(1); refreshMenu() }
        val bLoopLbl = makeButton("8", silk)
        bLoopLbl.setOnClickListener {
            loopLenSteps = 8
            padGrid.activeBank = 8
            refreshMenu()
        }
        menuLoopLbl = bLoopLbl
        loopRow.addView(bHalf, wrapParams((4 * d).toInt()))
        loopRow.addView(bLoopLbl, wrapParams((4 * d).toInt()))
        loopRow.addView(bDbl, wrapParams((4 * d).toInt()))
        panel.addView(loopRow)

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

        header("STRETCH SAMPLES \u00b7 B" + (playerBank + 1))
        val stRow = LinearLayout(this)
        for ((i, lbl) in listOf("OFF", "CLEAN", "BEAT", "RETRO").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                bankStretch[playerBank] = i
                engine.nativeSetStretchMode(playerBank, i)
                refreshMenu()
            }
            menuStretchButtons.add(b)
            stRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(stRow)

        header("SC\u00c8NES")
        val scRow = LinearLayout(this)
        val bSnap = makeButton("SNAPSHOT", amber)
        bSnap.setOnClickListener {
            sceneMode = 0
            padGrid.padMode = 0
            refreshMenu()
        }
        val bMotion = makeButton("MOTION", turquoise)
        bMotion.setOnClickListener {
            sceneMode = 1
            padGrid.padMode = 1
            refreshMenu()
        }
        bSnapBtn = bSnap
        bMotionBtn = bMotion
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

        header("FOND VIVANT (BATTERIE)")
        val artRow = LinearLayout(this)
        for ((i, lbl) in listOf("AUCUN", "LENT", "NORM", "RAPIDE", "MANDALA").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                bgArtOpt = i
                pad.bgArt = i
                pad.invalidate()
                refreshMenu()
            }
            menuArtButtons.add(b)
            artRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(artRow)

        header("STYLE")
        val styleRow = LinearLayout(this)
        for ((i, lbl) in listOf("84", "RETRO").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                ZuluTheme.retro = i == 1
                pad.invalidate()
                refreshMenu()
            }
            menuStyleButtons.add(b)
            styleRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(styleRow)

        header("MODE ECO (BATTERIE)")
        val ecoRow = LinearLayout(this)
        for ((i, lbl) in listOf("OFF", "ON").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                ecoOpt = i == 1
                engine.nativeSetEco(ecoOpt)
                if (ecoOpt && pad.bgArt != 0) {
                    bgArtEcoPrev = pad.bgArt
                    pad.bgArt = 0
                    bgArtOpt = 0
                } else if (!ecoOpt && bgArtEcoPrev > 0) {
                    pad.bgArt = bgArtEcoPrev
                    bgArtOpt = bgArtEcoPrev
                    bgArtEcoPrev = 0
                }
                pad.invalidate()
                refreshMenu()
            }
            menuEcoButtons.add(b)
            ecoRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(ecoRow)

        header("DRUM PADS (NOTES = 32 = 2 OCTAVES)")
        val drumRow = LinearLayout(this)
        val drumCounts = listOf(8, 12, 16, 32)
        for ((i, lbl) in listOf("8", "12", "16", "32").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                drumGrid.padCount = drumCounts[i]
                refreshMenu()
            }
            menuDrumButtons.add(b)
            drumRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(drumRow)

        header("REC ENGINES (DRUMS/SYNTH)")
        val midiRow = LinearLayout(this)
        for ((i, lbl) in listOf("MIDI", "AUDIO").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                recMidiEngines = i == 0
                refreshMenu()
            }
            menuMidiButtons.add(b)
            midiRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(midiRow)

        header("ENTREE (LINE-IN)")
        val inRow = LinearLayout(this)
        for ((i, lbl) in listOf("TRANSP", "INSTRU", "VOIX", "BBOX").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                inProfileSel = i
                engine.nativeSetInProfile(i)
                refreshMenu()
            }
            menuInButtons.add(b)
            inRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(inRow)

        header("EQUALISEUR (ENTREE + SORTIE)")
        val eqRow = LinearLayout(this)
        for ((i, lbl) in listOf("OFF", "ON").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                masterEqOn = i == 1
                engine.nativeSetMasterEq(masterEqOn)
                refreshMenu()
            }
            menuEqButtons.add(b)
            eqRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(eqRow)

        header("BOOST")
        val biRow = LinearLayout(this)
        for ((i, lbl) in listOf("IN x1", "IN x2", "IN x4").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                inBoostSel = i
                engine.nativeSetInBoost(if (i == 0) 1f else if (i == 1) 2f else 4f)
                refreshMenu()
            }
            menuBoostInButtons.add(b)
            biRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(biRow)
        val boRow = LinearLayout(this)
        for ((i, lbl) in listOf("OUT x1", "OUT x2", "OUT x3").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                outBoostSel = i
                engine.nativeSetOutBoost(if (i == 0) 1f else if (i == 1) 2f else 3f)
                refreshMenu()
            }
            menuBoostOutButtons.add(b)
            boRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(boRow)

        header("LOUDNESS (SORTIE)")
        val ldRow = LinearLayout(this)
        for ((i, lbl) in listOf("OFF", "ON").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                outLoudOn = i == 1
                engine.nativeSetOutLoud(outLoudOn)
                refreshMenu()
            }
            menuLoudButtons.add(b)
            ldRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(ldRow)

        header("RESEAU (FX QUI SE PARLENT)")
        val netRow = LinearLayout(this)
        for ((i, lbl) in listOf(
            "OFF", "MYCELIUM", "HUMUS", "NET VINTAGE",
            "CHAOS", "OISEAUX", "INSECTES", "MAREE"
        ).withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                netModeSel = i
                engine.nativeSetNetworkMode(i)
                refreshMenu()
            }
            menuNetButtons.add(b)
            netRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(netRow)

        header("RESEAU : REGLAGES")
        val nchRow = LinearLayout(this)
        for ((i, lbl) in listOf("CHAOS DOUX", "CHAOS MOYEN", "CHAOS FORT").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                netChaosSel = i
                pushNetParams()
                refreshMenu()
            }
            menuNetChaosButtons.add(b)
            nchRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(nchRow)
        val nmdRow = LinearLayout(this)
        for ((i, lbl) in listOf("MOD LEGER", "MOD MOYEN", "MOD FORT").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                netModSel = i
                pushNetParams()
                refreshMenu()
            }
            menuNetModButtons.add(b)
            nmdRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(nmdRow)
        val nalRow = LinearLayout(this)
        for ((i, lbl) in listOf("ALIAS OFF", "ALIAS LEGER", "ALIAS FORT").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                netAliasSel = i
                pushNetParams()
                refreshMenu()
            }
            menuNetAliasButtons.add(b)
            nalRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(nalRow)
        val nspRow = LinearLayout(this)
        for ((i, lbl) in listOf("VITESSE LENTE", "VITESSE NORMALE", "VITESSE RAPIDE", "VITESSE FRENETIQUE").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                netSpeedSel = i
                pushNetParams()
                refreshMenu()
            }
            menuNetSpeedButtons.add(b)
            nspRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(nspRow)

        header("TEMPLATE DE BASE (AU DEMARRAGE)")
        val tplRow = LinearLayout(this)
        val tS = makeButton("SAUVER L'ETAT ACTUEL", silk)
        tS.setOnClickListener {
            saveProject(0, global = true)
            Toast.makeText(this, "TEMPLATE DE BASE SAUVEGARDE", Toast.LENGTH_SHORT).show()
        }
        tplRow.addView(tS, wrapParams((4 * d).toInt()))
        val tE = makeButton("EFFACER", silk)
        tE.setOnClickListener {
            globalFile(0).delete()
            for (b in 0 until 8) bankFile(0, b, true).delete()
            Toast.makeText(this, "TEMPLATE EFFACE", Toast.LENGTH_SHORT).show()
        }
        tplRow.addView(tE, wrapParams((4 * d).toInt()))
        panel.addView(tplRow)

        header("PANIQUE")
        val panRow = LinearLayout(this)
        val pB = makeButton("TOUT REMETRE A ZERO", red)
        pB.setOnClickListener { panicReset() }
        panRow.addView(pB, wrapParams((4 * d).toInt()))
        panel.addView(panRow)

        header("LOGS SAMPLES")
        val logRow = LinearLayout(this)
        for ((i, lbl) in listOf("OFF", "ON").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                showSampleLog = i == 1
                getSharedPreferences("zulu", MODE_PRIVATE).edit().putBoolean("sampleLog", showSampleLog).apply()
                sampleLogView?.visibility = if (showSampleLog) View.VISIBLE else View.GONE
                refreshMenu()
            }
            menuLogButtons.add(b)
            logRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(logRow)

        header("NOMS DES SAMPLES (BANQUES 1-8)")
        val nmRow = LinearLayout(this)
        for (b in 1..8) {
            val bt = makeButton("B$b", silk)
            bt.setOnClickListener { askSampleName(b - 1) }
            menuNameButtons.add(bt)
            nmRow.addView(bt, wrapParams((4 * d).toInt()))
        }
        panel.addView(nmRow)

        header("ORIENTATION (REDEMARRE)")
        val orientRow = LinearLayout(this)
        for ((i, lbl) in listOf("HORIZ", "VERT").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener { setVerticalUi(i == 1) }
            menuOrientButtons.add(b)
            orientRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(orientRow)

        header("SNAPSHOT")
        val snapRow = LinearLayout(this)
        for ((i, lbl) in listOf("TRANSPARENT", "DIRECT").withIndex()) {
            val b = makeButton(lbl, silk)
            b.setOnClickListener {
                snapTransp = i == 0
                getSharedPreferences("zulu", MODE_PRIVATE).edit().putBoolean("snapTransp", snapTransp).apply()
                refreshMenu()
            }
            menuSnapButtons.add(b)
            snapRow.addView(b, wrapParams((4 * d).toInt()))
        }
        panel.addView(snapRow)

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

    private val punchLabels = listOf(
        listOf("REV", "DLY", "ECH", "RPT", "STP", "FRZ", "SCR", "TDO", "CRS"),
        listOf("RES", "RNG", "GRN", "JGL", "WRP", "FLD", "GAT", "TDO", "WOB"),
        listOf("ECH", "DUB", "SPG", "STP", "LPF", "WOB", "TRM", "PAN", "DRV"),
        listOf("CMB", "DRV", "BIT", "FLD", "GAT", "PMP", "RNG", "TDO", "JGL"),
        listOf("LOF", "CMP", "REV", "SCR", "STP", "GRN", "CHO", "GAT", "WET"),
        listOf("MEG", "DRV", "SPG", "K7", "CRK", "GRN", "STP", "TRM", "BIT"),
        listOf("FRZ", "GRN", "REV", "SWL", "TRS", "CHO", "FLG", "LPF", "WET"),
        listOf("K7", "CMP", "CHO", "TRM", "REV", "STP", "GAT", "BIT", "WET"),
        listOf("PHS", "FLG", "PMP", "GAT", "PAN", "TLK", "DRV", "DLY", "LPF"),
        listOf("MEG", "K7", "SPG", "FRZ", "GRN", "CRK", "TRS", "DRV", "WET")
    )
    private val punchFx = arrayOf(
        intArrayOf(-1, 0, 1, 13, 12, 10, 18, 23, -1),
        intArrayOf(15, 7, 19, 20, 30, 17, 9, 23, -1),
        intArrayOf(1, 27, 16, 12, -1, -1, 3, 4, -1),
        intArrayOf(22, -1, -1, 17, 9, 26, 7, 23, 20),
        intArrayOf(8, 11, 14, 18, 12, 19, 6, 9, -1),
        intArrayOf(28, -1, 16, 29, 17, 19, 12, 3, -1),
        intArrayOf(10, 19, 14, 25, 21, 6, 5, -1, -1),
        intArrayOf(29, 11, 6, 3, 14, 12, 9, -1, -1),
        intArrayOf(2, 5, 26, 9, 4, 31, -1, 0, -1),
        intArrayOf(28, 29, 16, 10, 19, 17, 21, -1, -1)
    )
    private val punchParam = arrayOf(
        intArrayOf(2, -1, -1, -1, -1, -1, -1, -1, 3),
        intArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, 1),
        intArrayOf(-1, -1, -1, -1, 0, 1, -1, -1, 7),
        intArrayOf(-1, 7, 3, -1, -1, -1, -1, -1, -1),
        intArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, 2),
        intArrayOf(-1, 7, -1, -1, -1, -1, -1, -1, 3),
        intArrayOf(-1, -1, -1, -1, -1, -1, -1, 0, 2),
        intArrayOf(-1, -1, -1, -1, -1, -1, -1, 3, 2),
        intArrayOf(-1, -1, -1, -1, -1, -1, 7, -1, 0),
        intArrayOf(-1, -1, -1, -1, -1, -1, -1, 7, 2)
    )
    private val punchVal = arrayOf(
        floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.75f),
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f),
        floatArrayOf(0f, 0f, 0f, 0f, 0.2f, 1f, 0f, 0f, 0.85f),
        floatArrayOf(0f, 1f, 0.9f, 0f, 0f, 0f, 0f, 0f, 0f),
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.85f),
        floatArrayOf(0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0.85f),
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.3f, 1f),
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.7f, 0.75f),
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0.8f, 0f, 0.85f),
        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f)
    )

    private fun assignBankFx(bank: Int) {
        val m = bankFxMask[bank]
        if (m.contentEquals(fxActive)) {
            m.fill(false)
            Toast.makeText(this, "BANQUE ${bank + 1} : SANS FX", Toast.LENGTH_SHORT).show()
        } else {
            fxActive.copyInto(m)
            Toast.makeText(this, "FX \u2192 BANQUE ${bank + 1}", Toast.LENGTH_SHORT).show()
        }
        if (bankOn[bank]) refreshBankFx()
    }

    private fun applyBankFx(b: Int) {
        if (b >= 0) {
            val mask = bankFxMask[b]
            if (!rackBackedUp) {
                fxActive.copyInto(rackBackup)
                rackBackedUp = true
            }
            for (f in 0 until 46) {
                if (masterFx.contains(f)) continue
                if (fxActive[f] != mask[f]) {
                    fxActive[f] = mask[f]
                    engine.nativeSetFx(f, fxActive[f])
                }
            }
        } else if (rackBackedUp) {
            for (f in 0 until 46) {
                if (masterFx.contains(f)) continue
                if (fxActive[f] != rackBackup[f]) {
                    fxActive[f] = rackBackup[f]
                    engine.nativeSetFx(f, fxActive[f])
                }
            }
            rackBackedUp = false
        }
        refreshFxHighlight()
    }

    private fun refreshBankFx() {
        val active = (0 until 8).lastOrNull { bankOn[it] && bankFxMask[it].any { v -> v } }
        applyBankFx(active ?: -1)
    }

    private fun presetTap(i: Int) {
        if (heldBankPad in 1..8) {
            assignBankFx(heldBankPad - 1)
            return
        }
        if (assignArm >= 0) {
            toggleAxisBank(assignArm, i)
            return
        }
        if (punchMode) {
            setPunchCollection(i)
            return
        }
        if (paramView?.visibility == View.VISIBLE) {
            val now = System.currentTimeMillis()
            val second = lastEditBank == i && now - lastEditAt < 450
            lastEditBank = i
            lastEditAt = now
            val list = userPresets[i]?.fxOn
                ?.withIndex()?.filter { it.value }?.map { it.index }
                ?: emptyList()
            if (list.isEmpty()) {
                Toast.makeText(this, "BANQUE VIDE", Toast.LENGTH_SHORT).show()
                return
            }
            openParamPage(if (second) list.getOrElse(1) { list[0] } else list[0])
            return
        }
        if (fxHeld) {
            fxConsumed = true
            saveUserPreset(i)
            Toast.makeText(this, "PRESET ${i + 1} SAUV\u00c9", Toast.LENGTH_SHORT).show()
        } else {
            val p = userPresets[i]
            if (p != null) {
                applyUserPreset(p)
            } else {
                activeFilter = i % 4
                engine.nativeSetFilterType(activeFilter)
            }
            pad.activePreset = i
        }
        pad.invalidate()
        refreshPresetRow()
    }

    private fun presetLongTap(i: Int) {
        saveUserPreset(i)
        refreshPresetRow()
    }

    private fun lenDigit(n: Int) {
        if (lenFirst == 0) {
            if (n == 1) {
                lenFirst = 1
                lenHandler.postDelayed({ if (lenEntry && lenFirst == 1) commitLen(1) }, 2000)
                Toast.makeText(this, "1_ ? (2e chiffre)", Toast.LENGTH_SHORT).show()
            } else {
                commitLen(n)
            }
        } else {
            commitLen(10 + n)
        }
    }

    private fun commitLen(v: Int) {
        lenHandler.removeCallbacksAndMessages(null)
        lenEntry = false
        lenFirst = 0
        loopLenSteps = v.coerceIn(1, 19)
        padGrid.activeBank = loopLenSteps
        Toast.makeText(this, "LONGUEUR : " + loopLenSteps + " TEMPS", Toast.LENGTH_SHORT).show()
        refreshBlink()
    }

    private fun showVuOverlay(t: String) {
        vuMeter.overlayText = t
        vuOverlayHandler.removeCallbacksAndMessages(null)
        vuOverlayHandler.postDelayed({ vuMeter.overlayText = null }, 900)
    }

    private fun arpTap() {
        if (shiftActive()) {
            if (toolPage == 2) {
                drumGrid.drumMode = (drumGrid.drumMode + 1) % 3
                drumGrid.invalidate()
            } else {
                toolPage = 2
                showToolPage()
            }
        } else {
            toolPage = (toolPage + 1) % 3
            showToolPage()
        }
        refreshPresetRow()
    }

    private fun showToolPage() {
        arpOverlay.visibility = if (toolPage == 1) View.VISIBLE else View.GONE
        drumGrid.visibility = if (toolPage == 2) View.VISIBLE else View.GONE
    }

    private fun refreshPresetRow() {
        for (i in 0 until 9) {
            val b = presetBtns.getOrNull(i) ?: continue
            b.setTextColor(
                when {
                    punchMode && punchCol == i -> amber
                    userPresets[i] != null -> cream
                    pad.activePreset == i -> amber
                    else -> silk
                }
            )
        }
        arpBtn?.setTextColor(if (arpOn) amber else silk)
        btnPunch?.setTextColor(if (punchMode) amber else yellow)
    }

    private fun openParamPage(fx: Int) {
        val pv = paramView ?: return
        val defs = fxParamDefs[fx] ?: return
        paramFx = fx
        if (!fxParamInit[fx]) {
            fxParamDefaults[fx]?.copyInto(fxParamVals[fx])
            fxParamInit[fx] = true
        }
        for (k in 0 until 8) engine.nativeSetFxParam(fx, k, fxParamVals[fx][k])
        pv.setContent(defs, fxParamVals[fx], fxParamAssign[fx])
        pv.masterOn = masterFx.contains(fx)
        pv.snapMode = when (snapFxOverride[fx]) { null -> 0; true -> 1; else -> 2 }
        pv.visibility = View.VISIBLE
    }

    private fun closeParamPage() {
        paramView?.visibility = View.GONE
        paramFx = -1
    }

    private fun armAssign(axis: Int) {
        assignArm = axis
        Toast.makeText(
            this,
            "AXE " + (if (axis == 0) "F" else "Z") + " : CHOISIR BANQUES (SHIFT+BANQUE = COURBE, FX = SORTIR)",
            Toast.LENGTH_LONG
        ).show()
        startAssignBlink()
    }

    private fun exitAssignMode() {
        if (assignArm < 0) return
        assignArm = -1
        stopAssignBlink()
    }

    private val assignBlinkRunnable = object : Runnable {
        override fun run() {
            if (assignArm < 0) return
            assignBlinkOn = !assignBlinkOn
            val b = if (assignArm == 0) btnF else btnZ
            b.setTextColor(if (assignBlinkOn) (if (assignArm == 0) yellow else pink) else silk)
            motionHandler.postDelayed(this, 280)
        }
    }

    private fun startAssignBlink() {
        motionHandler.removeCallbacks(assignBlinkRunnable)
        motionHandler.post(assignBlinkRunnable)
    }

    private fun stopAssignBlink() {
        motionHandler.removeCallbacks(assignBlinkRunnable)
        assignBlinkOn = false
        updateAxisButtons()
    }

    private fun pushAxisMulti(axis: Int) {
        val list = axisMulti[axis]
        val n = min(list.size, 8)
        val idxs = FloatArray(n)
        val modes = FloatArray(n)
        for (i in 0 until n) {
            idxs[i] = list[i].toFloat()
            modes[i] = axisCurve[axis][((list[i] - 8) / 8).coerceIn(0, 9)].toFloat()
        }
        engine.nativeSetAxisTargets(axis, idxs, modes)
    }

    private fun toggleAxisBank(axis: Int, bank: Int) {
        val an = if (axis == 0) "F" else "Z"
        if (shiftActive()) {
            axisCurve[axis][bank] = (axisCurve[axis][bank] + 1) % 4
            Toast.makeText(this, "BANQUE ${bank + 1} \u2192 $an : " + curveNames[axisCurve[axis][bank]], Toast.LENGTH_SHORT).show()
            pushAxisMulti(axis)
            return
        }
        val p = userPresets[bank]
        if (p == null) {
            Toast.makeText(this, "BANQUE ${bank + 1} VIDE", Toast.LENGTH_SHORT).show()
            return
        }
        val idxs = ArrayList<Int>()
        for (f in 0 until 46) {
            if (!p.fxOn[f]) continue
            for (k in 0 until 8) if (fxParamAssign[f][k] == 1) idxs.add(8 + f * 8 + k)
        }
        if (idxs.isEmpty()) {
            Toast.makeText(this, "AUCUN PARAM XY DANS LA BANQUE ${bank + 1}", Toast.LENGTH_SHORT).show()
            return
        }
        val cur = axisMulti[axis]
        val had = idxs.all { cur.contains(it) }
        if (had) {
            cur.removeAll(idxs)
            Toast.makeText(this, "BANQUE ${bank + 1} \u2192 $an OFF", Toast.LENGTH_SHORT).show()
        } else {
            for (t in idxs) if (!cur.contains(t) && cur.size < 8) cur.add(t)
            if (cur.size >= 8) Toast.makeText(this, "MAX 8 CIBLES PAR AXE", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "BANQUE ${bank + 1} \u2192 $an ON (" + cur.size + ")", Toast.LENGTH_SHORT).show()
        }
        pushAxisMulti(axis)
    }

    private fun cycleParamAssign(k: Int) {
        val fx = paramFx
        if (fx < 0) return
        val prev = fxParamAssign[fx][k]
        val next = (prev + 1) % 4
        fxParamAssign[fx][k] = next
        val target = 8 + fx * 8 + k
        if (prev == 1) engine.nativeSetAssign(0, assignXIdx)
        if (prev == 2 || prev == 3) {
            val l = prev - 2
            if (lfoTargets[l] == target) {
                lfoTargets[l] = -1
                pushLfo(l)
            }
        }
        when (next) {
            1 -> engine.nativeSetAssign(0, target)
            2, 3 -> {
                val l = next - 2
                lfoTargets[l] = target
                pushLfo(l)
            }
        }
        paramView?.refresh()
    }

    private fun applySendCombo(bank: Int, mode: Int) {
        if (bank !in 0..7) return
        val both = if (mode == 1) zHeld else fHeld
        val target = if (both) !sendComboStart else (mode == 1)
        bankFxSend[bank] = target
        engine.nativeSetBankFxSend(bank, target)
        engine.nativeCancelRec()
        engine.nativeUndo()
        sendPadBank = -1
        Toast.makeText(
            this,
            "FX SEND B${bank + 1} " + (if (target) "ON" else "OFF"),
            Toast.LENGTH_SHORT
        ).show()
    }

    private val punchCombo = mapOf(0 to intArrayOf(8, 14))

    private fun punchApply(zone: Int, on: Boolean) {
        val fx = punchFx[punchCol][zone]
        val pr = punchParam[punchCol][zone]
        if (!(snapHold && snapTransp)) {
            if (fx >= 0) engine.nativeSetFx(fx, on)
            else if (pr >= 0) engine.nativeSetParam(pr, punchVal[punchCol][zone], on)
            val combo = punchCombo[punchCol * 9 + zone]
            if (combo != null) {
                for (cfx in combo) engine.nativeSetFx(cfx, on)
                if (on) {
                    engine.nativeSetFilterType(1)
                    engine.nativeSetParam(0, 0.22f, true)
                } else {
                    engine.nativeSetFilterType(activeFilter)
                    engine.nativeSetParam(0, 0f, false)
                }
            }
        }
        punchLatched[zone] = on
        punchView?.setActive(zone, on)
    }

    private fun setPunchCollection(c: Int) {
        punchView?.releaseAll()
        punchCol = c
        punchView?.setCollectionIndex(c)
        punchView?.setLabels(punchLabels[c])
        for (z in 0 until 9) if (punchLatched[z]) punchApply(z, true)
        refreshPresetRow()
    }

    private fun setPunchMode(on: Boolean) {
        punchMode = on
        if (!on) {
            punchView?.releaseAll()
            for (z in 0 until 9) if (punchLatched[z]) punchApply(z, true)
        }
        punchView?.visibility = if (on) View.VISIBLE else View.GONE
        menuPunchBtn?.setTextColor(if (on) amber else silk)
        refreshPresetRow()
    }

    private fun snapQuietFor(fx: Int): Boolean = snapFxOverride[fx] ?: snapTransp

    private fun pushNetParams() {
        val chaos = floatArrayOf(0.25f, 0.5f, 0.9f)[netChaosSel]
        val mod = floatArrayOf(0.25f, 0.5f, 0.85f)[netModSel]
        val alias = floatArrayOf(0f, 0.4f, 0.9f)[netAliasSel]
        val speed = floatArrayOf(0.15f, 0.5f, 0.8f, 1f)[netSpeedSel]
        engine.nativeSetNetworkParams(chaos, mod, alias, speed)
    }

    private fun loopMul(dir: Int) {
        loopLenSteps =
            if (dir == 1) (loopLenSteps * 2).coerceAtMost(19)
            else (loopLenSteps / 2).coerceAtLeast(1)
        padGrid.activeBank = loopLenSteps
        Toast.makeText(this, "LOOP : " + loopLenSteps + " TEMPS", Toast.LENGTH_SHORT).show()
    }

    private fun refreshMenu() {
        menuLoopLbl?.text = loopLenSteps.toString()
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
        bSnapBtn?.setTextColor(if (sceneMode == 0) amber else silk)
        bMotionBtn?.setTextColor(if (sceneMode == 1) turquoise else silk)
        for ((i, b) in menuBillardButtons.withIndex()) {
            b.setTextColor(if (i == pad.billardFriction) amber else silk)
        }
        for ((i, b) in menuArtButtons.withIndex()) {
            b.setTextColor(if (i == bgArtOpt) amber else silk)
        }
        for ((i, b) in menuStyleButtons.withIndex()) {
            b.setTextColor(if ((i == 1) == ZuluTheme.retro) amber else silk)
        }
        for ((i, b) in menuEcoButtons.withIndex()) {
            b.setTextColor(if ((i == 1) == ecoOpt) amber else silk)
        }
        for ((i, b) in menuLofiButtons.withIndex()) {
            b.setTextColor(if (i == recFlavor) amber else silk)
        }
        for ((i, b) in menuStretchButtons.withIndex()) {
            b.setTextColor(if (i == bankStretch[playerBank]) amber else silk)
        }
        for ((i, b) in menuDrumButtons.withIndex()) {
            b.setTextColor(if (listOf(8, 12, 16, 32)[i] == drumGrid.padCount) amber else silk)
        }
        for ((i, b) in menuMidiButtons.withIndex()) {
            b.setTextColor(if ((i == 0) == recMidiEngines) amber else silk)
        }
        for ((i, b) in menuInButtons.withIndex()) {
            b.setTextColor(if (i == inProfileSel) amber else silk)
        }
        for ((i, b) in menuEqButtons.withIndex()) {
            b.setTextColor(if ((i == 1) == masterEqOn) amber else silk)
        }
        for ((i, b) in menuBoostInButtons.withIndex()) {
            b.setTextColor(if (i == inBoostSel) amber else silk)
        }
        for ((i, b) in menuBoostOutButtons.withIndex()) {
            b.setTextColor(if (i == outBoostSel) amber else silk)
        }
        for ((i, b) in menuLoudButtons.withIndex()) {
            b.setTextColor(if ((i == 1) == outLoudOn) amber else silk)
        }
        for ((i, b) in menuNetButtons.withIndex()) {
            b.setTextColor(if (i == netModeSel) amber else silk)
        }
        for ((i, b) in menuNetChaosButtons.withIndex()) {
            b.setTextColor(if (i == netChaosSel) amber else silk)
        }
        for ((i, b) in menuNetModButtons.withIndex()) {
            b.setTextColor(if (i == netModSel) amber else silk)
        }
        for ((i, b) in menuNetAliasButtons.withIndex()) {
            b.setTextColor(if (i == netAliasSel) amber else silk)
        }
        for ((i, b) in menuNetSpeedButtons.withIndex()) {
            b.setTextColor(if (i == netSpeedSel) amber else silk)
        }
        for ((i, b) in menuLogButtons.withIndex()) {
            b.setTextColor(if ((i == 1) == showSampleLog) amber else silk)
        }
        for ((i, b) in menuOrientButtons.withIndex()) {
            b.setTextColor(if ((i == 1) == verticalUi) amber else silk)
        }
        for ((i, b) in menuSnapButtons.withIndex()) {
            b.setTextColor(if ((i == 0) == snapTransp) amber else silk)
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
