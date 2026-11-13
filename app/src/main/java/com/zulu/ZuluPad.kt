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
            if (value == AxisState.MOM) fOrder = nextOrder()
            invalidate()
        }

    var stateZ = AxisState.MOM
        set(value) {
            field = value
            if (value == AxisState.MOM) zOrder = nextOrder()
            invalidate()
        }

    private var fOrder = 1L
    private var zOrder = 2L
    private var orderCounter = 2L

    private fun nextOrder(): Long {
        orderCounter += 1L
        return orderCounter
    }

    private var fx = 0.35f
    private var fy = 0.50f
    private var zx = 0.65f
    private var zy = 0.50f

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

        drawCursor(canvas, fx * w, (1f - fy) * h, yellow, "F", stateF)
        drawCursor(canvas, zx * w, (1f - zy) * h, pink, "Z", stateZ)

        postInvalidateOnAnimation()
    }

    private fun drawCursor(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
        letter: String,
        state: AxisState
    ) {
        val active = state == AxisState.MOM

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

    private fun hitF(x: Float, y: Float): Boolean {
        val dx = x - fx * width
        val dy = y - (1f - fy) * height
        val r = 54f * d
        return dx * dx + dy * dy <= r * r
    }

    private fun hitZ(x: Float, y: Float): Boolean {
        val dx = x - zx * width
        val dy = y - (1f - zy) * height
        val r = 54f * d
        return dx * dx + dy * dy <= r * r
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val id = event.getPointerId(index)
                val x = event.getX(index)
                val y = event.getY(index)

                // Priorité absolue au toucher direct du curseur
                if (fingerF == -1 && hitF(x, y)) {
                    fingerF = id
                } else if (fingerZ == -1 && hitZ(x, y)) {
                    fingerZ = id
                } 
                // Sinon, attribution aux axes libres selon priorité
                else {
                    val fFree = fingerF == -1
                    val zFree = fingerZ == -1
                    
                    if (fFree && zFree) {
                        if (stateF == AxisState.MOM && stateZ == AxisState.MOM) {
                            if (fOrder <= zOrder) fingerF = id else fingerZ = id
                        } else if (stateF == AxisState.MOM) {
                            fingerF = id
                        } else if (stateZ == AxisState.MOM) {
                            fingerZ = id
                        }
                    } else if (fFree && stateF == AxisState.MOM) {
                        fingerF = id
                    } else if (zFree && stateZ == AxisState.MOM) {
                        fingerZ = id
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == fingerF) fingerF = -1
                if (id == fingerZ) fingerZ = -1
            }

            MotionEvent.ACTION_CANCEL -> {
                fingerF = -1
                fingerZ = -1
            }
        }

        // Mise à jour des positions cibles pour tous les doigts actifs
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
