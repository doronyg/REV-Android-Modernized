package com.wowwee.revandroidsampleproject.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot

class PathDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val drawPath = Path()
    private val points = mutableListOf<PointF>()

    companion object {
        private const val INPUT_MIN_DISTANCE_PX = 3f
    }

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveThemeAccent()
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var drawingEnabled = true

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(drawPath, pathPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!drawingEnabled) {
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                clearPath()
                addPoint(event.x, event.y)
                drawPath.moveTo(event.x, event.y)
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) {
                    val hx = event.getHistoricalX(i)
                    val hy = event.getHistoricalY(i)
                    addPoint(hx, hy)
                }
                addPoint(event.x, event.y)
                rebuildPathFromPoints()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                addPoint(event.x, event.y)
                rebuildPathFromPoints()
                performClick()
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun clearPath() {
        points.clear()
        drawPath.reset()
        invalidate()
    }

    fun getPathPoints(): List<PointF> {
        return points.map { PointF(it.x, it.y) }
    }

    fun getPlaybackPoints(sampleDistancePx: Float): List<PointF> {
        if (points.size < 2) {
            return emptyList()
        }
        val smoothed = smoothPoints(points)
        return resampleUniform(smoothed, sampleDistancePx.coerceAtLeast(4f))
    }

    fun setDrawingEnabled(enabled: Boolean) {
        drawingEnabled = enabled
    }

    private fun addPoint(x: Float, y: Float) {
        if (points.isEmpty()) {
            points.add(PointF(x, y))
            return
        }

        val last = points.last()
        if (abs(last.x - x) < INPUT_MIN_DISTANCE_PX && abs(last.y - y) < INPUT_MIN_DISTANCE_PX) {
            return
        }

        points.add(PointF(x, y))
    }

    private fun rebuildPathFromPoints() {
        drawPath.reset()
        if (points.isEmpty()) {
            invalidate()
            return
        }

        drawPath.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val current = points[i]
            val midX = (prev.x + current.x) / 2f
            val midY = (prev.y + current.y) / 2f
            drawPath.quadTo(prev.x, prev.y, midX, midY)
        }
        val last = points.last()
        drawPath.lineTo(last.x, last.y)
        invalidate()
    }

    private fun smoothPoints(source: List<PointF>): List<PointF> {
        if (source.size < 3) {
            return source.map { PointF(it.x, it.y) }
        }

        val out = MutableList(source.size) { PointF() }
        out[0] = PointF(source[0].x, source[0].y)
        out[source.lastIndex] = PointF(source.last().x, source.last().y)

        for (i in 1 until source.lastIndex) {
            val prev = source[i - 1]
            val cur = source[i]
            val next = source[i + 1]
            out[i] = PointF(
                (prev.x + cur.x * 2f + next.x) / 4f,
                (prev.y + cur.y * 2f + next.y) / 4f
            )
        }

        return out
    }

    private fun resampleUniform(source: List<PointF>, spacing: Float): List<PointF> {
        if (source.size < 2) {
            return source
        }

        val sampled = mutableListOf(PointF(source[0].x, source[0].y))
        var previous = PointF(source[0].x, source[0].y)
        var carryDistance = 0f

        for (i in 1 until source.size) {
            var segmentStart = previous
            val segmentEnd = source[i]
            var dx = segmentEnd.x - segmentStart.x
            var dy = segmentEnd.y - segmentStart.y
            var segmentLength = hypot(dx.toDouble(), dy.toDouble()).toFloat()

            if (segmentLength <= 0f) {
                previous = segmentEnd
                continue
            }

            while (carryDistance + segmentLength >= spacing) {
                val remainingToSample = spacing - carryDistance
                val t = (remainingToSample / segmentLength).coerceIn(0f, 1f)
                val nx = segmentStart.x + dx * t
                val ny = segmentStart.y + dy * t
                val nextPoint = PointF(nx, ny)
                sampled.add(nextPoint)

                segmentStart = nextPoint
                dx = segmentEnd.x - segmentStart.x
                dy = segmentEnd.y - segmentStart.y
                segmentLength = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                carryDistance = 0f

                if (segmentLength <= 0f) {
                    break
                }
            }

            carryDistance += segmentLength
            previous = segmentEnd
        }

        val last = source.last()
        val sampledLast = sampled.last()
        if (abs(sampledLast.x - last.x) > 0.5f || abs(sampledLast.y - last.y) > 0.5f) {
            sampled.add(PointF(last.x, last.y))
        }

        return sampled
    }

    private fun resolveThemeAccent(): Int {
        val outValue = TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.colorAccent, outValue, true)) {
            outValue.data
        } else {
            Color.parseColor("#03A9F4")
        }
    }
}





