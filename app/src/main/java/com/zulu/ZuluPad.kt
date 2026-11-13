package com.zulu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ZuluPad(context: Context) : View(context) {

    constructor(context: Context, attrs: AttributeSet?) : this(context)

    enum class AxisState { OFF, MOM, HOLD }

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

    fun setCursor(nax: Float, nay: Float, nbx: Float, nby: Float) {
        targetAx = nax.coerceIn(0f, 1f)
        targetAy = nay.coerceIn(0f, 1f)
        if (!merged) {
            targetBx = nbx.coerceIn(0f, 1f)
            targetBy = nby.coerceIn(0f, 1f)
        }
        invalidate()
    }

    var activePreset = -1
    var arpOn = false
        set(value) {
            field = value
            invalidate()
        }

    var userPresets = BooleanArray(9)

    var ghostZu: FloatArray? = null
        set(value) { field = value; invalidate() }
    var ghostLu: FloatArray? = null
        set(value) { field = value; invalidate() }

    private val turquoiseC = Color.rgb(0, 240, 255)
    private val luC = Color.rgb(200, 90, 255)

    private fun drawGhost(canvas: Canvas, gx: Float, gy: Float, color: Int) {
        val x = gx * width
        val y = (1f - gy) * height
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * d
        paint.color = color
        paint.alpha = 110
        canvas.drawCircle(x, y, 11f * d, paint)
        paint.alpha = 70
        canvas.drawLine(x - 16f * d, y, x + 16f * d, y, paint)
        canvas.drawLine(x, y - 16f * d, x, y + 16f * d, paint)
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    var onAxisChange: ((Float, Float, Float, Float) -> Unit)? = null
    var onPadTouchState: ((Boolean) -> Unit)? = null

    private var mandalaPh = 0f
    private var mandalaLvl = 0f
    private var mandalaAggr = 0f
    private val mandalaPath = Path()

    fun setAudio(lvl: Float, aggr: Float) {
        mandalaLvl += 0.10f * (lvl.coerceIn(0f, 1f) - mandalaLvl)
        mandalaAggr += 0.06f * (aggr.coerceIn(0f, 1f) - mandalaAggr)
        mandalaPh += 0.0012f + mandalaLvl * 0.003f
        if (mandalaPh >= 1f) mandalaPh -= 1f
        if (mandalaLvl > 0.004f || mandalaAggr > 0.004f) postInvalidateOnAnimation()
    }

    private fun drawMandala(c: Canvas, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val baseR = minOf(w, h) * 0.46f
        val amp = 0.05f + mandalaLvl * 0.22f
        val ag = mandalaAggr.coerceIn(0f, 1f)
        val col = Color.rgb(
            (0 + (255 - 0) * ag).toInt(),
            (229 - (229 - 96) * ag).toInt(),
            (255 - (255 - 46) * ag).toInt()
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * d
        paint.color = col
        val petals = 12
        for (layer in 0 until 3) {
            val dir = if (layer % 2 == 0) 1f else -1f
            val rot = mandalaPh * dir * 6.28318f + layer * 0.41f
            val rr = baseR * (0.40f + layer * 0.21f) * (1f + mandalaLvl * 0.10f * (layer + 1))
            paint.alpha = (12 + layer * 4 + (mandalaLvl * 30f).toInt()).coerceIn(0, 62)
            mandalaPath.reset()
            val segs = petals * 10
            for (i in 0..segs) {
                val t = i / segs.toFloat()
                val a = rot + t * 6.28318f
                val wob = Math.sin((t * petals + layer * 1.7f + mandalaPh * 5f * dir) * 6.28318).toFloat()
                val rad = rr * (1f + amp * wob)
                val x = cx + rad * Math.cos(a.toDouble()).toFloat()
                val y = cy + rad * Math.sin(a.toDouble()).toFloat()
                if (i == 0) mandalaPath.moveTo(x, y) else mandalaPath.lineTo(x, y)
            }
            mandalaPath.close()
            c.drawPath(mandalaPath, paint)
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    var bendActive = false
        set(value) {
            field = value
            invalidate()
        }

    var fingerMode = 0
    var bendFreeze = false
    var billardFriction = 2

    var onBendTouch: ((Boolean) -> Unit)? = null
    var onBendPos: ((Float, Float) -> Unit)? = null

    private val rnd = java.util.Random()
    private var glitchClock = 0

    private var targetAx = ax
    private var targetAy = ay
    private var targetBx = bx
    private var targetBy = by

    private var fingerA = -1
    private var fingerB = -1

    private var restAx = ax
    private var restAy = ay
    private var restBx = bx
    private var restBy = by

    private var merged = false
    private var lastMergeTap = 0L
    private var lastTouched = false

    private var billardTicks = 0
    private var velAx = 0f
    private var velAy = 0f
    private var velBx = 0f
    private var velBy = 0f

    private val yellow = Color.rgb(235, 255, 40)
    private val pink = Color.rgb(255, 70, 205)
    private val offColor = Color.rgb(90, 96, 108)
    private val amber = Color.rgb(255, 190, 0)
    private val marker = markerFont(context)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(20, 22, 28)
        canvas.drawRect(0f, 0f, w, h, paint)
        drawZebra(canvas, w, h, d)
        drawRelief(canvas, 0f, 0f, w, h, d)

        drawMandala(canvas, w, h)

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

        paint.strokeWidth = 1f * d
        paint.color = Color.rgb(60, 66, 80)
        drawSketchyRoundRect(canvas, paint, 1f * d, 1f * d, w - 1f * d, h - 1f * d, 6f * d, 99)

        drawScrews(canvas, paint, 0f, 0f, w, h, d)

        ghostZu?.let { drawGhost(canvas, it[0], it[1], turquoiseC) }
        ghostLu?.let { drawGhost(canvas, it[0], it[1], luC) }

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

        if (merged) {
            targetBx = targetAx
            targetBy = targetAy
        }

        if (billardTicks != 0) {
            if (billardTicks > 0) billardTicks--
            targetAx += velAx; targetAy += velAy
            targetBx += velBx; targetBy += velBy
            if (targetAx < 0f) { targetAx = 0f; velAx = -velAx }
            if (targetAx > 1f) { targetAx = 1f; velAx = -velAx }
            if (targetAy < 0f) { targetAy = 0f; velAy = -velAy }
            if (targetAy > 1f) { targetAy = 1f; velAy = -velAy }
            if (targetBx < 0f) { targetBx = 0f; velBx = -velBx }
            if (targetBx > 1f) { targetBx = 1f; velBx = -velBx }
            if (targetBy < 0f) { targetBy = 0f; velBy = -velBy }
            if (targetBy > 1f) { targetBy = 1f; velBy = -velBy }
            if (billardFriction > 0) {
                velAx *= 0.99f; velAy *= 0.99f
                velBx *= 0.99f; velBy *= 0.99f
            }
            onAxisChange?.invoke(targetAx, targetAy, targetBx, targetBy)
            if (billardTicks == 0) {
                velAx = 0f; velAy = 0f; velBx = 0f; velBy = 0f
            }
        }

        drawCursor(canvas, ax * w + jax, (1f - ay) * h + jay, yellow, "A", stateA != AxisState.OFF,
            "${(ax * 100).toInt()} \u00b7 ${(ay * 100).toInt()}")
        drawCursor(canvas, bx * w + jbx, (1f - by) * h + jby, pink, "B", stateB != AxisState.OFF,
            "${(bx * 100).toInt()} \u00b7 ${(by * 100).toInt()}")

        if (merged) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f * d
            paint.color = Color.rgb(236, 224, 196)
            paint.alpha = 190
            canvas.drawCircle(ax * w + jax, (1f - ay) * h + jay, 26f * d, paint)
            paint.alpha = 255
        }

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
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    billardTicks = 0
                    val now = System.currentTimeMillis()
                    val tx = event.x
                    val ty = event.y
                    val caX = ax * width
                    val caY = (1f - ay) * height
                    val cbX = bx * width
                    val cbY = (1f - by) * height
                    val near = minOf(
                        Math.hypot((tx - caX).toDouble(), (ty - caY).toDouble()),
                        Math.hypot((tx - cbX).toDouble(), (ty - cbY).toDouble())
                    ) < 26f * d
                    if (now - lastMergeTap < 300 && near) {
                        if (merged) unfuse() else fuse()
                        lastMergeTap = 0L
                    } else {
                        lastMergeTap = now
                    }
                }
                if (fingerA == -1 && fingerB == -1) onBendTouch?.invoke(true)
                if (!(bendFreeze && bendActive)) {
                    val px = event.getX(event.actionIndex)
                    val py = event.getY(event.actionIndex)
                    val dA = Math.hypot(
                        (px - ax * width).toDouble(),
                        (py - (1f - ay) * height).toDouble()
                    )
                    val dB = Math.hypot(
                        (px - bx * width).toDouble(),
                        (py - (1f - by) * height).toDouble()
                    )
                    val grab = (30f * d).toDouble()
                    val nearA = dA < grab
                    val nearB = dB < grab
                    val firstA = when (fingerMode) {
                        1 -> false
                        2 -> px < width / 2f
                        else -> true
                    }
                    fun takeA() {
                        if (stateA == AxisState.MOM && fingerA == -1) {
                            restAx = targetAx; restAy = targetAy
                            fingerA = id
                        }
                    }
                    fun takeB() {
                        if (stateB == AxisState.MOM && fingerB == -1) {
                            restBx = targetBx; restBy = targetBy
                            fingerB = id
                        }
                    }
                    when {
                        nearA && stateA == AxisState.MOM && fingerA == -1 &&
                            (!nearB || dA <= dB) -> takeA()
                        nearB && stateB == AxisState.MOM && fingerB == -1 -> takeB()
                        nearA && stateA == AxisState.MOM && fingerA == -1 -> takeA()
                        nearA && stateA == AxisState.HOLD -> {}
                        nearB && stateB == AxisState.HOLD -> {}
                        fingerA == -1 && fingerB == -1 -> {
                            if (firstA) {
                                takeA()
                                if (fingerA == -1) takeB()
                            } else {
                                takeB()
                                if (fingerB == -1) takeA()
                            }
                        }
                        fingerA == -1 && stateA == AxisState.MOM -> takeA()
                        fingerB == -1 && stateB == AxisState.MOM -> takeB()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == fingerA) {
                    fingerA = -1
                    if (stateA == AxisState.MOM) {
                        targetAx = restAx
                        targetAy = restAy
                    }
                }
                if (id == fingerB) {
                    fingerB = -1
                    if (stateB == AxisState.MOM) {
                        targetBx = restBx
                        targetBy = restBy
                    }
                }
                if (fingerA == -1 && fingerB == -1) onBendTouch?.invoke(false)
            }
            MotionEvent.ACTION_CANCEL -> {
                fingerA = -1
                fingerB = -1
                if (stateA == AxisState.MOM) {
                    targetAx = restAx
                    targetAy = restAy
                }
                if (stateB == AxisState.MOM) {
                    targetBx = restBx
                    targetBy = restBy
                }
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
        if (bendActive) onBendPos?.invoke(targetAx, targetAy)
        onAxisChange?.invoke(targetAx, targetAy, targetBx, targetBy)

        val touched = fingerA != -1 || fingerB != -1
        if (touched != lastTouched) {
            lastTouched = touched
            onPadTouchState?.invoke(touched)
        }

        invalidate()
        return true
    }

    private fun fuse() {
        merged = true
        val mx = (ax + bx) / 2f
        val my = (ay + by) / 2f
        targetAx = mx
        targetAy = my
        targetBx = mx
        targetBy = my
    }

    private fun unfuse() {
        merged = false
        val ang = rnd.nextFloat() * 2f * Math.PI.toFloat()
        val dist = 0.12f + rnd.nextFloat() * 0.22f
        val ca = Math.cos(ang.toDouble()).toFloat()
        val sa = Math.sin(ang.toDouble()).toFloat()
        targetAx = (ax + ca * dist).coerceIn(0f, 1f)
        targetAy = (ay + sa * dist).coerceIn(0f, 1f)
        targetBx = (ax - ca * dist).coerceIn(0f, 1f)
        targetBy = (ay - sa * dist).coerceIn(0f, 1f)
        val speed = 0.012f + rnd.nextFloat() * 0.02f
        velAx = ca * speed * (0.7f + rnd.nextFloat() * 0.6f)
        velAy = sa * speed * (0.7f + rnd.nextFloat() * 0.6f)
        velBx = -ca * speed * (0.7f + rnd.nextFloat() * 0.6f)
        velBy = -sa * speed * (0.7f + rnd.nextFloat() * 0.6f)
        billardTicks = when (billardFriction) {
            0 -> -1
            1 -> 30
            2 -> 60
            3 -> 120
            else -> 240
        }
        if (billardFriction == 0) {
            velAx *= 0.7f; velAy *= 0.7f; velBx *= 0.7f; velBy *= 0.7f
        }
    }
}
