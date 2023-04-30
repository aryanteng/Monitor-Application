package com.example.monitorapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class TrajectoryView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val path = Path()

    fun addPoint(x: Float, y: Float) {
        path.lineTo(x, y)
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawPath(path, paint)
    }

}
