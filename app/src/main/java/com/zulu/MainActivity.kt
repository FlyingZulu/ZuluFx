package com.zulu

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ZuluPad(context: android.content.Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var ax = 0.35f
    private var ay = 0.50f
    private var bx = 0.65f
    private var by = 0.50f

    override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        canvas.drawColor(Color.rgb(20, 22, 28))

        paint.color = Color.rgb(48, 53, 66)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f

        for (i in 1..3) {
            val x = width * i / 4f
            val y = height * i / 4f
            canvas.drawLine(x, 0f, x, height, paint)
            canvas.drawLine(0f, y, width, y, paint)
        }

        drawCursor(canvas, ax * width, (1f - ay) * height, Color.YELLOW, "A")
        drawCursor(canvas, bx * width, (1f - by) * height, Color.rgb(255, 55, 160), "B")
    }

    private fun drawCursor(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
        label: String
    ) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = 70
        canvas.drawCircle(x, y, 42f, paint)

        paint.alpha = 255
        canvas.drawCircle(x, y, 17f, paint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, x, y + 6f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val index = event.actionIndex
        val x = (event.getX(index) / width).coerceIn(0f, 1f)
        val y = (1f - event.getY(index) / height).coerceIn(0f, 1f)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 1) {
                    ax = (event.getX(0) / width).coerceIn(0f, 1f)
                    ay = (1f - event.getY(0) / height).coerceIn(0f, 1f)
                }

                if (event.pointerCount >= 2) {
                    bx = (event.getX(1) / width).coerceIn(0f, 1f)
                    by = (1f - event.getY(1) / height).coerceIn(0f, 1f)
                }

                invalidate()
            }
        }

        return true
    }
}

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(11, 12, 16))
        }

        val pad = ZuluPad(this)

        val side = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 12, 12, 12)
        }

        val label = TextView(this).apply {
            text = "ZULU\nINTERFACE"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
        }

        val start = Button(this).apply {
            text = "START"
        }

        val mute = Button(this).apply {
            text = "MUTE"
        }

        side.addView(label)
        side.addView(start)
        side.addView(mute)

        layout.addView(
            pad,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        )

        layout.addView(
            side,
            LinearLayout.LayoutParams(140, LinearLayout.LayoutParams.MATCH_PARENT)
        )

        setContentView(layout)
    }
}
