package com.example.indoornavigation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.indoornavigation.R

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
        Pair(300f, 700f),  // Near Bedroom
        Pair(400f, 400f),  // Passage turn
        Pair(600f, 150f),  // Hallway entrance
        Pair(850f, 200f)   // Inside Hall
    )

    private var userLocation: Pair<Float, Float> = Pair(150f, 750f)

    fun updateUserPosition(x: Float, y: Float) {
        userLocation = Pair(x, y)
        invalidate()
    }

    private var destination: Pair<Float, Float>? = null

    // Bitmap for destination pin
    private val pinBitmap: Bitmap

    init {
        val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_pin_green)
        pinBitmap = Bitmap.createScaledBitmap(originalBitmap, 50, 50, true)
    }

    fun setDestination(x: Float, y: Float) {
        destination = Pair(x, y)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val topOffset = 100f
        canvas.save()
        canvas.translate(0f, topOffset)

        // Draw main walls
        canvas.drawLine(100f, 800f, 100f, 100f, wallPaint)
        canvas.drawLine(100f, 100f, 1000f, 100f, wallPaint)
        canvas.drawLine(1000f, 100f, 1000f, 800f, wallPaint)
        canvas.drawLine(1000f, 800f, 100f, 800f, wallPaint)

        // Draw L-shaped passage
        canvas.drawLine(300f, 800f, 300f, 100f, wallPaint)
        canvas.drawLine(500f, 200f, 1000f, 200f, wallPaint)
        canvas.drawLine(500f, 800f, 500f, 200f, wallPaint)

        // Draw Bedroom
        canvas.drawRect(100f, 600f, 300f, 800f, wallPaint)
        canvas.drawText("Bedroom", 130f, 750f, textPaint)

        // Draw Hall
        canvas.drawRect(700f, 200f, 950f, 350f, wallPaint)
        canvas.drawText("Hall", 820f, 270f, textPaint)

        // Draw destination pin
        destination?.let { (x, y) ->
            canvas.drawBitmap(pinBitmap, x - pinBitmap.width / 2, y - pinBitmap.height, null)
        }

        // Draw beacons
        for ((x, y) in beacons) {
            canvas.drawCircle(x, y, 30f, beaconPaint)
        }

        // Draw user location
        canvas.drawCircle(userLocation.first, userLocation.second, 30f, userPaint)

        canvas.restore()
    }
}
