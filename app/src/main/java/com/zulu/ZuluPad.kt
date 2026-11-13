package com.zulu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.sqrt
import kotlin.random.Random

class ZuluPad(context: Context) : View(context) {

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var ax = 0.35f
    private var ay = 0.50f
    private var bx = 0.65f
    private var by = 0.50f

    private var targetAx = ax
    private var targetAy = ay
    private var targetBx = bx
    private var targetBy = by

    private var velocityAx = 0f
    private var velocityAy = 0f
    private var velocityBx = 0f
    private var velocityBy = 0f

    private var fingerA = -1
    private var fingerB = -1

    private var pinched = false
    private var lastDistance = 0f

    private val yellow = Color.rgb(255, 220, 35)
    private val pink = Color.rgb(255, 70, 170)

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

        updatePhysics()

        drawCursor(canvas, ax * w, (1f - ay) * h, yellow, "A")
        drawCursor(canvas, bx * w, (1f - by) * h, pink, "B")

        postInvalidateOnAnimation()
    }

    private fun drawCursor(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
        letter: String
    ) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = 45
        canvas.drawCircle(x, y, 42f * d, paint)

        paint.alpha = 255
        canvas.drawCircle(x, y, 17f * d, paint)

        paint.color = Color.WHITE
        canvas.drawCircle(x, y, 6f * d, paint)

        paint.color = Color.rgb(20, 22, 28)
        paint.textSize = 13f * d
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(letter, x, y + 5f * d, paint)
    }

    private fun updatePhysics() {
        val attraction = 0.09f
        val friction = 0.86f

        velocityAx = (velocityAx + (targetAx - ax) * attraction) * friction
        velocityAy = (velocityAy + (targetAy - ay) * attraction) * friction
        velocityBx = (velocityBx + (targetBx - bx) * attraction) * friction
        velocityBy = (velocityBy + (targetBy - by) * attraction) * friction

        ax += velocityAx
        ay += velocityAy
        bx += velocityBx
        by += velocityBy

        if (ax < 0f || ax > 1f) velocityAx = -velocityAx * 0.65f
        if (ay < 0f || ay > 1f) velocityAy = -velocityAy * 0.65f
        if (bx < 0f || bx > 1f) velocityBx = -velocityBx * 0.65f
        if (by < 0f || by > 1f) velocityBy = -velocityBy * 0.65f

        ax = ax.coerceIn(0f, 1f)
        ay = ay.coerceIn(0f, 1f)
        bx = bx.coerceIn(0f, 1f)
        by = by.coerceIn(0f, 1f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val id = event.getPointerId(event.actionIndex)

                if (fingerA == -1) {
                    fingerA = id
                } else if (fingerB == -1) {
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
                pinched = false
            }
        }

        updateTargets(event)
        invalidate()
        return true
    }

    private fun updateTargets(event: MotionEvent) {
        val a = event.findPointerIndex(fingerA)
        val b = event.findPointerIndex(fingerB)

        if (a >= 0) {
            targetAx = (event.getX(a) / width).coerceIn(0f, 1f)
            targetAy = (1f - event.getY(a) / height).coerceIn(0f, 1f)
        }

        if (b >= 0) {
            targetBx = (event.getX(b) / width).coerceIn(0f, 1f)
            targetBy = (1f - event.getY(b) / height).coerceIn(0f, 1f)
        }

        if (event.pointerCount < 4 || a < 0 || b < 0) {
            pinched = false
            return
        }

        val dx = targetBx - targetAx
        val dy = targetBy - targetAy
        val distance = sqrt(dx * dx + dy * dy)

        if (distance < 0.18f) {
            pinched = true
        }

        if (pinched && distance > 0.32f && distance > lastDistance) {
            val angle = Random.nextFloat() * 6.283185f
            val force = 0.06f

            velocityAx = kotlin.math.cos(angle) * force
            velocityAy = kotlin.math.sin(angle) * force
            velocityBx = -velocityAx
            velocityBy = -velocityAy

            pinched = false
        }

        lastDistance = distance
    }
}
