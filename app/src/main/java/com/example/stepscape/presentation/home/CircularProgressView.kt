package com.example.stepscape.presentation.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.stepscape.R


class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Float = 0f
    private val maxProgress: Float = 100f
    
    private val strokeWidth = 38f
    
    private val backgroundPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = this@CircularProgressView.strokeWidth
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.progress_track)
        strokeCap = Paint.Cap.ROUND
    }
    
    private val progressPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = this@CircularProgressView.strokeWidth
        isAntiAlias = true
        color = ContextCompat.getColor(context, R.color.progress_orange)
        strokeCap = Paint.Cap.ROUND
    }
    
    private val rectF = RectF()
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val padding = strokeWidth / 2 + 24f
        rectF.set(
            padding,
            padding,
            width - padding,
            height - padding
        )
        
        canvas.drawArc(rectF, 0f, 360f, false, backgroundPaint)
        
        val sweepAngle = (progress / maxProgress) * 360f
        canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
    }
    

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, maxProgress)
        invalidate()
    }
    

    fun getProgress(): Float = progress
}
