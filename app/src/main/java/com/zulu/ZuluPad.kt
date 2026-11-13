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

    var stateA = AxisState.MOM
        set(value) {
            field = value
            if (value == AxisState.OFF && fingerA != -1) {
                fingerA = -1
            }
            invalidate()
        }

    var stateB = AxisState.MOM
        set(value) {
            field = value
            if (value == AxisState.OFF && fingerB != -1) {
                fingerB = -1
            }
            invalidate()
        }

    private var ax = 0.35f
    private var ay = 0.50f
    private var bx = 0.65f
    private var by = 0.50f

    private var targetAx = ax
    private var targetAy = ay
    private var targetBx = bx
    private var targetBy = by

    private var fingerA = -1
    private var fingerB = -1

    private val yellow = Color.rgb(255, 245, 0)
    private val pink = Color.rgb(255, 70, 170)
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

        ax += (targetAx - ax) * 0.18f
        ay += (targetAy - ay) * 0.18f
        bx += (targetBx - bx) * 0.18f
        by += (targetBy - by) * 0.18f

        drawCursor(
            canvas, ax * w, (1f - ay) * h,
            yellow, "A", stateA == AxisState.MOM
        )

        drawCursor(
            canvas, bx * w, (1f - by) * h,
            pink, "B", stateB == AxisState.MOM
        )

        postInvalidateOnAnimation()
    }

    private fun drawCursor(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
        letter: String,
        active: Boolean
    ) {
        paint.color = if (active) color else offColor

        if (active) {
            paint.style = Paint.Style.FILL
            paint.alpha = 45
            canvas.drawCircle(x, y, 42f * d, paint)
            paint.alpha = 255
            canvas.drawCircle(x, y, 17f * d, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f * d
            paint.alpha = 180
            canvas.drawCircle(x, y, 13f * d, paint)
        }

        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.color = if (active) Color.WHITE else offColor
        canvas.drawCircle(x, y, 5f * d, paint)

        paint.color = Color.rgb(20, 22, 28)
        paint.textSize = 13f * d
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(letter, x, y + 5f * d, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val id = event.getPointerId(event.actionIndex)

                if (stateA == AxisState.MOM && fingerA == -1) {
                    fingerA = id
                } else if (stateB == AxisState.MOM && fingerB == -1) {
                    fingerB = id
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == fingerA) fingerA = -1
                if (id == fingerB) fingerB = -1
            }

            MotionEvent.ACTION_CANCEL -> {
                fingerA = -1
                fingerB = -1
            }
        }

        val indexA = event.findPointerIndex(fingerA)
        val indexB = event.findPointerIndex(fingerB)

        if (stateA == AxisState.MOM && indexA >= 0) {
            targetAx = (event.getX(indexA) / width).coerceIn(0f, 1f)
            targetAy = (1f - event.getY(indexA) / height).coerceIn(0f, 1f)
        }

        if (stateB == AxisState.MOM && indexB >= 0) {
            targetBx = (event.getX(indexB) / width).coerceIn(0f, 1f)
            targetBy = (1f - event.getY(indexB) / height).coerceIn(0f, 1f)
        }

        invalidate()
        return true
    }
}
