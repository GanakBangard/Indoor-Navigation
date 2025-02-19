package com.example.indoornavigation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CustomMapView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val wallPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private val beaconPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val userPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.BLUE
        textSize = 40f
    }

    private val beacons = listOf(
        Pair(200f, 700f),  // Near Bedroom
        Pair(400f, 500f),  // Passage turn
        Pair(700f, 300f),  // Hallway entrance
        Pair(900f, 200f)   // Inside Hall
    )

    private var userLocation: Pair<Float, Float> = Pair(150f, 750f)

    fun updateUserPosition(x: Float, y: Float) {
        userLocation = Pair(x, y)
        invalidate() // Redraw the view with updated user location
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw main walls
        canvas.drawLine(100f, 800f, 100f, 100f, wallPaint) // Left wall
        canvas.drawLine(100f, 100f, 1000f, 100f, wallPaint) // Top wall
        canvas.drawLine(1000f, 100f, 1000f, 800f, wallPaint) // Right wall
        canvas.drawLine(1000f, 800f, 100f, 800f, wallPaint) // Bottom wall

        // Draw L-shaped passage
        canvas.drawLine(400f, 800f, 400f, 500f, wallPaint)
        canvas.drawLine(400f, 500f, 700f, 500f, wallPaint)
        canvas.drawLine(700f, 500f, 700f, 300f, wallPaint)

        // Draw Bedroom
        canvas.drawRect(100f, 600f, 300f, 800f, wallPaint)
        canvas.drawText("Bedroom", 130f, 750f, textPaint)

        // Draw Hall
        canvas.drawRect(800f, 200f, 950f, 350f, wallPaint)
        canvas.drawText("Hall", 820f, 270f, textPaint)

        // Draw beacons as red circles
        for ((x, y) in beacons) {
            canvas.drawCircle(x, y, 30f, beaconPaint)
        }

        // Draw user location as blue circle
        canvas.drawCircle(userLocation.first, userLocation.second, 30f, userPaint)
    }
}