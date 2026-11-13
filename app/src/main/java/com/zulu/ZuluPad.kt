package com.zulu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ZuluPad(context: Context) : View(context) {

    constructor(context: Context, attrs: AttributeSet?) : this(context)

    enum class AxisState { OFF, MOM }

    val cmdId = "pad"

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var stateA = AxisState.MOM
        set(value) {
            field = value
            if (value == AxisState.OFF) fingerA = -1
            invalidate()
        }

    var stateB = AxisState.MOM
        set(value) {
            field = value
            if (value == AxisState.OFF) fingerB = -1
            invalidate()
        }

    var ax = 0.35f
    var ay = 0.50f
    var bx = 0.65f
    var by = 0.50f

    var waveform: FloatArray? = null

    var activePreset = -1
        set(value) {
            field = value
            invalidate()
        }

    var onPresetTouch: ((Int) -> Unit)? = null

    private val presetSlots = 9

    private fun presetStripHeight(): Float = 20f * d

    var bendActive = false
        set(value) {
            field = value
            invalidate()
        }

    var onBendTouch: ((Boolean) -> Unit)? = null

    private val rnd = java.util.Random()
    private var glitchClock = 0

    private var targetAx = ax
    private var targetAy = ay
    private var targetBx = bx
    private var targetBy = by

    private var fingerA = -1
    private var fingerB = -1

    private val yellow = Color.rgb(245, 255, 90)
    private val pink = Color.rgb(255, 125, 205)
    private val offColor = Color.rgb(90, 96, 108)
    private val amber = Color.rgb(255, 176, 0)
    private val marker = markerFont(context)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(20, 22, 28)
        canvas.drawRect(0f, 0f, w, h, paint)

        val wf = waveform
        if (wf != null && wf.size > 1) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f * d
            paint.color = Color.rgb(88, 100, 118)
            paint.isAntiAlias = true
            val step = maxOf(1, wf.size / 160)
            var px = 0f
            var py = h / 2f
            var i = 0
            while (i < wf.size) {
                val x = w * i / (wf.size - 1).toFloat()
                val y = h / 2f - wf[i].coerceIn(-1f, 1f) * h * 0.35f
                if (i > 0) canvas.drawLine(px, py, x, y, paint)
                px = x
                py = y
                i += step
            }
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = d
        paint.color = Color.rgb(50, 56, 70)
        for (i in 1..3) {
            canvas.drawLine(w * i / 4f, 0f, w * i / 4f, h, paint)
            canvas.drawLine(0f, h * i / 4f, w, h * i / 4f, paint)
        }

        val strip = presetStripHeight()
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(200, 24, 26, 32)
        canvas.drawRect(0f, 0f, w, strip, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * d
        paint.color = Color.rgb(60, 66, 80)
        canvas.drawLine(0f, strip, w, strip, paint)
        val cellW = w / presetSlots
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = 10f * d
        for (i in 0 until presetSlots) {
            val cx = cellW * (i + 0.5f)
            if (i > 0) canvas.drawLine(cellW * i, 2f * d, cellW * i, strip - 2f * d, paint)
            paint.style = Paint.Style.FILL
            paint.color = if (i == activePreset) amber else Color.rgb(120, 128, 144)
            canvas.drawText((i + 1).toString(), cx, strip - 6f * d, paint)
            if (i == activePreset) {
                paint.color = amber
                paint.alpha = 40
                canvas.drawRect(cellW * i + 1f, 1f, cellW * (i + 1) - 1f, strip - 1f, paint)
                paint.alpha = 255
            }
        }

        paint.strokeWidth = 1f * d
        paint.color = Color.rgb(60, 66, 80)
        drawSketchyRoundRect(canvas, paint, 1f * d, 1f * d, w - 1f * d, h - 1f * d, 6f * d, 99)

        ax += (targetAx - ax) * 0.18f
        ay += (targetAy - ay) * 0.18f
        bx += (targetBx - bx) * 0.18f
        by += (targetBy - by) * 0.18f

        var jax = 0f
        var jay = 0f
        var jbx = 0f
        var jby = 0f
        if (bendActive) {
            glitchClock++
            if (glitchClock > 8 + rnd.nextInt(16)) {
                glitchClock = 0
                targetAx = rnd.nextFloat()
                targetAy = rnd.nextFloat()
                targetBx = rnd.nextFloat()
                targetBy = rnd.nextFloat()
            }
            jax = (rnd.nextFloat() - 0.5f) * 4f * d
            jay = (rnd.nextFloat() - 0.5f) * 4f * d
            jbx = (rnd.nextFloat() - 0.5f) * 4f * d
            jby = (rnd.nextFloat() - 0.5f) * 4f * d
        }

        drawCursor(canvas, ax * w + jax, (1f - ay) * h + jay, yellow, "A", stateA == AxisState.MOM,
            "${(ax * 100).toInt()} \u00b7 ${(ay * 100).toInt()}")
        drawCursor(canvas, bx * w + jbx, (1f - by) * h + jby, pink, "B", stateB == AxisState.MOM,
            "${(bx * 100).toInt()} \u00b7 ${(by * 100).toInt()}")

        postInvalidateOnAnimation()
    }

    private fun drawCursor(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
        letter: String,
        active: Boolean,
        values: String
    ) {
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = marker
        paint.textSize = 13f * d

        if (active) {
            paint.style = Paint.Style.FILL
            paint.color = color
            paint.alpha = 45
            canvas.drawCircle(x, y, 42f * d, paint)
            paint.alpha = 255
            canvas.drawCircle(x, y, 17f * d, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, 5f * d, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f * d
            paint.color = offColor
            paint.alpha = 180
            canvas.drawCircle(x, y, 13f * d, paint)
            paint.style = Paint.Style.FILL
            paint.color = offColor
            canvas.drawCircle(x, y, 5f * d, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(20, 22, 28)
        canvas.drawText(letter, x, y + 5f * d, paint)

        paint.textSize = 11f * d
        paint.color = if (active) color else offColor
        canvas.drawText(values, x, y + 30f * d, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val id = event.getPointerId(event.actionIndex)
                if (event.getY(event.actionIndex) < presetStripHeight()) {
                    activePreset = (event.getX(event.actionIndex) / width * presetSlots)
                        .toInt().coerceIn(0, presetSlots - 1)
                    onPresetTouch?.invoke(activePreset)
                    invalidate()
                    return true
                }
                if (fingerA == -1 && fingerB == -1) onBendTouch?.invoke(true)
                if (fingerA == -1 && fingerB == -1) {
                    if (stateA == AxisState.MOM) fingerA = id
                    else if (stateB == AxisState.MOM) fingerB = id
                } else if (fingerA == -1 && stateA == AxisState.MOM) {
                    fingerA = id
                } else if (fingerB == -1 && stateB == AxisState.MOM) {
                    fingerB = id
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == fingerA) fingerA = -1
                if (id == fingerB) fingerB = -1
                if (fingerA == -1 && fingerB == -1) onBendTouch?.invoke(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                fingerA = -1
                fingerB = -1
                onBendTouch?.invoke(false)
            }
        }

        val idxA = event.findPointerIndex(fingerA)
        val idxB = event.findPointerIndex(fingerB)

        if (idxA >= 0) {
            targetAx = (event.getX(idxA) / width).coerceIn(0f, 1f)
            targetAy = (1f - event.getY(idxA) / height).coerceIn(0f, 1f)
        }
        if (idxB >= 0) {
            targetBx = (event.getX(idxB) / width).coerceIn(0f, 1f)
            targetBy = (1f - event.getY(idxB) / height).coerceIn(0f, 1f)
        }

        invalidate()
        return true
    }
}
