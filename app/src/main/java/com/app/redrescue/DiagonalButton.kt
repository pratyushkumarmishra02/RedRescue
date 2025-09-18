package com.app.redrescue

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Handler
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import kotlin.apply

class DiagonalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatButton(context, attrs) {

    private lateinit var flashPaint: Paint
    private var lineProgress = -0.2f
    private val handler = Handler()
    private lateinit var animationRunnable: Runnable

    init {
        initialize()
    }

    private fun initialize() {
        // Paint for the moving flashlight effect
        flashPaint = Paint().apply {
            strokeWidth = 30f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        // Animation logic
        animationRunnable = Runnable {
            lineProgress += 0.03f
            if (lineProgress > 1.5f) {
                lineProgress = -0.5f
            }
            invalidate()
            handler.postDelayed(animationRunnable, 35)
        }
        handler.post(animationRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Calculate diagonal line coordinates
        val startX = lineProgress * width
        val startY = -height * 0.5f
        val endX = startX - (height * 2.0f)
        val endY = height * 1.5f

        // Create gradient shader
        flashPaint.shader = LinearGradient(
            startX, startY, endX, endY,
            intArrayOf(0x00FFFFFF, 0xFFFFFFFF.toInt(), 0x00FFFFFF),
            floatArrayOf(0.2f, 0.5f, 0.8f),
            Shader.TileMode.CLAMP
        )

        // Draw the diagonal line
        canvas.drawLine(startX, startY, endX, endY, flashPaint)
    }
}
