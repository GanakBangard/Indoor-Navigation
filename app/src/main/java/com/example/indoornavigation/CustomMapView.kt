package com.example.indoornavigation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CustomMapView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var userX: Float = -1f
    private var userY: Float = -1f

    fun updateUserPosition(newX: Double, newY: Double) {
        userX = newX.toFloat()
        userY = newY.toFloat()
        invalidate() // Refresh view with new position
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // **Draw Vertical Passage (Hallway)**
        paint.color = Color.LTGRAY
        val passageLeft = width * 0.4f
        val passageRight = width * 0.6f
        canvas.drawRect(passageLeft, 0f, passageRight, height, paint)

        // **Draw Beacons + Numbers**
        paint.color = Color.RED
        val beaconRadius = 20f
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
        }

        val beaconPositions = listOf(
            Pair(width / 2, height * 0.1f), // Beacon 1 (Top)
            Pair(width / 2, height * 0.5f), // Beacon 2 (Middle)
            Pair(width / 2, height * 0.9f)  // Beacon 3 (Bottom)
        )

        beaconPositions.forEachIndexed { index, (x, y) ->
            canvas.drawCircle(x, y, beaconRadius, paint)
            canvas.drawText("${index + 1}", x + 30f, y, textPaint) // Draw beacon number
        }

        // **Draw User (Blue Dot)**
        if (userX != -1f && userY != -1f) {
            paint.color = Color.BLUE
            val userRadius = 25f

            // Keep user within map bounds
            val clampedX = userX.coerceIn(passageLeft - 20f, passageRight + 20f)
            val clampedY = userY.coerceIn(0f, height)

            canvas.drawCircle(clampedX, clampedY, userRadius, paint)
        }
    }

}
