package com.zulu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

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
    private var mandalaMot = 0f
    private val mandalaPath = Path()

    var bgArt = 1
        set(value) {
            field = value
            kaleidoBmp?.eraseColor(Color.TRANSPARENT)
        }

    private var kaleidoBmp: Bitmap? = null
    private var kaleidoCv: Canvas? = null

    fun setAudio(lvl: Float, aggr: Float) {
        mandalaLvl += 0.10f * (lvl.coerceIn(0f, 1f) - mandalaLvl)
        mandalaAggr += 0.06f * (aggr.coerceIn(0f, 1f) - mandalaAggr)
        mandalaMot *= 0.94f
        mandalaPh += 0.0012f + mandalaLvl * 0.003f + mandalaMot * 0.006f
        if (mandalaPh >= 1f) mandalaPh -= 1f
        if (bgArt != 0 && (mandalaLvl > 0.004f || mandalaAggr > 0.004f || mandalaMot > 0.004f))
            postInvalidateOnAnimation()
    }

    fun feedMotion(dx: Float, dy: Float) {
        val m = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        mandalaMot = (mandalaMot + m * 1.5f).coerceIn(0f, 1f)
        if (bgArt != 0) postInvalidateOnAnimation()
    }

    private fun acidCol(ag: Float): Int {
        val stops = arrayOf(
            intArrayOf(0, 229, 255), intArrayOf(255, 60, 220),
            intArrayOf(180, 255, 40), intArrayOf(255, 190, 40)
        )
        val p = mandalaPh * 4f
        val i = p.toInt() % 4
        val j = (i + 1) % 4
        val f = p - p.toInt().toFloat()
        var r = (stops[i][0] + (stops[j][0] - stops[i][0]) * f).toInt()
        var g = (stops[i][1] + (stops[j][1] - stops[i][1]) * f).toInt()
        val b = (stops[i][2] + (stops[j][2] - stops[i][2]) * f).toInt()
        r += ((255 - r) * ag).toInt()
        g = (g * (1f - ag * 0.55f)).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private fun kaleidoSeg(x0: Float, y0: Float, x1: Float, y1: Float) {
        if (bgArt !in 1..3) return
        val w = width
        val h = height
        if (w < 2 || h < 2) return
        if (kaleidoBmp == null || kaleidoBmp!!.width != w || kaleidoBmp!!.height != h) {
            kaleidoBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            kaleidoCv = Canvas(kaleidoBmp!!)
        }
        val kc = kaleidoCv ?: return
        val cx = w / 2f
        val cy = h / 2f
        val col = acidCol(mandalaAggr.coerceIn(0f, 1f))
        val sp = Math.sqrt(((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)).toDouble()).toFloat()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (ZuluTheme.retro) 3.5f * d else 2f * d
        paint.color = col
        paint.alpha = (50 + sp * 3f).toInt().coerceIn(50, 190)
        val dist2 = (x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)
        for (s in 0 until 8) {
            val ang = s * 0.7853982f
            val ca = Math.cos(ang.toDouble()).toFloat()
            val sa = Math.sin(ang.toDouble()).toFloat()
            for (mir in 0 until 2) {
                val ax0 = x0 - cx
                val ay0 = if (mir == 0) y0 - cy else cy - y0
                val ax1 = x1 - cx
                val ay1 = if (mir == 0) y1 - cy else cy - y1
                val rx0 = ax0 * ca - ay0 * sa + cx
                val ry0 = ax0 * sa + ay0 * ca + cy
                val rx1 = ax1 * ca - ay1 * sa + cx
                val ry1 = ax1 * sa + ay1 * ca + cy
                if (dist2 < 4f) kc.drawCircle(rx1, ry1, 1.6f * d, paint)
                else kc.drawLine(rx0, ry0, rx1, ry1, paint)
            }
        }
        paint.alpha = 255
        paint.style = Paint.Style.FILL
    }

    private fun drawKaleido(canvas: Canvas) {
        if (bgArt !in 1..3) return
        val w = width
        val h = height
        if (w < 2 || h < 2) return
        if (kaleidoBmp == null || kaleidoBmp!!.width != w || kaleidoBmp!!.height != h) {
            kaleidoBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            kaleidoCv = Canvas(kaleidoBmp!!)
        }
        val bmp = kaleidoBmp ?: return
        val fade = intArrayOf(0, 1, 4, 12)[bgArt]
        if (fade > 0) {
            val kc = kaleidoCv ?: return
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.alpha = fade
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            kc.drawPaint(paint)
            paint.xfermode = null
            paint.alpha = 255
        }
        paint.style = Paint.Style.FILL
        paint.alpha = 210
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        paint.alpha = 255
    }

    private fun drawMandala(c: Canvas, w: Float, h: Float) {
        if (bgArt != 4) return
        mandalaMot *= 0.97f
        val cx = w / 2f
        val cy = h / 2f
        val baseR = minOf(w, h) * 0.46f
        val amp = 0.06f + mandalaLvl * 0.24f + mandalaMot * 0.18f
        val ag = (mandalaAggr + mandalaMot * 0.4f).coerceIn(0f, 1f)
        val col = acidCol(ag)
        paint.color = col
        paint.style = Paint.Style.FILL
        paint.alpha = (10 + ((mandalaLvl + mandalaMot * 0.5f) * 26f).toInt()).coerceIn(0, 44)
        c.drawCircle(cx, cy, baseR * 1.08f, paint)
        paint.alpha = (14 + ((mandalaLvl + mandalaMot * 0.5f) * 30f).toInt()).coerceIn(0, 52)
        c.drawCircle(cx, cy, baseR * 0.64f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f * d
        val petalSets = intArrayOf(12, 9, 16, 6)
        for (layer in 0 until 4) {
            val dir = if (layer % 2 == 0) 1f else -1f
            val petals = petalSets[layer]
            val rot = mandalaPh * dir * 6.28318f + layer * 0.41f
            val rr = baseR * (0.34f + layer * 0.17f) * (1f + mandalaLvl * 0.10f * (layer + 1))
            paint.alpha = (26 + layer * 8 + (mandalaLvl * 46f).toInt()).coerceIn(0, 110)
            mandalaPath.reset()
            val segs = petals * 10
            for (i in 0..segs) {
                val t = i / segs.toFloat()
                val a = rot + t * 6.28318f
                val wob = Math.sin((t * petals + layer * 1.7f + mandalaPh * 5f * dir) * 6.28318).toFloat()
                val wob2 = Math.sin((t * (petals / 2 + 2) + layer * 2.3f + mandalaPh * 3f * dir) * 6.28318).toFloat()
                var rad = rr * (1f + amp * (0.7f + 0.3f * ag) * wob + amp * 0.4f * wob2)
                rad *= 1f + ag * 0.07f * Math.abs(Math.sin(t * petals * 3.14159)).toFloat()
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
    var shiftCheck: (() -> Boolean)? = null
    var onFrictionCycle: (() -> Unit)? = null
    var onBendPos: ((Float, Float) -> Unit)? = null

    private val rnd = java.util.Random()
    private var glitchClock = 0

    private var targetAx = ax
    private var targetAy = ay
    private var targetBx = bx
    private var targetBy = by

    private var fingerA = -1
    private var fingerB = -1


    private var merged = false
    private var lastMergeTap = 0L
    private var dAx = 0f
    private var dAy = 0f
    private var dBx = 0f
    private var dBy = 0f
    private var lastTouched = false
    private var pinchInitSpread = 0f
    private var pinchFused = false
    private var pinchHoldStart = 0L

    private var billardTicks = 0
    private var velAx = 0f
    private var velAy = 0f
    private var velBx = 0f
    private var velBy = 0f

    private val verStr = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) { "?" }

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

        if (bgArt == 4) drawMandala(canvas, w, h) else drawKaleido(canvas)

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

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(165, 172, 185)
        paint.alpha = 110
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = marker
        paint.textSize = 8f * d
        canvas.drawText("ZULU v$verStr", 6f * d, h - 6f * d, paint)
        paint.alpha = 255

        drawScrews(canvas, paint, 0f, 0f, w, h, d)

        ghostZu?.let { drawGhost(canvas, it[0], it[1], turquoiseC) }
        ghostLu?.let { drawGhost(canvas, it[0], it[1], luC) }

        val fA = if (fingerA != -1) 1f else 0.4f
        val fB = if (fingerB != -1) 1f else 0.4f
        ax += (targetAx - ax) * fA
        ay += (targetAy - ay) * fA
        bx += (targetBx - bx) * fB
        by += (targetBy - by) * fB

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
            val pax = targetAx * width
            val pay = (1f - targetAy) * height
            val pbx = targetBx * width
            val pby = (1f - targetBy) * height
            targetAx += velAx; targetAy += velAy
            targetBx += velBx; targetBy += velBy
            kaleidoSeg(pax, pay, targetAx * width, (1f - targetAy) * height)
            kaleidoSeg(pbx, pby, targetBx * width, (1f - targetBy) * height)
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
                if (maxOf(abs(velAx), abs(velAy), abs(velBx), abs(velBy)) < 0.0008f) {
                    velAx = 0f; velAy = 0f; velBx = 0f; velBy = 0f
                    billardTicks = 0
                }
            }
            onAxisChange?.invoke(targetAx, targetAy, targetBx, targetBy)
            if (billardTicks == 0) {
                velAx = 0f; velAy = 0f; velBx = 0f; velBy = 0f
            }
        }

        drawCursor(canvas, ax * w + jax, (1f - ay) * h + jay, yellow, "F", stateA != AxisState.OFF,
            "${(ax * 100).toInt()} \u00b7 ${(ay * 100).toInt()}")
        drawCursor(canvas, bx * w + jbx, (1f - by) * h + jby, pink, "Z", stateB != AxisState.OFF,
            "${(bx * 100).toInt()} \u00b7 ${(by * 100).toInt()}")

        if (merged) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f * d
            paint.color = Color.rgb(236, 224, 196)
            paint.alpha = 190
            canvas.drawCircle(ax * w + jax, (1f - ay) * h + jay, 26f * d, paint)
            paint.alpha = 255
        }

        if (ZuluTheme.retro) drawScanlines(canvas, w, h)

        postInvalidateOnAnimation()
    }

    private fun drawScanlines(c: Canvas, w: Float, h: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.alpha = 26
        var y = 0f
        while (y < h) {
            c.drawRect(0f, y, w, y + 1.5f * d, paint)
            y += 4f * d
        }
        paint.alpha = 255
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
                    if (now - lastMergeTap < 450 && shiftCheck?.invoke() == true) {
                        lastMergeTap = 0L
                        onFrictionCycle?.invoke()
                        return true
                    }
                    val tx = event.x
                    val ty = event.y
                    val caX = ax * width
                    val caY = (1f - ay) * height
                    val cbX = bx * width
                    val cbY = (1f - by) * height
                    val near = minOf(
                        Math.hypot((tx - caX).toDouble(), (ty - caY).toDouble()),
                        Math.hypot((tx - cbX).toDouble(), (ty - cbY).toDouble())
                    ) < 44f * d ||
                        (abs(caX - cbX) < 60f * d && abs(caY - cbY) < 60f * d &&
                            Math.hypot((tx - (caX + cbX) / 2.0).toDouble(), (ty - (caY + cbY) / 2.0).toDouble()) < 70f * d)
                    if (now - lastMergeTap < 450 && near && merged) {
                        unfuse()
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
                    val candA = stateA == AxisState.MOM && fingerA == -1
                    val candB = stateB == AxisState.MOM && fingerB == -1
                    fun takeA() {
                        if (candA) fingerA = id
                    }
                    fun takeB() {
                        if (candB) fingerB = id
                    }
                    when {
                        candA && candB -> when (fingerMode) {
                            1 -> if (dA <= dB) takeB() else takeA()
                            2 -> if (px < width / 2f) takeA() else takeB()
                            else -> if (dA <= dB) takeA() else takeB()
                        }
                        candA -> takeA()
                        candB -> takeB()
                    }
                }
                checkPinchFuse(event)
            }
            MotionEvent.ACTION_MOVE -> checkPinchFuse(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == fingerA) {
                    fingerA = -1
                    startGlide(true, dAx, dAy)
                }
                if (id == fingerB) {
                    fingerB = -1
                    startGlide(false, dBx, dBy)
                }
                if (fingerA == -1 && fingerB == -1) onBendTouch?.invoke(false)
                if (event.pointerCount <= 1) {
                    pinchInitSpread = 0f
                    pinchFused = false
                    pinchHoldStart = 0L
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                fingerA = -1
                fingerB = -1
                onBendTouch?.invoke(false)
                pinchInitSpread = 0f
                pinchFused = false
                pinchHoldStart = 0L
            }
        }

        val idxA = event.findPointerIndex(fingerA)
        val idxB = event.findPointerIndex(fingerB)

        if (idxA >= 0) {
            val nx = (event.getX(idxA) / width).coerceIn(0f, 1f)
            val ny = (1f - event.getY(idxA) / height).coerceIn(0f, 1f)
            dAx = nx - targetAx
            dAy = ny - targetAy
            feedMotion(dAx, dAy)
            kaleidoSeg(targetAx * width, (1f - targetAy) * height, nx * width, (1f - ny) * height)
            targetAx = nx
            targetAy = ny
        }
        if (idxB >= 0) {
            val nx = (event.getX(idxB) / width).coerceIn(0f, 1f)
            val ny = (1f - event.getY(idxB) / height).coerceIn(0f, 1f)
            dBx = nx - targetBx
            dBy = ny - targetBy
            feedMotion(dBx, dBy)
            kaleidoSeg(targetBx * width, (1f - targetBy) * height, nx * width, (1f - ny) * height)
            targetBx = nx
            targetBy = ny
        }
        if (merged) {
            targetBx = targetAx
            targetBy = targetAy
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

    private fun checkPinchFuse(event: MotionEvent) {
        if (pinchFused || merged) return
        if (event.pointerCount < 2) {
            pinchInitSpread = 0f
            pinchHoldStart = 0L
            return
        }
        var mx = 0f
        for (i in 0 until event.pointerCount) for (j in i + 1 until event.pointerCount) {
            mx = maxOf(
                mx,
                Math.hypot(
                    (event.getX(i) - event.getX(j)).toDouble(),
                    (event.getY(i) - event.getY(j)).toDouble()
                ).toFloat()
            )
        }
        if (pinchInitSpread == 0f) {
            pinchInitSpread = mx
            pinchHoldStart = 0L
            return
        }
        if (pinchInitSpread > 50f * d && mx < pinchInitSpread * 0.62f) {
            val now = System.currentTimeMillis()
            if (pinchHoldStart == 0L) {
                pinchHoldStart = now
            } else if (now - pinchHoldStart >= 1000) {
                if (!merged) fuse()
                pinchFused = true
                pinchInitSpread = 0f
                pinchHoldStart = 0L
                lastMergeTap = 0L
            }
        } else {
            if (mx > pinchInitSpread * 0.85f) pinchInitSpread = mx
            pinchHoldStart = 0L
        }
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

    private fun startGlide(isA: Boolean, vx: Float, vy: Float) {
        val sp = Math.hypot(vx.toDouble(), vy.toDouble()).toFloat()
        if (sp < 0.012f) return
        val cx = vx.coerceIn(-0.045f, 0.045f)
        val cy = vy.coerceIn(-0.045f, 0.045f)
        if (isA) {
            velAx = cx; velAy = cy
        } else {
            velBx = cx; velBy = cy
        }
        if (merged) {
            velBx = velAx; velBy = velAy
        }
        billardTicks = when (billardFriction) {
            0 -> -1
            1 -> 30
            2 -> 60
            3 -> 120
            else -> 240
        }
    }

    private fun unfuse() {
        merged = false
        val ang = rnd.nextFloat() * 2f * Math.PI.toFloat()
        val dist = 0.16f + rnd.nextFloat() * 0.30f
        val ca = Math.cos(ang.toDouble()).toFloat()
        val sa = Math.sin(ang.toDouble()).toFloat()
        targetAx = (ax + ca * dist).coerceIn(0f, 1f)
        targetAy = (ay + sa * dist).coerceIn(0f, 1f)
        targetBx = (ax - ca * dist).coerceIn(0f, 1f)
        targetBy = (ay - sa * dist).coerceIn(0f, 1f)
        val speed = 0.024f + rnd.nextFloat() * 0.036f
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
