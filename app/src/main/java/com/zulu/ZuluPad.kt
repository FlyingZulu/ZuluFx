package com.zulu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

class ZuluPad(context: Context) : View(context) {

    enum class AxisState { OFF, MOM }

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var stateF = AxisState.MOM
        set(value) {
            field = value
            if (value == AxisState.OFF && fingerF != -1) fingerF = -1
            invalidate()
        }

    var stateZ = AxisState.MOM
        set(value) {
            field = value
            if (value == AxisState.OFF && fingerZ != -1) fingerZ = -1
            invalidate()
        }

    private var fOrder = 1L
    private var zOrder = 2L

    var fx = 0.35f
    var fy = 0.50f
    var zx = 0.65f
    var zy = 0.50f

    private var targetFx = fx
    private var targetFy = fy
    private var targetZx = zx
    private var targetZy = zy

    private var fingerF = -1
    private var fingerZ = -1

    private val yellow = Color.rgb(245, 255, 90)
    private val pink = Color.rgb(255, 125, 205)
    private val offColor = Color.rgb(90, 96, 108)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(20, 22, 28)
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = d
        paint.color = Color.rgb(50, 56, 70)

        for (i in 1..3) {
            canvas.drawLine(w * i / 4f, 0f, w * i / 4f, h, paint)
            canvas.drawLine(0f, h * i / 4f, w, h * i / 4f, paint)
        }

        fx += (targetFx - fx) * 0.18f
        fy += (targetFy - fy) * 0.18f
        zx += (targetZx - zx) * 0.18f
        zy += (targetZy - zy) * 0.18f

        drawCursor(canvas, fx * w, (1f - fy) * h, yellow, "F")
        drawCursor(canvas, zx * w, (1f - zy) * h, pink, "Z")

        postInvalidateOnAnimation()
    }

    private fun drawCursor(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
        letter: String
    ) {
        val active = (letter == "F" && stateF == AxisState.MOM) ||
                     (letter == "Z" && stateZ == AxisState.MOM)

        paint.color = if (active) color else offColor
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 13f * d

        if (active) {
            paint.style = Paint.Style.FILL
            paint.alpha = 45
            canvas.drawCircle(x, y, 42f * d, paint)
            paint.alpha = 255
            canvas.drawCircle(x, y, 17f * d, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, 5f * d, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f * d
            paint.alpha = 180
            canvas.drawCircle(x, y, 13f * d, paint)
            paint.color = offColor
            canvas.drawCircle(x, y, 5f * d, paint)
        }

        paint.color = Color.rgb(20, 22, 28)
        canvas.drawText(letter, x, y + 5f * d, paint)
    }

    private fun hitF(x: Float, y: Float): Boolean {
        val cx = fx * width
        val cy = (1f - fy) * height
        val dx = x - cx
        val dy = y - cy
        return dx * dx + dy * dy <= (54f * d) * (54f * d)
    }

    private fun hitZ(x: Float, y: Float): Boolean {
        val cx = zx * width
        val cy = (1f - zy) * height
        val dx = x - cx
        val dy = y - cy
        return dx * dx + dy * dy <= (54f * d) * (54f * d)
    }

    private fun assignFinger(id: Int, x: Float, y: Float) {
        val fFree = stateF == AxisState.MOM && fingerF == -1
        val zFree = stateZ == AxisState.MOM && fingerZ == -1

        if (fFree && hitF(x, y)) { fingerF = id; return }
        if (zFree && hitZ(x, y)) { fingerZ = id; return }

        if (fFree && zFree) {
            if (fOrder <= zOrder) { fingerF = id } else { fingerZ = id }
            return
        }

        if (fFree) { fingerF = id } else if (zFree) { fingerZ = id }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                assignFinger(event.getPointerId(event.actionIndex),
                    event.getX(event.actionIndex), event.getY(event.actionIndex))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == fingerF) fingerF = -1
                if (id == fingerZ) fingerZ = -1
            }
            MotionEvent.ACTION_CANCEL -> {
                fingerF = -1
                fingerZ = -1
            }
        }

        val idxF = event.findPointerIndex(fingerF)
        val idxZ = event.findPointerIndex(fingerZ)

        if (idxF >= 0) {
            targetFx = (event.getX(idxF) / width).coerceIn(0f, 1f)
            targetFy = (1f - event.getY(idxF) / height).coerceIn(0f, 1f)
        }

        if (idxZ >= 0) {
            targetZx = (event.getX(idxZ) / width).coerceIn(0f, 1f)
            targetZy = (1f - event.getY(idxZ) / height).coerceIn(0f, 1f)
        }

        invalidate()
        return true
    }
}
