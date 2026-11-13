package com.zulu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val cream = Color.rgb(236, 224, 196)
private val ink = Color.rgb(40, 38, 32)
private val needleRed = Color.rgb(200, 60, 40)
private val amber = Color.rgb(255, 176, 0)
private val silk = Color.rgb(165, 172, 185)

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

    var onDoubleTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null

    var pressed = false
        private set
    var onPressed: ((Boolean) -> Unit)? = null

    private var lastTap = 0L
    private var longPressPending = false

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        paint.fill(Color.rgb(30, 33, 40))
        canvas.drawRoundRect(0f, 0f, w, h, 6f * d, 6f * d, paint)
        paint.stroke(Color.rgb(75, 82, 98), 1f * d)
        drawSketchyRoundRect(canvas, paint, 0.8f * d, 0.8f * d, w - 0.8f * d, h - 0.8f * d, 6f * d, 17)

        val m = 4f * d
        val barW = (w - 3f * m) / 2f
        drawBar(canvas, m, m, m + barW, h - m, levelL)
        drawBar(canvas, m * 2f + barW, m, w - m, h - m, levelR)
    }

    private fun drawBar(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, level: Float) {
        val segs = 20
        val segH = (b - t) / segs
        val lit = (level * segs).toInt()
        for (i in 0 until segs) {
            paint.fill(
                when {
                    i >= lit -> Color.rgb(38, 42, 52)
                    i >= segs * 85 / 100 -> Color.rgb(220, 70, 50)
                    i >= segs * 65 / 100 -> amber
                    else -> Color.rgb(90, 200, 90)
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
                val now = System.currentTimeMillis()
                longPressPending = true
                pressed = true
                onPressed?.invoke(true)
                postDelayed({
                    if (longPressPending) {
                        longPressPending = false
                        onLongPress?.invoke()
                    }
                }, 600)
                if (now - lastTap < 300) {
                    onDoubleTap?.invoke()
                    lastTap = 0
                } else {
                    lastTap = now
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

    var soloMask = 0
        set(value) { field = value; invalidate() }

    var onPadTouch: ((Int) -> Unit)? = null

    private var flash = -1

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
            val rec = shiftMode && recPad == i

            paint.fill(
                when {
                    rec && blinkOn -> Color.rgb(255, 90, 70)
                    rec -> Color.rgb(120, 40, 34)
                    soloOn -> Color.rgb(0, 229, 255)
                    lit -> Color.rgb(255, 176, 0)
                    else -> Color.rgb(40, 44, 54)
                }
            )
            canvas.drawRoundRect(l, t, l + padW, t + padH, 5f * d, 5f * d, paint)
            paint.stroke(Color.rgb(75, 82, 98), 1f * d)
            drawSketchyRoundRect(canvas, paint, l, t, l + padW, t + padH, 5f * d, 100 + i)

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
                paint.fill(if (lit) ink else Color.rgb(70, 76, 90))
                canvas.drawCircle(l + padW / 2f, t + padH / 2f, 2.5f * d, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = padAt(event.getX(event.actionIndex), event.getY(event.actionIndex))
                if (i >= 0) {
                    flash = i
                    onPadTouch?.invoke(i + 1)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> flash = -1
        }
        invalidate()
        return true
    }

    private fun padAt(x: Float, y: Float): Int {
        val w = width.toFloat()
        val h = height.toFloat()
        val gap = 4f * d
        val rowsF = rows.toFloat()
        val padW = (w - 5f * gap) / 4f
        val padH = (h - (rowsF + 1f) * gap) / rowsF
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
        canvas.drawRoundRect(kx - 8f * d, cy - 13f * d, kx + 8f * d, cy + 13f * d, 4f * d, 4f * d, paint)
        paint.stroke(if (stutter) Color.WHITE else silk, 1.2f * d)
        paint.alpha = if (stutter) 220 else 90
        canvas.drawRoundRect(kx - 8f * d, cy - 13f * d, kx + 8f * d, cy + 13f * d, 4f * d, 4f * d, paint)
        paint.alpha = 255
        paint.stroke(Color.rgb(40, 26, 8), 1.5f * d)
        canvas.drawLine(kx, cy - 9f * d, kx, cy + 9f * d, paint)

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

    private val turquoise = Color.rgb(60, 220, 200)
    private val violet = Color.rgb(190, 120, 255)

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
        val th = min(h * 0.7f, 44f * d)
        val tc = currentThumbColor()
        paint.fill(tc)
        canvas.drawRoundRect(kx - 8f * d, cy - th / 2f, kx + 8f * d, cy + th / 2f, 4f * d, 4f * d, paint)
        paint.stroke(Color.WHITE, 1.2f * d)
        paint.alpha = 90
        canvas.drawRoundRect(kx - 8f * d, cy - th / 2f, kx + 8f * d, cy + th / 2f, 4f * d, 4f * d, paint)
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
