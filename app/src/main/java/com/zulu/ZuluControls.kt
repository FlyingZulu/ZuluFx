package com.zulu

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val cream = Color.rgb(236, 224, 196)
private val ink = Color.rgb(40, 38, 32)
private val needleRed = Color.rgb(200, 60, 40)
private val amber = Color.rgb(255, 190, 0)
private val silk = Color.rgb(165, 172, 185)

object ZuluTheme {
    var retro = false
}

internal fun drawSketchyLine(
    canvas: Canvas,
    paint: Paint,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    seed: Int
) {
    val rnd = java.util.Random(seed.toLong())
    for (pass in 0..1) {
        val j0 = (rnd.nextFloat() - 0.5f) * 2.2f
        val j1 = (rnd.nextFloat() - 0.5f) * 2.2f
        val j2 = (rnd.nextFloat() - 0.5f) * 2.2f
        val j3 = (rnd.nextFloat() - 0.5f) * 2.2f
        canvas.drawLine(x0 + j0, y0 + j1, x1 + j2, y1 + j3, paint)
    }
}

internal fun drawRelief(c: Canvas, l: Float, t: Float, r: Float, b: Float, d: Float) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.style = Paint.Style.STROKE
    p.strokeWidth = 1.2f * d
    p.color = Color.WHITE
    p.alpha = 16
    c.drawLine(l + 6f * d, t + 1.2f * d, r - 6f * d, t + 1.2f * d, p)
    p.color = Color.BLACK
    p.alpha = 42
    c.drawLine(l + 6f * d, b - 1.2f * d, r - 6f * d, b - 1.2f * d, p)
    p.color = Color.WHITE
    p.alpha = 8
    c.drawLine(l + 1.2f * d, t + 6f * d, l + 1.2f * d, b - 6f * d, p)
    p.color = Color.BLACK
    p.alpha = 26
    c.drawLine(r - 1.2f * d, t + 6f * d, r - 1.2f * d, b - 6f * d, p)
    p.alpha = 255
    p.style = Paint.Style.FILL
}

internal fun drawZebra(c: Canvas, w: Float, h: Float, d: Float) {
    val p = Paint()
    p.style = Paint.Style.STROKE
    if (ZuluTheme.retro) {
        p.strokeWidth = 5f * d
        p.color = Color.WHITE
        p.alpha = 9
        var x = -h
        while (x < w + h) {
            c.drawLine(x, 0f, x + h, h, p)
            x += 12f * d
        }
        p.alpha = 255
        return
    }
    p.strokeWidth = 3.5f * d
    p.color = Color.WHITE
    p.alpha = 6
    var x = -h
    var n = 0
    while (x < w + h) {
        drawSketchyLine(c, p, x, 0f, x + h, h, 9000 + n)
        x += 16f * d
        n++
    }
    p.alpha = 255
}

internal fun drawPunkCat(
    canvas: Canvas,
    paint: Paint,
    cx: Float,
    cy: Float,
    r: Float,
    d: Float
) {
    val blink = System.currentTimeMillis() % 4200 < 160
    val rnd = java.util.Random(77L)
    val ink = Color.rgb(40, 38, 32)

    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(248, 244, 238)
    paint.alpha = 235
    canvas.drawOval(cx - r * 1.15f, cy - r * 0.92f, cx + r * 1.15f, cy + r * 1.08f, paint)

    val earPath = android.graphics.Path()
    for (s in intArrayOf(-1, 1)) {
        earPath.reset()
        earPath.moveTo(cx + s * r * 0.30f, cy - r * 0.72f)
        earPath.lineTo(cx + s * r * 0.62f, cy - r * 1.42f)
        earPath.lineTo(cx + s * r * 0.95f, cy - r * 0.55f)
        earPath.close()
        canvas.drawPath(earPath, paint)
    }

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.6f * d
    paint.color = ink
    paint.alpha = 220
    for (pass in 0..1) {
        val path = android.graphics.Path()
        val segs = 26
        for (i in 0..segs) {
            val a = Math.PI * 1.05 + Math.PI * 0.9 * i / segs
            val jx = (rnd.nextFloat() - 0.5f) * 1.4f * d
            val jy = (rnd.nextFloat() - 0.5f) * 1.4f * d
            val x = cx + (Math.cos(a) * r * 1.12f).toFloat() + jx
            val y = cy + (Math.sin(a) * r * 1.0f).toFloat() + jy
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }
    for (s in intArrayOf(-1, 1)) {
        val j = (rnd.nextFloat() - 0.5f) * 1.4f * d
        canvas.drawLine(cx + s * r * 0.30f, cy - r * 0.72f, cx + s * r * 0.62f, cy - r * 1.42f + j, paint)
        canvas.drawLine(cx + s * r * 0.62f, cy - r * 1.42f + j, cx + s * r * 0.95f, cy - r * 0.55f, paint)
    }

    val bowPath = android.graphics.Path()
    val bx = cx + r * 0.78f
    val by = cy - r * 0.82f
    bowPath.addCircle(bx - r * 0.22f, by, r * 0.20f, android.graphics.Path.Direction.CW)
    bowPath.addCircle(bx + r * 0.22f, by, r * 0.20f, android.graphics.Path.Direction.CW)
    bowPath.addCircle(bx, by, r * 0.11f, android.graphics.Path.Direction.CW)
    paint.style = Paint.Style.FILL
    paint.color = needleRed
    paint.alpha = 235
    canvas.drawPath(bowPath, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.2f * d
    paint.color = ink
    canvas.drawPath(bowPath, paint)

    paint.style = Paint.Style.FILL
    paint.color = ink
    paint.alpha = 230
    if (blink) {
        canvas.drawLine(cx - r * 0.44f, cy - r * 0.06f, cx - r * 0.16f, cy - r * 0.06f, paint)
        canvas.drawLine(cx + r * 0.16f, cy - r * 0.06f, cx + r * 0.44f, cy - r * 0.06f, paint)
    } else {
        canvas.drawCircle(cx - r * 0.30f, cy - r * 0.06f, r * 0.105f, paint)
        canvas.drawCircle(cx + r * 0.30f, cy - r * 0.06f, r * 0.105f, paint)
    }
    paint.color = Color.rgb(255, 150, 170)
    canvas.drawCircle(cx - r * 0.30f, cy + r * 0.16f, r * 0.10f, paint)
    canvas.drawCircle(cx + r * 0.30f, cy + r * 0.16f, r * 0.10f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f * d
    paint.color = ink
    paint.alpha = 230
    canvas.drawLine(cx - r * 0.07f, cy + r * 0.30f, cx + r * 0.07f, cy + r * 0.34f, paint)

    for (s in intArrayOf(-1, 1)) {
        for (k in 0..2) {
            val j = (rnd.nextFloat() - 0.5f) * 1.2f * d
            canvas.drawLine(
                cx + s * r * 0.42f, cy + r * (0.10f + k * 0.13f),
                cx + s * r * (1.05f + k * 0.06f), cy + r * (0.00f + k * 0.18f) + j, paint
            )
        }
    }

    paint.style = Paint.Style.FILL
    paint.color = needleRed
    paint.alpha = 255
    canvas.drawCircle(cx - r * 1.02f, cy + r * 0.42f, r * 0.09f, paint)
    canvas.drawCircle(cx + r * 1.02f, cy + r * 0.46f, r * 0.07f, paint)

    paint.color = cream
    paint.alpha = 255
    paint.style = Paint.Style.FILL
}

class PunkCatView(context: Context) : View(context) {
    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        drawPunkCat(c, paint, w / 2f, h / 2f, min(w, h) * 0.34f, d)
        if (System.currentTimeMillis() % 4200 < 200) postInvalidateOnAnimation()
    }
}

class ZebraBgDrawable : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas: Canvas) {
        val b = bounds
        canvas.drawColor(Color.rgb(16, 18, 23))
        val d = Resources.getSystem().displayMetrics.density
        drawZebra(canvas, b.width().toFloat(), b.height().toFloat(), d)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

internal fun drawScrews(
    canvas: Canvas,
    paint: Paint,
    l: Float,
    t: Float,
    r: Float,
    b: Float,
    d: Float
) {
    val inset = 7f * d
    val rad = 2.2f * d
    val pts = listOf(
        l + inset to t + inset, r - inset to t + inset,
        l + inset to b - inset, r - inset to b - inset
    )
    for ((x, y) in pts) {
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(55, 60, 72)
        canvas.drawCircle(x, y, rad, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f * d
        paint.color = Color.rgb(110, 118, 134)
        canvas.drawCircle(x, y, rad, paint)
        canvas.drawLine(x - rad * 0.7f, y - rad * 0.7f, x + rad * 0.7f, y + rad * 0.7f, paint)
        paint.style = Paint.Style.FILL
    }
}

internal fun drawSketchyRoundRect(
    canvas: Canvas,
    paint: Paint,
    l: Float,
    t: Float,
    r: Float,
    b: Float,
    rad: Float,
    seed: Int
) {
    val rnd = java.util.Random(seed.toLong())
    for (pass in 0..1) {
        val jx = (rnd.nextFloat() - 0.5f) * 2.4f
        val jy = (rnd.nextFloat() - 0.5f) * 2.4f
        canvas.drawRoundRect(l + jx, t + jy, r + jx, b + jy, rad, rad, paint)
    }
}

fun markerFont(context: Context): Typeface =
    Typeface.createFromAsset(context.assets, "fonts/PermanentMarker-Regular.ttf")

private fun Paint.fill(color: Int) {
    style = Paint.Style.FILL
    this.color = color
}

private fun Paint.stroke(color: Int, width: Float) {
    style = Paint.Style.STROKE
    this.color = color
    strokeWidth = width
}

class BarVuView(context: Context) : View(context) {

    val cmdId = "vu"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    var levelL = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }

    var levelR = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }

    var inLevelL = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }

    var inLevelR = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }

    var overlayText: String? = null
        set(v) {
            field = v
            invalidate()
        }

    var onLongPress: (() -> Unit)? = null
    var onVolDelta: ((Float) -> Unit)? = null
    var onInDelta: ((Float) -> Unit)? = null

    var pressed = false
        private set
    var onPressed: ((Boolean) -> Unit)? = null

    private var longPressPending = false
    private var lastX = 0f
    private var lastY = 0f
    private var swipeAxis = 0

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        paint.fill(Color.rgb(52, 56, 68))
        canvas.drawRoundRect(0f, 0f, w, h, 6f * d, 6f * d, paint)
        paint.stroke(Color.rgb(130, 138, 156), 1f * d)
        drawSketchyRoundRect(canvas, paint, 0.8f * d, 0.8f * d, w - 0.8f * d, h - 0.8f * d, 6f * d, 17)

        val m = 4f * d
        val top = m + 9f * d
        val barW = (w - 5f * m) / 4f
        drawBar(canvas, m, top, m + barW, h - m, levelL, false)
        drawBar(canvas, m * 2f + barW, top, m * 2f + barW * 2f, h - m, levelR, false)
        drawBar(canvas, m * 3f + barW * 2f, top, m * 3f + barW * 3f, h - m, inLevelL, true)
        drawBar(canvas, m * 4f + barW * 3f, top, w - m, h - m, inLevelR, true)

        paint.fill(Color.rgb(160, 168, 184))
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = 7f * d
        val half = m + barW
        canvas.drawText("OUT", m + half, m + 6.5f * d, paint)
        canvas.drawText("IN", w - m - half, m + 6.5f * d, paint)

        overlayText?.let {
            paint.fill(Color.rgb(16, 18, 23))
            paint.alpha = 220
            canvas.drawRoundRect(w * 0.06f, h * 0.26f, w * 0.94f, h * 0.8f, 3f * d, 3f * d, paint)
            paint.alpha = 255
            paint.fill(Color.rgb(255, 244, 130))
            paint.textSize = 10f * d
            canvas.drawText(it, w / 2f, h * 0.64f, paint)
        }
    }

    private fun drawBar(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, level: Float, isIn: Boolean) {
        val segs = 20
        val segH = (b - t) / segs
        val lit = (level * segs).toInt()
        for (i in 0 until segs) {
            paint.fill(
                when {
                    i >= lit -> if (isIn) Color.rgb(66, 72, 88) else Color.rgb(78, 84, 100)
                    i >= segs * 85 / 100 -> Color.rgb(255, 96, 70)
                    i >= segs * 65 / 100 -> Color.rgb(255, 205, 40)
                    else -> if (isIn) Color.rgb(0, 229, 255) else Color.rgb(110, 255, 120)
                }
            )
            canvas.drawRoundRect(
                l + 1f, b - (i + 1) * segH + 1f, r - 1f, b - i * segH - 0.5f,
                1.5f * d, 1.5f * d, paint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longPressPending = true
                pressed = true
                swipeAxis = 0
                lastX = event.x
                lastY = event.y
                onPressed?.invoke(true)
                postDelayed({
                    if (longPressPending) {
                        longPressPending = false
                        onLongPress?.invoke()
                    }
                }, 600)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                if (swipeAxis == 0 && (Math.abs(dx) > 10f * d || Math.abs(dy) > 10f * d)) {
                    swipeAxis = if (Math.abs(dy) > Math.abs(dx)) 1 else 2
                    longPressPending = false
                }
                if (swipeAxis == 1) onVolDelta?.invoke(-(dy / (45f * d)))
                else if (swipeAxis == 2) onInDelta?.invoke(dx / (45f * d))
                if (swipeAxis != 0) {
                    lastX = event.x
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longPressPending = false
                if (pressed) {
                    pressed = false
                    onPressed?.invoke(false)
                }
            }
        }
        return true
    }
}

class BpmLcdView(context: Context) : View(context) {

    val cmdId = "bpm"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var onBpmChange: ((Float) -> Unit)? = null

    var bpm = 120f
        set(v) {
            field = v.coerceIn(40f, 200f)
            invalidate()
        }

    var inputEnabled = true

    var pressed = false
        private set
    var onPressed: ((Boolean) -> Unit)? = null

    private val taps = ArrayList<Long>()
    private var downY = 0f
    private var downTime = 0L
    private var moved = false
    private var dragY = 0f

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        paint.fill(Color.rgb(24, 26, 32))
        canvas.drawRoundRect(0f, 0f, w, h, 4f * d, 4f * d, paint)
        paint.fill(Color.rgb(122, 138, 116))
        val m = 2.5f * d
        canvas.drawRoundRect(m, m, w - m, h - m, 2.5f * d, 2.5f * d, paint)

        paint.fill(Color.rgb(24, 30, 22))
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.MONOSPACE
        paint.isFakeBoldText = true
        paint.textSize = h * 0.62f
        canvas.drawText(bpm.toInt().toString(), w * 0.72f, h * 0.72f, paint)

        paint.textSize = h * 0.30f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("BPM", w * 0.76f, h * 0.72f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragY = event.y
                downY = event.y
                downTime = System.currentTimeMillis()
                moved = false
                pressed = true
                onPressed?.invoke(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (Math.abs(event.y - downY) > 8f * d) moved = true
                if (moved && inputEnabled) {
                    val dy = dragY - event.y
                    dragY = event.y
                    bpm += dy * 0.4f
                    onBpmChange?.invoke(bpm)
                }
            }
            MotionEvent.ACTION_UP -> {
                pressed = false
                onPressed?.invoke(false)
                val dt = System.currentTimeMillis() - downTime
                if (inputEnabled && !moved && dt < 300) {
                    val now = System.currentTimeMillis()
                    if (taps.isNotEmpty() && now - taps.last() > 2000) taps.clear()
                    taps.add(now)
                    if (taps.size >= 2) {
                        val recent = taps.takeLast(4)
                        var sum = 0L
                        for (i in 1 until recent.size) sum += recent[i] - recent[i - 1]
                        bpm = (60000.0 * (recent.size - 1) / sum).toFloat()
                        onBpmChange?.invoke(bpm)
                    }
                    if (taps.size > 8) taps.removeAt(0)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                pressed = false
                onPressed?.invoke(false)
            }
        }
        return true
    }
}

class PadGridView(context: Context) : View(context) {

    val cmdId = "pads"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    var shiftMode = false
        set(value) { field = value; invalidate() }

    var activeBank = 0

    var padCount = 8
        set(value) { field = value; invalidate() }

    var blinkOn = true
        set(value) { field = value; invalidate() }

    var recPad = -1
        set(value) { field = value; invalidate() }

    var recArmed = false
        set(value) { field = value; invalidate() }

    var soloMask = 0
        set(value) { field = value; invalidate() }

    var padMode = 0
        set(value) { field = value; invalidate() }

    var snapSides = IntArray(14)
        set(value) { field = value; invalidate() }

    var activeSnap = intArrayOf(-1, -1)
        set(value) { field = value; invalidate() }

    var onPadTouch: ((Int) -> Unit)? = null
    var onPadDown: ((Int) -> Unit)? = null
    var onPadUp: (() -> Unit)? = null
    var onPadLongTouch: ((Int) -> Unit)? = null

    private var flash = -1
    private var longPad = -1
    private var longRunnable: Runnable? = null

    private val zuColor = Color.rgb(0, 229, 255)
    private val luColor = Color.rgb(190, 120, 255)

    private val rows: Int get() = (padCount + 3) / 4

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val gap = 4f * d
        val rowsF = rows.toFloat()
        val padW = (w - 5f * gap) / 4f
        val padH = (h - (rowsF + 1f) * gap) / rowsF

        for (i in 0 until padCount) {
            val col = i % 4
            val row = i / 4
            val l = gap + col * (padW + gap)
            val t = gap + row * (padH + gap)
            val soloPad = padCount == 12 && i >= 8
            val soloOn = soloPad && (soloMask and (1 shl (i - 8))) != 0
            val lit = (shiftMode && activeBank == i + 1) || flash == i
            val rec = (shiftMode || recArmed) && recPad == i
            val side = if (i < snapSides.size) snapSides[i] else 0
            val lastRow = row == rows - 1

            paint.fill(
                when {
                    rec && blinkOn -> Color.rgb(255, 90, 70)
                    rec -> Color.rgb(120, 40, 34)
                    soloOn -> Color.rgb(0, 229, 255)
                    lit -> Color.rgb(255, 244, 130)
                    side == 1 -> Color.rgb(18, 58, 60)
                    side == 2 -> Color.rgb(56, 38, 78)
                    lastRow -> Color.rgb(198, 42, 140)
                    else -> Color.rgb(196, 108, 20)
                }
            )
            val rad = if (ZuluTheme.retro) 0f else 5f * d
            canvas.drawRoundRect(l, t, l + padW, t + padH, rad, rad, paint)
            paint.stroke(Color.rgb(75, 82, 98), 1f * d)
            drawSketchyRoundRect(canvas, paint, l, t, l + padW, t + padH, rad, 100 + i)

            if (side != 0 && !soloPad && !shiftMode) {
                val isActive = (side == 1 && activeSnap[0] == i) ||
                    (side == 2 && activeSnap[1] == i)
                paint.stroke(if (side == 1) zuColor else luColor, if (isActive) 2f * d else 1f * d)
                paint.alpha = if (isActive) 255 else 130
                canvas.drawRoundRect(l, t, l + padW, t + padH, rad, rad, paint)
                paint.alpha = 255
                if (isActive) {
                    paint.fill(if (side == 1) zuColor else luColor)
                    canvas.drawCircle(l + padW / 2f, t + padH / 2f, 3f * d, paint)
                }
            }

            if (shiftMode) {
                paint.fill(if (lit) ink else silk)
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = marker
                paint.textSize = padH * 0.5f
                canvas.drawText("${i + 1}", l + padW / 2f, t + padH * 0.7f, paint)
            } else if (soloPad) {
                paint.fill(if (soloOn) ink else silk)
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = marker
                paint.textSize = padH * 0.42f
                canvas.drawText("S", l + padW / 2f, t + padH * 0.68f, paint)
            } else {
                paint.fill(if (lit) ink else Color.rgb(40, 26, 12))
                canvas.drawCircle(l + padW / 2f, t + padH / 2f, 2.5f * d, paint)
            }
        }

        val modeCol = when {
            padMode == 2 -> Color.rgb(70, 255, 130)
            padMode == 1 -> Color.rgb(0, 240, 255)
            else -> Color.rgb(255, 190, 0)
        }
        val modeName = when {
            padMode == 2 -> "SAMPLES"
            padMode == 1 -> "MOTION"
            else -> "SC\u00c8NE"
        }
        paint.stroke(modeCol, 1.5f * d)
        paint.alpha = 150
        canvas.drawRoundRect(1f * d, 1f * d, w - 1f * d, h - 1f * d, 7f * d, 7f * d, paint)
        paint.alpha = 255

        drawScrews(canvas, paint, 0f, 0f, w, h, d)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = padAt(event.getX(event.actionIndex), event.getY(event.actionIndex))
                if (i >= 0) {
                    flash = i
                    onPadDown?.invoke(i + 1)
                    onPadTouch?.invoke(i + 1)
                    longPad = i
                    longRunnable = Runnable {
                        if (longPad == i) onPadLongTouch?.invoke(i + 1)
                    }.also { postDelayed(it, 600) }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                flash = -1
                onPadUp?.invoke()
                longRunnable?.let { removeCallbacks(it) }
                longRunnable = null
                longPad = -1
            }
        }
        invalidate()
        return true
    }

    private fun padAt(x: Float, y: Float): Int {
        val w = width.toFloat()
        val gap = 4f * d
        val rowsF = rows.toFloat()
        val padW = (w - 5f * gap) / 4f
        val padH = (height - (rowsF + 1f) * gap) / rowsF
        val col = ((x - gap) / (padW + gap)).toInt()
        val row = ((y - gap) / (padH + gap)).toInt()
        if (col !in 0..3 || row !in 0 until rows) return -1
        val i = row * 4 + col
        return if (i in 0 until padCount) i else -1
    }
}

class FlyFaderView(context: Context) : View(context) {

    val cmdId = "fly"

    val stutter: Boolean
        get() = value >= 0.75f

    var onValueChange: ((Float, Boolean) -> Unit)? = null

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    var value = 0f
        private set

    fun setValue(v: Float) {
        value = v.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val m = 8f * d

        paint.fill(Color.rgb(30, 33, 40))
        canvas.drawRoundRect(0f, 0f, w, h, 8f * d, 8f * d, paint)
        drawRelief(canvas, 0f, 0f, w, h, d)
        paint.stroke(Color.rgb(75, 82, 98), 1f * d)
        drawSketchyRoundRect(canvas, paint, 0.8f * d, 0.8f * d, w - 0.8f * d, h - 0.8f * d, 8f * d, 71)

        val cy = h / 2f
        paint.stroke(Color.rgb(18, 20, 25), 5f * d)
        canvas.drawLine(m, cy, w - m, cy, paint)
        paint.stroke(Color.rgb(60, 66, 80), 1f * d)
        canvas.drawLine(m, cy, w - m, cy, paint)

        val zoneStart = m + (w - 2f * m) * 0.75f
        paint.stroke(if (stutter) amber else Color.rgb(52, 58, 72), 1f * d)
        var hx = zoneStart
        while (hx < w - m - 2f) {
            canvas.drawLine(hx, cy - 6f * d, hx - 3f * d, cy + 6f * d, paint)
            hx += 4f * d
        }

        val kx = m + value * (w - 2f * m)
        paint.fill(amber)
        canvas.drawRoundRect(kx - 11f * d, cy - 17f * d, kx + 11f * d, cy + 17f * d, 5f * d, 5f * d, paint)
        paint.fill(Color.WHITE)
        paint.alpha = 55
        canvas.drawRect(kx - 10f * d, cy - 16f * d, kx + 10f * d, cy, paint)
        paint.fill(Color.BLACK)
        paint.alpha = 60
        canvas.drawRect(kx - 10f * d, cy, kx + 10f * d, cy + 16f * d, paint)
        paint.alpha = 255
        paint.stroke(if (stutter) Color.WHITE else silk, 1.2f * d)
        paint.alpha = if (stutter) 220 else 90
        canvas.drawRoundRect(kx - 11f * d, cy - 17f * d, kx + 11f * d, cy + 17f * d, 5f * d, 5f * d, paint)
        paint.alpha = 255
        paint.stroke(Color.rgb(40, 26, 8), 1.5f * d)
        canvas.drawLine(kx, cy - 12f * d, kx, cy + 12f * d, paint)

        paint.fill(amber)
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = marker
        paint.textSize = 9f * d
        canvas.drawText("FLY", m + 2f * d, m + 9f * d, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> {
                val w = width.toFloat()
                val m = 8f * d
                value = ((event.x - m) / (w - 2f * m)).coerceIn(0f, 1f)
                onValueChange?.invoke(value, stutter)
            }
        }
        invalidate()
        return true
    }
}

class GlitchScopeView(context: Context) : View(context) {

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)
    private val rnd = java.util.Random()
    private var phase = 0f
    private var running = false

    fun start() {
        running = true
        scaleX = 0.7f
        scaleY = 0.7f
        animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        postInvalidateOnAnimation()
    }

    fun stop() {
        running = false
        visibility = GONE
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        paint.fill(Color.rgb(12, 13, 16))
        canvas.drawRoundRect(0f, 0f, w, h, 4f * d, 4f * d, paint)
        paint.stroke(Color.rgb(75, 82, 98), 1f * d)
        drawSketchyRoundRect(canvas, paint, 0.8f * d, 0.8f * d, w - 0.8f * d, h - 0.8f * d, 4f * d, 17)

        phase += 0.11f
        paint.stroke(amber, 1.2f * d)
        val n = 36
        var px = 6f * d
        var py = h / 2f
        for (i in 1..n) {
            val x = 6f * d + (w - 12f * d) * i / n
            val y = h / 2f + sin(phase + i * 0.9f) * h * 0.22f +
                (if (rnd.nextInt(12) == 0) (rnd.nextFloat() - 0.5f) * h * 0.6f else 0f)
            canvas.drawLine(px, py, x, y, paint)
            px = x
            py = y
        }

        paint.fill(silk)
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = marker
        paint.textSize = 7f * d
        canvas.drawText("BEND", w - 5f * d, h - 4f * d, paint)

        if (running) postInvalidateOnAnimation()
    }
}

class VFaderView(
    context: Context,
    private val label: String,
    private val marks: List<Pair<Float, String>> = emptyList(),
    private val thumbColor: Int = Color.rgb(165, 172, 185),
    private val blendMorph: Boolean = false
) : View(context) {

    private val turquoise = Color.rgb(0, 240, 215)
    private val violet = Color.rgb(200, 90, 255)

    private fun currentThumbColor(): Int {
        if (!blendMorph) return thumbColor
        val t = value.coerceIn(0f, 1f)
        fun lerp(a: Int, b: Int) = (a + (b - a) * t).toInt()
        return Color.rgb(
            lerp(Color.red(turquoise), Color.red(violet)),
            lerp(Color.green(turquoise), Color.green(violet)),
            lerp(Color.blue(turquoise), Color.blue(violet))
        )
    }

    val cmdId = "vfader_$label"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    var onValueChange: ((Float) -> Unit)? = null

    var value = 0.5f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val m = 8f * d

        paint.fill(Color.rgb(30, 33, 40))
        canvas.drawRoundRect(0f, 0f, w, h, 8f * d, 8f * d, paint)
        drawRelief(canvas, 0f, 0f, w, h, d)
        paint.stroke(Color.rgb(75, 82, 98), 1f * d)
        drawSketchyRoundRect(canvas, paint, 0.8f * d, 0.8f * d, w - 0.8f * d, h - 0.8f * d, 8f * d, 73)

        val cy = h / 2f
        paint.stroke(Color.rgb(18, 20, 25), 5f * d)
        canvas.drawLine(m, cy, w - m, cy, paint)
        paint.stroke(Color.rgb(60, 66, 80), 1f * d)
        canvas.drawLine(m, cy, w - m, cy, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = 8f * d
        for ((p, txt) in marks) {
            val mx = m + p * (w - 2f * m)
            paint.stroke(Color.rgb(90, 97, 114), 1f * d)
            canvas.drawLine(mx, cy + 4f * d, mx, cy + 9f * d, paint)
            paint.fill(Color.rgb(120, 128, 144))
            canvas.drawText(txt, mx, cy + 20f * d, paint)
        }

        val kx = m + value * (w - 2f * m)
        val th = min(h * 0.85f, 56f * d)
        val tc = currentThumbColor()
        paint.fill(tc)
        canvas.drawRoundRect(kx - 11f * d, cy - th / 2f, kx + 11f * d, cy + th / 2f, 5f * d, 5f * d, paint)
        paint.fill(Color.WHITE)
        paint.alpha = 55
        canvas.drawRect(kx - 10f * d, cy - th / 2f + 1f, kx + 10f * d, cy, paint)
        paint.fill(Color.BLACK)
        paint.alpha = 60
        canvas.drawRect(kx - 10f * d, cy, kx + 10f * d, cy + th / 2f - 1f, paint)
        paint.alpha = 255
        paint.stroke(Color.WHITE, 1.2f * d)
        paint.alpha = 110
        canvas.drawRoundRect(kx - 11f * d, cy - th / 2f, kx + 11f * d, cy + th / 2f, 5f * d, 5f * d, paint)
        paint.alpha = 255
        paint.stroke(Color.rgb(20, 22, 28), 1.5f * d)
        canvas.drawLine(kx, cy - th / 2f + 4f * d, kx, cy + th / 2f - 4f * d, paint)

        paint.fill(silk)
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = marker
        paint.textSize = 9f * d
        canvas.drawText(label, m + 2f * d, m + 10f * d, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> {
                val w = width.toFloat()
                val m = 8f * d
                value = ((event.x - m) / (w - 2f * m)).coerceIn(0f, 1f)
                onValueChange?.invoke(value)
            }
        }
        invalidate()
        return true
    }
}

class StepEditorView(context: Context) : View(context) {

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    var bank = 0
    var stepCount = 8
    var pitch = FloatArray(14)
    var vel = FloatArray(14) { 1f }

    var onStepChange: ((Int, Float, Float) -> Unit)? = null

    private val pitchMax = 12f

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val m = 10f * d
        val n = stepCount.coerceIn(1, 14)
        val colW = (w - 2f * m) / n
        val split = m + (h - 2f * m) * 0.58f

        paint.fill(Color.rgb(16, 18, 24))
        canvas.drawRoundRect(0f, 0f, w, h, 6f * d, 6f * d, paint)
        paint.stroke(Color.rgb(75, 82, 98), 1f * d)
        drawSketchyRoundRect(canvas, paint, 0.8f * d, 0.8f * d, w - 0.8f * d, h - 0.8f * d, 6f * d, 23)

        paint.textSize = 8f * d
        paint.typeface = marker
        paint.textAlign = Paint.Align.LEFT
        paint.fill(Color.rgb(120, 128, 144))
        canvas.drawText("PITCH", m, m + 8f * d, paint)
        canvas.drawText("VEL", m, split + 10f * d, paint)

        for (s in 0 until n) {
            val l = m + s * colW

            val pt = m + 12f * d
            val pb = split - 4f * d
            paint.fill(Color.rgb(26, 30, 38))
            canvas.drawRect(l + 1f, pt, l + colW - 2f, pb, paint)
            val cy = (pt + pb) / 2f
            paint.stroke(Color.rgb(60, 66, 80), 1f * d)
            canvas.drawLine(l + 1f, cy, l + colW - 2f, cy, paint)
            val p = pitch[s].coerceIn(-pitchMax, pitchMax)
            val half = (pb - pt) / 2f
            val barY = cy - (p / pitchMax) * half
            paint.fill(Color.rgb(0, 229, 255))
            if (p >= 0) canvas.drawRect(l + 2f, barY, l + colW - 3f, cy, paint)
            else canvas.drawRect(l + 2f, cy, l + colW - 3f, barY, paint)
            paint.fill(Color.rgb(236, 224, 196))
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                if (p > 0.5f) "+${p.toInt()}" else "${p.toInt()}",
                l + colW / 2f, pt + 9f * d, paint
            )

            val vt = split + 14f * d
            val vb = h - m
            paint.fill(Color.rgb(26, 30, 38))
            canvas.drawRect(l + 1f, vt, l + colW - 2f, vb, paint)
            val v = vel[s].coerceIn(0f, 1f)
            val vy = vb - v * (vb - vt)
            paint.fill(Color.rgb(255, 176, 0))
            canvas.drawRect(l + 2f, vy, l + colW - 3f, vb, paint)
            paint.fill(Color.rgb(120, 128, 144))
            canvas.drawText("${(v * 99).toInt()}", l + colW / 2f, vt + 9f * d, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> {
                val w = width.toFloat()
                val h = height.toFloat()
                val m = 10f * d
                val n = stepCount.coerceIn(1, 14)
                val colW = (w - 2f * m) / n
                val split = m + (h - 2f * m) * 0.58f
                val s = (((event.x - m) / colW).toInt()).coerceIn(0, n - 1)
                if (event.y < split) {
                    val pt = m + 12f * d
                    val pb = split - 4f * d
                    val cy = (pt + pb) / 2f
                    val half = (pb - pt) / 2f
                    val p = if (half > 0f) ((cy - event.y) / half * pitchMax) else 0f
                    pitch[s] = p.coerceIn(-pitchMax, pitchMax).toInt().toFloat()
                    onStepChange?.invoke(s, pitch[s], vel[s])
                } else {
                    val vt = split + 14f * d
                    val vb = h - m
                    val v = if (vb > vt) 1f - (event.y - vt) / (vb - vt) else 1f
                    vel[s] = v.coerceIn(0f, 1f)
                    onStepChange?.invoke(s, pitch[s], vel[s])
                }
            }
        }
        invalidate()
        return true
    }
}

class LfoDrawView(context: Context) : View(context) {

    var onShapeChange: ((FloatArray) -> Unit)? = null
    private val shape = FloatArray(64) { 0.5f }
    private var lastBin = -1
    private var lastVal = 0.5f
    private val d = resources.displayMetrics.density

    private val pBg = Paint().apply { color = Color.rgb(12, 14, 18) }
    private val pGrid = Paint().apply {
        color = Color.argb(40, 110, 118, 134)
        strokeWidth = 1f * d
    }
    private val pMid = Paint().apply {
        color = Color.rgb(75, 82, 98)
        strokeWidth = 1f * d
    }
    private val pLine = Paint().apply {
        color = Color.rgb(255, 176, 64)
        strokeWidth = 2.5f * d
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val pPlay = Paint().apply {
        color = Color.argb(120, 236, 224, 196)
        strokeWidth = 1.5f * d
    }

    fun setShape(s: FloatArray) {
        for (i in 0 until 64) shape[i] = s[i].coerceIn(0f, 1f)
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val x = (e.x / width).coerceIn(0f, 1f)
                val v = 1f - (e.y / height).coerceIn(0f, 1f)
                val bin = (x * 63f).toInt().coerceIn(0, 63)
                if (lastBin >= 0 && bin != lastBin) {
                    val from = min(lastBin, bin)
                    val to = maxOf(lastBin, bin)
                    val span = (bin - lastBin).toFloat()
                    for (i in from..to) {
                        val t = if (span == 0f) 0f else (i - lastBin) / span
                        shape[i] = lastVal + (v - lastVal) * t
                    }
                } else {
                    shape[bin] = v
                }
                lastBin = bin
                lastVal = v
                onShapeChange?.invoke(shape.copyOf())
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> lastBin = -1
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawRect(0f, 0f, w, h, pBg)
        for (i in 1 until 8) c.drawLine(w * i / 8f, 0f, w * i / 8f, h, pGrid)
        c.drawLine(0f, h / 2f, w, h / 2f, pMid)
        val path = android.graphics.Path()
        for (i in 0 until 64) {
            val x = w * i / 63f
            val y = (1f - shape[i]) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        c.drawPath(path, pLine)
        val ph = (System.nanoTime() % 1_000_000_000L) / 1_000_000_000f
        c.drawLine(w * ph, 0f, w * ph, h, pPlay)
        if (isShown) postInvalidateOnAnimation()
    }
}

class PunchPadView(context: Context) : View(context) {
    private val marker = markerFont(context)
    var onPunchDown: ((Int) -> Unit)? = null
    var onPunchUp: ((Int) -> Unit)? = null
    var onExit: (() -> Unit)? = null
    var shiftCheck: (() -> Boolean)? = null

    private var labels = listOf("REV", "DLY", "ECH", "RPT", "STP", "FRZ", "SCR", "TDO", "CRS")
    private val colNames = listOf("ZULU", "APHX", "HGTN", "SKRX", "DOOM", "WAIT", "RHED", "BOC", "DAFT", "W/Y")
    private var colIdx = 0
    var onCollectionChange: ((Int) -> Unit)? = null
    private var headerH = 0f
    private val active = BooleanArray(9)
    private val pointers = android.util.SparseArray<Int>()
    private val pChip = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = silk
        typeface = marker
        textAlign = Paint.Align.CENTER
    }

    private val pBg = Paint().apply { color = Color.rgb(14, 15, 20) }
    private val pScrew = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(60, 64, 76) }
    private val pGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(75, 82, 98)
        strokeWidth = 2f
    }
    private val pLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = silk
        typeface = marker
        textAlign = Paint.Align.CENTER
    }
    private val pOnFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 176, 0)
    }
    private val pOnBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = amber
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun setActive(zone: Int, on: Boolean) {
        if (zone in 0 until 9) {
            active[zone] = on
            invalidate()
        }
    }

    fun setLabels(l: List<String>) {
        labels = l
        invalidate()
    }

    fun setCollectionIndex(i: Int) {
        colIdx = i
        invalidate()
    }

    fun releaseAll() {
        for (z in 0 until 9) {
            if (active[z]) {
                active[z] = false
                onPunchUp?.invoke(z)
            }
        }
        pointers.clear()
        invalidate()
    }

    private fun zoneOf(x: Float, y: Float): Int {
        val gy = (y - headerH).coerceIn(0f, height - headerH - 1f)
        val c = (x / (width / 3f)).toInt().coerceIn(0, 2)
        val r = (gy / ((height - headerH) / 3f)).toInt().coerceIn(0, 2)
        return r * 3 + c
    }

    private fun press(id: Int, z: Int) {
        pointers.put(id, z)
        if (!active[z]) {
            active[z] = true
            onPunchDown?.invoke(z)
        }
        invalidate()
    }

    private fun release(id: Int) {
        val z = pointers.get(id, -1)
        if (z >= 0) {
            active[z] = false
            onPunchUp?.invoke(z)
            pointers.delete(id)
            invalidate()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (shiftCheck?.invoke() == true) {
                    onExit?.invoke()
                    return true
                }
                val i = e.actionIndex
                if (e.getY(i) < headerH) {
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                        val chip = (e.getX(i) / (width / colNames.size.toFloat())).toInt()
                            .coerceIn(0, colNames.size - 1)
                        if (chip != colIdx) onCollectionChange?.invoke(chip)
                    }
                    return true
                }
                press(e.getPointerId(i), zoneOf(e.getX(i), e.getY(i)))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    val z = zoneOf(e.getX(i), e.getY(i))
                    val old = pointers.get(id, -1)
                    if (old >= 0 && old != z) {
                        release(id)
                        press(id, z)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                release(e.getPointerId(e.actionIndex))
                if (e.pointerCount <= 1) releaseAll()
            }
            MotionEvent.ACTION_POINTER_UP -> release(e.getPointerId(e.actionIndex))
            MotionEvent.ACTION_CANCEL -> releaseAll()
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        headerH = h / 7f
        c.drawRect(0f, 0f, w, h, pBg)
        drawScrews(c, pScrew, 0f, 0f, w, h, context.resources.displayMetrics.density)
        val cw = w / 3f
        val gh = h - headerH
        val ch = gh / 3f
        for (i in 1 until 3) {
            c.drawLine(cw * i, headerH, cw * i, h, pGrid)
            c.drawLine(0f, headerH + ch * i, w, h, pGrid)
        }
        c.drawLine(0f, headerH, w, headerH, pGrid)
        val chw = w / colNames.size
        for (i in colNames.indices) {
            pChip.color = if (i == colIdx) amber else Color.rgb(105, 111, 128)
            pChip.textSize = minOf(headerH * 0.45f, chw / (colNames[i].length * 0.85f))
            c.drawText(colNames[i], chw * i + chw / 2f, headerH * 0.68f, pChip)
        }
        if (colIdx in colNames.indices) {
            c.drawLine(chw * colIdx + chw * 0.15f, headerH - 4f,
                chw * (colIdx + 1) - chw * 0.15f, headerH - 4f, pOnBorder)
        }
        pLabel.textSize = ch / 3.6f
        for (z in 0 until 9) {
            val cx = cw * (z % 3) + cw / 2f
            val cy = headerH + ch * (z / 3) + ch / 2f
            if (active[z]) {
                c.drawRect(cw * (z % 3) + 3f, headerH + ch * (z / 3) + 3f,
                    cw * (z % 3 + 1) - 3f, headerH + ch * (z / 3 + 1) - 3f, pOnFill)
                c.drawRect(cw * (z % 3) + 3f, headerH + ch * (z / 3) + 3f,
                    cw * (z % 3 + 1) - 3f, headerH + ch * (z / 3 + 1) - 3f, pOnBorder)
            }
            c.drawText(labels[z], cx, cy + pLabel.textSize / 3f, pLabel)
        }
    }
}

class ParamPageView(context: Context) : View(context) {
    private val marker = markerFont(context)
    var onParam: ((Int, Float) -> Unit)? = null
    var onAssignCycle: ((Int) -> Unit)? = null
    var onExit: (() -> Unit)? = null
    var onMasterToggle: (() -> Unit)? = null
    var onSnapToggle: (() -> Unit)? = null
    var shiftCheck: (() -> Boolean)? = null
    var masterOn = false
    var snapMode = 0

    private var labels: List<String> = emptyList()
    private var values = FloatArray(0)
    private var assigns = IntArray(0)
    private var dragSlot = -1

    private val pBg = Paint().apply { color = Color.rgb(14, 15, 20) }
    private val pScrew = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(60, 64, 76) }
    private val pSlot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(40, 44, 54) }
    private val pHandle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = amber }
    private val pDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(40, 44, 54) }
    private val pBox = Paint().apply { color = Color.rgb(40, 44, 54) }
    private val pLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = silk
        typeface = marker
        textAlign = Paint.Align.CENTER
    }
    private val pLegendTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = silk
        typeface = marker
    }
    private val ledCols = intArrayOf(
        Color.rgb(0, 240, 255),
        Color.rgb(70, 255, 130),
        Color.rgb(255, 70, 205),
        amber
    )

    fun setContent(l: List<String>, v: FloatArray, a: IntArray) {
        labels = l
        values = v
        assigns = a
        invalidate()
    }

    fun refresh() {
        invalidate()
    }

    private fun setFromY(slot: Int, y: Float) {
        val top = height * 0.24f
        val bot = height * 0.78f
        val v = 1f - ((y - top) / (bot - top))
        values[slot] = v.coerceIn(0f, 1f)
        onParam?.invoke(slot, values[slot])
        invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val n = labels.size.coerceAtLeast(1)
        val dd = context.resources.displayMetrics.density
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (e.y < 19 * dd && e.x > width - 152 * dd) {
                    if (e.x > width - 70 * dd) onMasterToggle?.invoke() else onSnapToggle?.invoke()
                    return true
                }
                if (shiftCheck?.invoke() == true) {
                    if (e.y < height * 0.06f || e.y > height * 0.9f) {
                        onExit?.invoke()
                    } else {
                        val slot = (e.x * n / width).toInt().coerceIn(0, n - 1)
                        onAssignCycle?.invoke(slot)
                    }
                    return true
                }
                if (e.y < height * 0.18f) {
                    val slot = (e.x * n / width).toInt().coerceIn(0, n - 1)
                    onAssignCycle?.invoke(slot)
                    return true
                }
                dragSlot = (e.x * n / width).toInt().coerceIn(0, n - 1)
                setFromY(dragSlot, e.y)
            }
            MotionEvent.ACTION_MOVE -> if (dragSlot >= 0) setFromY(dragSlot, e.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragSlot = -1
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val d = context.resources.displayMetrics.density
        c.drawRect(0f, 0f, w, h, pBg)
        drawRelief(c, 0f, 0f, w, h, d)
        drawScrews(c, pScrew, 0f, 0f, w, h, d)
        val n = labels.size
        if (n == 0) return
        pLegendTxt.textSize = 9f * d
        val names = listOf("XY", "LFO", "ARP", "SNAP")
        var lx = 12f * d
        val ly = 9f * d
        for (j in 0 until 4) {
            pDot.color = ledCols[j]
            c.drawCircle(lx + 3.5f * d, ly, 3f * d, pDot)
            pLegendTxt.color = silk
            c.drawText(names[j], lx + 9f * d, ly + 3.2f * d, pLegendTxt)
            lx += 9f * d + pLegendTxt.measureText(names[j]) + 7f * d
        }
        val top = h * 0.24f
        val bot = h * 0.78f
        val cw = w / n
        val sw = maxOf(cw * 0.28f, 8f * d)
        val ledR = minOf(cw * 0.1f, 3.5f * d)
        val ledStep = cw * 0.24f
        for (i in 0 until n) {
            val cx = cw * i + cw / 2f
            for (j in 0 until 4) {
                val lit = when (j) {
                    0 -> assigns[i] == 1
                    1 -> assigns[i] == 2 || assigns[i] == 3
                    2 -> assigns[i] == 4
                    else -> assigns[i] == 5
                }
                pDot.color = if (lit) ledCols[j] else Color.rgb(40, 44, 54)
                c.drawCircle(cx + (j - 1.5f) * ledStep, h * 0.15f, ledR, pDot)
            }
            c.drawRect(cx - sw / 2f, top, cx + sw / 2f, bot, pSlot)
            val hy = bot - values[i] * (bot - top)
            c.drawRect(cx - sw / 2f - 4f * d, hy - 5f * d, cx + sw / 2f + 4f * d, hy + 5f * d, pHandle)
            pLabel.textSize = minOf(cw * 0.3f, 13f * d)
            c.drawText(labels[i], cx, h * 0.86f, pLabel)
        }
        val by2 = 17f * d
        val bx1 = w - 66f * d
        pBox.color = if (masterOn) Color.rgb(185, 110, 255) else Color.rgb(40, 44, 54)
        c.drawRect(bx1, 3f * d, w - 4f * d, by2, pBox)
        pLabel.textSize = 8.5f * d
        pLabel.color = if (masterOn) Color.rgb(22, 16, 34) else silk
        c.drawText("MASTER", (bx1 + w - 4f * d) / 2f, by2 - 4.5f * d, pLabel)
        val sx1 = bx1 - 82f * d
        pBox.color = when (snapMode) {
            1 -> Color.rgb(0, 240, 255)
            2 -> Color.rgb(70, 255, 130)
            else -> Color.rgb(40, 44, 54)
        }
        c.drawRect(sx1, 3f * d, bx1 - 4f * d, by2, pBox)
        pLabel.color = if (snapMode > 0) Color.rgb(22, 16, 34) else silk
        c.drawText(
            if (snapMode == 1) "SNAP:MUTE" else if (snapMode == 2) "SNAP:SON" else "SNAP:AUTO",
            (sx1 + bx1 - 4f * d) / 2f, by2 - 4.5f * d, pLabel
        )
        pLabel.color = silk
    }
}

class LineInKnob(context: Context) : View(context) {

    val cmdId = "in"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    var value = 1f
        private set
    var onValueChange: ((Float) -> Unit)? = null

    fun setVal(v: Float) {
        value = v.coerceIn(0f, 2f)
        invalidate()
    }

    private var dragY = 0f
    private var dragV = 0f

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.42f

        paint.fill(Color.rgb(30, 33, 40))
        canvas.drawCircle(cx, cy, r + 2.5f * d, paint)
        paint.stroke(Color.rgb(90, 98, 116), 1.2f * d)
        canvas.drawCircle(cx, cy, r + 2.5f * d, paint)

        val a = (-135f + 270f * (value / 2f)) * Math.PI.toFloat() / 180f
        paint.stroke(Color.rgb(0, 229, 255), 2f * d)
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, -135f, 270f * (value / 2f), false, paint)

        paint.fill(cream)
        canvas.drawCircle(cx, cy, r * 0.55f, paint)
        paint.stroke(Color.rgb(255, 190, 0), 1.4f * d)
        canvas.drawLine(cx, cy, cx + (r * 0.5f) * Math.cos(a.toDouble()).toFloat(),
            cy + (r * 0.5f) * Math.sin(a.toDouble()).toFloat(), paint)

        paint.fill(silk)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = 7f * d
        canvas.drawText("IN", cx, h - 1.5f * d, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragY = event.y
                dragV = value
            }
            MotionEvent.ACTION_MOVE -> {
                val dv = (dragY - event.y) / (60f * d)
                value = (dragV + dv).coerceIn(0f, 2f)
                onValueChange?.invoke(value)
                invalidate()
            }
        }
        return true
    }
}

class DrumGridView(context: Context) : View(context) {

    val cmdId = "drums"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    var padCount = 12
        set(value) { field = value; invalidate() }
    var drumMode = 0

    var onDrum: ((Int) -> Unit)? = null
    var onExit: (() -> Unit)? = null

    private var flash = -1
    private var flashAt = 0L

    private val modeNames = listOf("NOTES", "SLICE", "RND")

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val gap = 4f * d
        val top = 20f * d
        val cols = if (padCount > 16) 8 else 4
        val rows = (padCount + cols - 1) / cols
        val rowsF = rows.toFloat()
        val padW = (w - (cols + 1) * gap) / cols
        val padH = (h - top - (rowsF + 1) * gap) / rowsF
        val rad = if (ZuluTheme.retro) 0f else 5f * d

        paint.fill(Color.rgb(20, 22, 28))
        canvas.drawRect(0f, 0f, w, h, paint)
        drawZebra(canvas, w, h, d)

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        for (i in 0 until padCount) {
            val col = i % cols
            val row = i / cols
            val l = gap + col * (padW + gap)
            val t = top + gap + row * (padH + gap)
            val lit = flash == i && System.currentTimeMillis() - flashAt < 120
            paint.fill(
                when {
                    lit -> Color.rgb(255, 244, 130)
                    row == rows - 1 -> Color.rgb(198, 42, 140)
                    else -> Color.rgb(196, 108, 20)
                }
            )
            canvas.drawRoundRect(l, t, l + padW, t + padH, rad, rad, paint)
            paint.stroke(Color.rgb(75, 82, 98), 1f * d)
            drawSketchyRoundRect(canvas, paint, l, t, l + padW, t + padH, rad, 300 + i)
            paint.fill(if (lit) Color.rgb(40, 26, 12) else Color.rgb(255, 244, 130))
            paint.textSize = padH * 0.42f
            canvas.drawText("${i + 1}", l + padW / 2f, t + padH * 0.7f, paint)
        }

        paint.fill(Color.rgb(16, 18, 23))
        paint.alpha = 225
        canvas.drawRoundRect(3f * d, 2f * d, 3f * d + 240f * d, 17f * d, 4f * d, 4f * d, paint)
        paint.alpha = 255
        paint.stroke(Color.rgb(0, 255, 170), 1f * d)
        canvas.drawRoundRect(3f * d, 2f * d, 3f * d + 240f * d, 17f * d, 4f * d, 4f * d, paint)
        for ((m, name) in modeNames.withIndex()) {
            val mw = 80f * d
            val mx = 3f * d + m * mw
            paint.fill(if (drumMode == m) Color.rgb(0, 255, 170) else Color.rgb(120, 128, 146))
            paint.textSize = 9f * d
            canvas.drawText(name, mx + mw / 2f, 13f * d, paint)
        }
        paint.fill(Color.rgb(255, 96, 70))
        paint.textSize = 12f * d
        canvas.drawText("\u00d7", w - 10f * d, 14f * d, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(event.actionIndex)
                val y = event.getY(event.actionIndex)
                if (x > w - 24f * d && y < 20f * d) {
                    onExit?.invoke()
                    return true
                }
                if (y < 20f * d && x < 3f * d + 240f * d) {
                    val m = ((x - 3f * d) / (80f * d)).toInt().coerceIn(0, 2)
                    if (m != drumMode) {
                        drumMode = m
                        invalidate()
                    }
                    return true
                }
                val gap = 4f * d
                val top = 20f * d
                val cols = if (padCount > 16) 8 else 4
                val rows = (padCount + cols - 1) / cols
                val padW = (w - (cols + 1) * gap) / cols
                val padH = (height - top - rows * gap + gap) / rows
                val col = ((x - gap) / (padW + gap)).toInt()
                val row = ((y - top - gap) / (padH + gap)).toInt()
                if (col in 0 until cols && row in 0 until rows) {
                    val i = row * cols + col
                    if (i in 0 until padCount) {
                        flash = i
                        flashAt = System.currentTimeMillis()
                        onDrum?.invoke(i)
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                flash = -1
                invalidate()
            }
        }
        return true
    }
}

class ModeBadgeView(context: Context) : View(context) {

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    var mode = 0
        set(value) { field = value; invalidate() }

    var onBadgeTap: (() -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val col = when (mode) {
            1 -> Color.rgb(0, 240, 255)
            2 -> Color.rgb(70, 255, 130)
            else -> Color.rgb(255, 190, 0)
        }
        val name = when (mode) {
            1 -> "MOTION"
            2 -> "SAMPLES"
            else -> "SC\u00c8NE"
        }
        paint.fill(Color.rgb(16, 18, 23))
        canvas.drawRoundRect(0f, 0f, w, h, 5f * d, 5f * d, paint)
        paint.stroke(col, 1.2f * d)
        canvas.drawRoundRect(0f, 0f, w, h, 5f * d, 5f * d, paint)
        paint.fill(col)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = h * 0.34f
        canvas.drawText(name, w / 2f, h * 0.66f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            onBadgeTap?.invoke()
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }
}
