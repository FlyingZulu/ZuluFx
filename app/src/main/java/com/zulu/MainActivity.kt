package com.zulu

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class ZuluPad(context: Context) : View(context) {

    private val d = resources.displayMetrics.density

    // Positions visibles des curseurs.
    private var ax = 0.35f
    private var ay = 0.50f
    private var bx = 0.65f
    private var by = 0.50f

    // Positions des doigts A et B.
    private var rawAx = ax
    private var rawAy = ay
    private var rawBx = bx
    private var rawBy = by

    // Cibles vers lesquelles les curseurs sont attirés.
    private var targetAx = ax
    private var targetAy = ay
    private var targetBx = bx
    private var targetBy = by

    // Vitesses, utilisées pour l'inertie et le billard.
    private var velocityAx = 0f
    private var velocityAy = 0f
    private var velocityBx = 0f
    private var velocityBy = 0f

    // Identifiants stables des doigts Android.
    private var fingerA = -1
    private var fingerB = -1

    // État du geste spécial à quatre doigts.
    private var billiardActive = false
    private var billiardStartDistance = 0f
    private var breakReady = true
    private var billiardTime = 0f

    private var lastFrameNanos = 0L

    private val yellow = Color.rgb(255, 220, 35)
    private val pink = Color.rgb(255, 65, 170)

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 22, 28)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(45, 51, 64)
        style = Paint.Style.STROKE
        strokeWidth = 1f * d
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(70, 78, 96)
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
    }

    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * d
    }

    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(128, 140, 165)
        textSize = 13f * d
    }

    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        updatePhysics()

        val w = width.toFloat()
        val h = height.toFloat()

        val margin = 5f * d
        val radius = 16f * d

        rect.set(margin, margin, w - margin, h - margin)

        canvas.drawRoundRect(rect, radius, radius, backgroundPaint)

        for (i in 1..3) {
            val x = margin + (w - 2f * margin) * i / 4f
            val y = margin + (h - 2f * margin) * i / 4f

            canvas.drawLine(x, margin, x, h - margin, gridPaint)
            canvas.drawLine(margin, y, w - margin, y, gridPaint)
        }

        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        labelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("RÉSO ↑", margin + 12f * d, margin + 28f * d, labelPaint)

        labelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("FILTRE →", w - margin - 12f * d, h - margin - 14f * d, labelPaint)

        if (billiardActive) {
            labelPaint.textAlign = Paint.Align.CENTER
            labelPaint.color = Color.rgb(175, 185, 210)
            canvas.drawText("MODE BILLARD", w * 0.5f, margin + 28f * d, labelPaint)
            labelPaint.color = Color.rgb(128, 140, 165)
        }

        val cursorAX = margin + ax * (w - 2f * margin)
        val cursorAY = margin + (1f - ay) * (h - 2f * margin)

        val cursorBX = margin + bx * (w - 2f * margin)
        val cursorBY = margin + (1f - by) * (h - 2f * margin)

        drawCursor(canvas, cursorAX, cursorAY, yellow, "A")
        drawCursor(canvas, cursorBX, cursorBY, pink, "B")

        postInvalidateOnAnimation()
    }

    private fun drawCursor(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: Int,
        letter: String
    ) {
        crossPaint.color = color
        crossPaint.alpha = 55

        canvas.drawLine(x, 5f * d, x, height - 5f * d, crossPaint)
        canvas.drawLine(5
