package com.example.monitorapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View

class UserTrajectory @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }


    private val path = Path()
    private val trajectoryBounds = RectF()

    fun addPoint(x: Float, y: Float) {
        Log.i("ADD POINT", "$x,$y")
        path.lineTo(x, y)
        trajectoryBounds.union(x, y)
        requestLayout()
        invalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val trajectoryBounds = RectF()
        path.computeBounds(trajectoryBounds, true)
        val canvasWidth = width - paddingLeft - paddingRight
        val canvasHeight = height - paddingTop - paddingBottom
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val scale = 5f // Adjust the scaling factor here
        canvas.translate(centerX, centerY)
        canvas.scale(scale, scale)
        canvas.drawPath(path, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }
}

