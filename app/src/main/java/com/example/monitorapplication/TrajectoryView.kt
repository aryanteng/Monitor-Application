package com.example.monitorapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View

class TrajectoryView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 10f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val path = Path()
    private var minWidth = 0f
    private var minHeight = 0f
    private var scaleFactor = 1f

    fun addPoint(x: Float, y: Float) {
        Log.i("ADD POINT", "$x,$y")
        path.lineTo(x, y)
        minWidth += x
        minHeight += y
        requestLayout()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        val scale = minOf(
            (width - paddingLeft - paddingRight) / canvas.width,
            (height - paddingTop - paddingBottom) / canvas.height
        )
        val scaledWidth = canvas.width * scale
        val scaledHeight = canvas.height * scale
        val dx = (width - scaledWidth) / 2
        val dy = (height - scaledHeight) / 2
        canvas.translate(dx.toFloat(), dy.toFloat())
        canvas.scale(scale.toFloat(), scale.toFloat())
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSizeAndState(
            minWidth.toInt() + paddingLeft + paddingRight,
            widthMeasureSpec,
            0
        )
        val height = resolveSizeAndState(
            minHeight.toInt() + paddingTop + paddingBottom,
            heightMeasureSpec,
            0
        )
        setMeasuredDimension(width, height)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceAtLeast(0.1f).coerceAtMost(5.0f)
            invalidate()
            return true
        }
    }
}
