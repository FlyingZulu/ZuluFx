package com.zulu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

private val cream = Color.rgb(236, 224, 196)
private val ink = Color.rgb(40, 38, 32)
private val needleRed = Color.rgb(200, 60, 40)
private val amber = Color.rgb(255, 176, 0)
private val silk = Color.rgb(165, 172, 185)
private val graffitiOrange = Color.rgb(255, 120, 30)

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

open class DialView(context: Context) : View(context) {

    protected val d = resources.displayMetrics.density
    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val marker = markerFont(context)

    protected fun drawBezelAndFace(canvas: Canvas, w: Float, h: Float) {
        paint.fill(Color.rgb(30, 33, 40))
        canvas.drawRoundRect(0f, 0f, w, h, 6f * d, 6f * d, paint)
        val m = 2.5f * d
        paint.fill(cream)
        canvas.drawRoundRect(m, m, w - m, h - m, 4f * d, 4f * d, paint)
        paint.stroke(Color.rgb(75, 82, 98), 1f * d)
        drawSketchyRoundRect(canvas, paint, 0.8f * d, 0.8f * d, w - 0.8f * d, h - 0.8f * d, 6f * d, 7)
    }

    protected fun needleXY(pivotX: Float, pivotY: Float, len: Float, angleDeg: Float): FloatArray {
        val rad = Math.toRadians(angleDeg.toDouble())
        return floatArrayOf(
            (pivotX + len * sin(rad)).toFloat(),
            (pivotY - len * cos(rad)).toFloat()
        )
    }
}

class VuMeterView(context: Context) : DialView(context) {

    val cmdId = "vu"

    var levelL = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    var levelR = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        drawBezelAndFace(canvas, w, h)

        val pivotX = w / 2f
        val pivotY = h * 1.18f
        val radius = h * 0.92f
        val start = -52f
        val sweep = 104f

        paint.stroke(ink, 1.2f * d)
        canvas.drawArc(
            pivotX - radius, pivotY - radius, pivotX + radius, pivotY + radius,
            270f + start, sweep, false, paint
        )

        for (i in 0..10) {
            val a = start + sweep * i / 10f
            val outer = needleXY(pivotX, pivotY, radius, a)
            val inner = needleXY(pivotX, pivotY, radius * 0.86f, a)
            paint.stroke(if (i >= 8) needleRed else ink, if (i % 5 == 0) 1.4f * d else 1f * d)
            canvas.drawLine(inner[0], inner[1], outer[0], outer[1], paint)
        }

        paint.fill(ink)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = 9f * d
        canvas.drawText("VU", pivotX, h * 0.46f, paint)

        drawNeedle(canvas, pivotX, pivotY, radius * 0.95f, start + sweep * levelL)
        drawNeedle(canvas, pivotX, pivotY, radius * 0.78f, start + sweep * levelR)
    }

    private fun drawNeedle(canvas: Canvas, pivotX: Float, pivotY: Float, len: Float, angle: Float) {
        val tip = needleXY(pivotX, pivotY, len, angle)
        val tail = needleXY(pivotX, pivotY, len * 0.78f, angle)
        paint.stroke(ink, 1.6f * d)
        canvas.drawLine(pivotX, pivotY, tail[0], tail[1], paint)
        paint.stroke(needleRed, 1.6f * d)
        canvas.drawLine(tail[0], tail[1], tip[0], tip[1], paint)
    }
}

class BpmDialView(context: Context) : DialView(context) {

    val cmdId = "bpm"

    var bpm = 120f
        set(value) { field = value.coerceIn(60f, 200f); invalidate() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        drawBezelAndFace(canvas, w, h)

        val pivotX = w / 2f
        val pivotY = h * 1.05f
        val radius = h * 0.78f
        val start = -52f
        val sweep = 104f

        paint.stroke(ink, 1.1f * d)
        canvas.drawArc(
            pivotX - radius, pivotY - radius, pivotX + radius, pivotY + radius,
            270f + start, sweep, false, paint
        )

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        for (b in 60..200 step 20) {
            val a = start + sweep * (b - 60) / 140f
            val outer = needleXY(pivotX, pivotY, radius, a)
            val inner = needleXY(pivotX, pivotY, radius * 0.88f, a)
            paint.stroke(ink, 1f * d)
            canvas.drawLine(inner[0], inner[1], outer[0], outer[1], paint)
            if (b % 40 == 20 || b == 60 || b == 200) {
                val lab = needleXY(pivotX, pivotY, radius * 0.7f, a)
                paint.fill(ink)
                paint.textSize = 6.5f * d
                canvas.drawText("$b", lab[0], lab[1] + 2.5f * d, paint)
            }
        }

        val a = start + sweep * (bpm - 60) / 140f
        val tip = needleXY(pivotX, pivotY, radius * 0.94f, a)
        paint.stroke(ink, 1.6f * d)
        canvas.drawLine(pivotX, pivotY, tip[0], tip[1], paint)

        val winW = w * 0.42f
        val winH = h * 0.2f
        val winL = pivotX - winW / 2f
        val winB = h - 2.5f * d
        paint.fill(Color.rgb(24, 22, 18))
        canvas.drawRect(winL, winB - winH, winL + winW, winB, paint)
        paint.fill(amber)
        paint.textSize = winH * 0.72f
        canvas.drawText("${bpm.toInt()}", pivotX, winB - winH * 0.22f, paint)

        paint.fill(ink)
        paint.textSize = 7f * d
        canvas.drawText("BPM", pivotX, h * 0.32f, paint)
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

    var onPadTouch: ((Int) -> Unit)? = null

    private var flash = -1

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val gap = 4f * d
        val padW = (w - 5f * gap) / 4f
        val padH = (h - 3f * gap) / 2f

        for (i in 0..7) {
            val col = i % 4
            val row = i / 4
            val l = gap + col * (padW + gap)
            val t = gap + row * (padH + gap)
            val lit = (shiftMode && activeBank == i + 1) || flash == i

            paint.fill(if (lit) Color.rgb(255, 176, 0) else Color.rgb(40, 44, 54))
            canvas.drawRoundRect(l, t, l + padW, t + padH, 5f * d, 5f * d, paint)
            paint.stroke(Color.rgb(75, 82, 98), 1f * d)
            drawSketchyRoundRect(canvas, paint, l, t, l + padW, t + padH, 5f * d, 100 + i)

            if (shiftMode) {
                paint.fill(if (lit) ink else silk)
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = marker
                paint.textSize = padH * 0.5f
                canvas.drawText("${i + 1}", l + padW / 2f, t + padH * 0.7f, paint)
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
        val padW = (w - 5f * gap) / 4f
        val padH = (h - 3f * gap) / 2f
        val col = ((x - gap) / (padW + gap)).toInt()
        val row = ((y - gap) / (padH + gap)).toInt()
        if (col !in 0..3 || row !in 0..1) return -1
        return row * 4 + col
    }
}

class ZuLuSelector(context: Context) : View(context) {

    val cmdId = "zuLu"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    val turquoise = Color.rgb(0, 229, 255)
    val violet = Color.rgb(170, 140, 255)

    var side = 0

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val gap = 4f * d
        drawHalf(canvas, 0f, w / 2f - gap / 2f, "ZU", turquoise, side == 1, 51)
        drawHalf(canvas, w / 2f + gap / 2f, w, "LU", violet, side == 2, 52)
    }

    private fun drawHalf(canvas: Canvas, l: Float, r: Float, label: String, color: Int, on: Boolean, seed: Int) {
        paint.fill(if (on) color else Color.rgb(40, 44, 54))
        canvas.drawRoundRect(l, 0f, r, height.toFloat(), 4f * d, 4f * d, paint)
        paint.stroke(Color.rgb(75, 82, 98), 1f * d)
        drawSketchyRoundRect(canvas, paint, l, 0f, r, height.toFloat(), 4f * d, seed)
        paint.fill(if (on) ink else color)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = height * 0.42f
        canvas.drawText(label, (l + r) / 2f, height * 0.68f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN ||
            event.actionMasked == MotionEvent.ACTION_POINTER_DOWN
        ) {
            side = if (event.getX(event.actionIndex) < width / 2f) 1 else 2
        }
        invalidate()
        return true
    }
}

class DiagFaderView(context: Context) : View(context) {

    val cmdId = "faderZuLu"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    val turquoise = Color.rgb(0, 229, 255)
    val violet = Color.rgb(170, 140, 255)

    var value = 0.5f
        private set

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val m = 10f * d

        paint.fill(Color.rgb(30, 33, 40))
        canvas.drawRoundRect(0f, 0f, w, h, 8f * d, 8f * d, paint)
        paint.stroke(Color.rgb(75, 82, 98), 1f * d)
        drawSketchyRoundRect(canvas, paint, 0.8f * d, 0.8f * d, w - 0.8f * d, h - 0.8f * d, 8f * d, 61)

        val groove = android.graphics.Path()
        groove.moveTo(m, h - m)
        groove.quadTo(w / 2f + 3f * d, h / 2f + 3f * d, w - m, m)
        paint.stroke(Color.rgb(18, 20, 25), 5f * d)
        canvas.drawPath(groove, paint)
        paint.stroke(Color.rgb(60, 66, 80), 1f * d)
        canvas.drawPath(groove, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = 10f * d
        paint.fill(turquoise)
        canvas.drawText("ZU", m + 6f * d, h - 3f * d, paint)
        paint.fill(violet)
        canvas.drawText("LU", w - m - 6f * d, 11f * d, paint)

        val u = m + value * (w - 2f * m)
        val v = (h - m) - value * (h - 2f * m)

        paint.fill(Color.rgb(55, 60, 74))
        canvas.drawCircle(u, v, 9f * d, paint)
        paint.stroke(silk, 1.2f * d)
        canvas.drawCircle(u, v, 9f * d, paint)
        paint.stroke(Color.rgb(28, 31, 38), 1f * d)
        for (i in -1..1) {
            canvas.drawLine(u - 5f * d, v + i * 3f * d, u + 5f * d, v + i * 3f * d, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> {
                val w = width.toFloat()
                val h = height.toFloat()
                val m = 10f * d
                val u = ((event.x - m) / (w - 2f * m)).coerceIn(0f, 1f)
                val v = 1f - ((event.y - m) / (h - 2f * m)).coerceIn(0f, 1f)
                value = (u + v) / 2f
            }
        }
        invalidate()
        return true
    }
}

class SegmentedABRecView(context: Context) : View(context) {

    val cmdId = "abRec"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    private val yellow = Color.rgb(245, 255, 90)
    private val pink = Color.rgb(255, 125, 205)
    private val red = Color.rgb(255, 90, 70)

    var aOn = true
        set(value) { field = value; invalidate() }

    var bOn = true
        set(value) { field = value; invalidate() }

    var recHeld = false
        set(value) { field = value; invalidate() }

    var blinkOn = true
        set(value) { field = value; invalidate() }

    var onSegmentDown: ((Int) -> Unit)? = null
    var onSegmentUp: ((Int) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val segW = w / 3f

        paint.fill(Color.rgb(30, 33, 40))
        canvas.drawRoundRect(0f, 0f, w, h, 8f * d, 8f * d, paint)
        paint.stroke(Color.rgb(75, 82, 98), 1f * d)
        drawSketchyRoundRect(canvas, paint, 0.8f * d, 0.8f * d, w - 0.8f * d, h - 0.8f * d, 8f * d, 41)

        val labels = arrayOf("A", "B", "REC")
        val colors = arrayOf(yellow, pink, red)
        val states = booleanArrayOf(aOn, bOn, recHeld && blinkOn)

        for (i in 0..2) {
            val l = i * segW
            val inset = 3f * d
            paint.fill(if (states[i]) colors[i] else Color.rgb(40, 44, 54))
            canvas.drawRoundRect(l + inset, inset, l + segW - inset, h - inset, 5f * d, 5f * d, paint)
            paint.fill(if (states[i]) ink else colors[i])
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = marker
            paint.textSize = h * 0.38f
            canvas.drawText(labels[i], l + segW / 2f, h * 0.64f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
                onSegmentDown?.invoke(segmentAt(event.getX(event.actionIndex)))
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL ->
                onSegmentUp?.invoke(segmentAt(event.getX(event.actionIndex)))
        }
        return true
    }

    private fun segmentAt(x: Float): Int = (x / width * 3f).toInt().coerceIn(0, 2)
}

class JitterDialView(context: Context) : DialView(context) {

    val cmdId = "jitter"

    var value = 0f
        private set

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        drawBezelAndFace(canvas, w, h)

        val cx = w / 2f
        val cy = h * 0.46f
        val radius = minOf(w, h) * 0.34f
        val start = -52f
        val sweep = 104f

        for (i in 0..10) {
            val a = start + sweep * i / 10f
            val outer = needleXY(cx, cy, radius, a)
            val inner = needleXY(cx, cy, radius * 0.88f, a)
            paint.stroke(ink, 1f * d)
            canvas.drawLine(inner[0], inner[1], outer[0], outer[1], paint)
        }

        val tip = needleXY(cx, cy, radius * 0.9f, start + sweep * value)
        paint.stroke(ink, 1.8f * d)
        canvas.drawLine(cx, cy, tip[0], tip[1], paint)
        paint.fill(ink)
        canvas.drawCircle(cx, cy, 3f * d, paint)

        paint.fill(ink)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = 8f * d
        canvas.drawText("JITTER", cx, h * 0.86f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> {
                val cx = width / 2f
                val cy = height * 0.46f
                val dx = (event.x - cx).toDouble()
                val dy = (event.y - cy).toDouble()
                var angle = Math.toDegrees(Math.atan2(dx, -dy))
                if (angle > 180.0) angle -= 360.0
                value = ((angle + 52.0) / 104.0).toFloat().coerceIn(0f, 1f)
            }
        }
        invalidate()
        return true
    }
}

class JitterFaderView(context: Context) : View(context) {

    val cmdId = "jitter"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val marker = markerFont(context)

    var value = 0f
        private set

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val m = 8f * d

        paint.fill(Color.rgb(30, 33, 40))
        canvas.drawRoundRect(0f, 0f, w, h, 8f * d, 8f * d, paint)
        paint.stroke(Color.rgb(75, 82, 98), 1f * d)
        drawSketchyRoundRect(canvas, paint, 0.8f * d, 0.8f * d, w - 0.8f * d, h - 0.8f * d, 8f * d, 71)

        val cx = w / 2f
        paint.stroke(Color.rgb(18, 20, 25), 5f * d)
        canvas.drawLine(cx, m, cx, h - m, paint)
        paint.stroke(Color.rgb(60, 66, 80), 1f * d)
        canvas.drawLine(cx, m, cx, h - m, paint)

        val ky = (h - m) - value * (h - 2f * m)
        paint.fill(Color.rgb(55, 60, 74))
        canvas.drawRoundRect(cx - 11f * d, ky - 5f * d, cx + 11f * d, ky + 5f * d, 3f * d, 3f * d, paint)
        paint.stroke(silk, 1.2f * d)
        canvas.drawRoundRect(cx - 11f * d, ky - 5f * d, cx + 11f * d, ky + 5f * d, 3f * d, 3f * d, paint)
        paint.stroke(Color.rgb(28, 31, 38), 1f * d)
        canvas.drawLine(cx - 7f * d, ky, cx + 7f * d, ky, paint)

        canvas.save()
        canvas.rotate(-90f, cx, h * 0.28f)
        paint.fill(amber)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = 9f * d
        canvas.drawText("JITTER", cx, h * 0.28f + 3f * d, paint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> {
                val h = height.toFloat()
                val m = 8f * d
                value = 1f - ((event.y - m) / (h - 2f * m)).coerceIn(0f, 1f)
            }
        }
        invalidate()
        return true
    }
}
