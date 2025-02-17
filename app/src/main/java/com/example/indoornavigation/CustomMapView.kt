package com.example.indoornavigation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CustomMapView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paintBeacon = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val paintUser = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private val paintGrid = Paint().apply {
        color = Color.GRAY
        strokeWidth = 4f
    }

    private var userX: Float = -1f // Default, means not set
    private var userY: Float = -1f // Default, means not set

    // **Define Beacon Positions in Map Coordinates**
    private val beacons = listOf(
        Pair(0f, 2f),
        Pair(0f, 5f),
        Pair(0f, 8f)
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // **Draw Grid Line for Passage**
        canvas.drawLine(width / 2, 0f, width / 2, height, paintGrid)

        // **Draw Beacons as Red Dots**
        for ((x, y) in beacons) {
            canvas.drawCircle(width / 2, height - (y / 10) * height, 20f, paintBeacon)
        }

        // **Draw User as Blue Dot**
        if (userX != -1f && userY != -1f) {
            canvas.drawCircle(userX, userY, 25f, paintUser)
        }
    }

    // **Update User Position and Refresh View**
    fun updateUserPosition(x: Double, y: Double) {
        val width = width.toFloat()
        val height = height.toFloat()

        userX = width / 2 // Always center X-axis
        userY = height - (y.toFloat() / 10) * height // Scale Y-axis from (0,0) to (0,10)

        invalidate() // Redraw view to show updates
    }
}
