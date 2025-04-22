package com.example.indoornavigation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*

class CustomCampusMapView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val wallPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private val roomLabelPaint = Paint().apply {
        color = Color.BLACK
        textSize = 36f
        isAntiAlias = true
    }

    private val beaconPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val userPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private val destinationPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val pathPaint = Paint().apply {
        color = Color.MAGENTA
        strokeWidth = 10f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var userLocation: Pair<Float, Float> = Pair(375f, 200f)
    private var destinationRoom: String? = null
    private var currentPath: List<Pair<Float, Float>> = emptyList()

    private val beacons = listOf(
        Pair(300f, 250f), Pair(300f, 750f), Pair(300f, 1200f),
        Pair(450f, 200f), Pair(450f, 400f), Pair(450f, 600f),
        Pair(450f, 800f), Pair(450f, 1250f), Pair(800f, 1050f), Pair(600f, 975f)
    )

    // Graph nodes and edges (walkable path)
    private val graphNodes = listOf(
        // Vertical corridor
        Pair(375f, 100f), Pair(375f, 250f), Pair(375f, 400f), Pair(375f, 600f),
        Pair(375f, 750f), Pair(375f, 900f), Pair(375f, 975f), Pair(375f, 1050f),
        Pair(375f, 1200f), Pair(375f, 1400f),

        // Horizontal corridor (L-shape turn)
        Pair(600f, 975f), Pair(800f, 975f), Pair(1000f, 975f)
    )



    private val graphEdges = mapOf(
        // Vertical corridor
        Pair(375f, 100f) to listOf(Pair(375f, 250f)),
        Pair(375f, 250f) to listOf(Pair(375f, 100f), Pair(375f, 400f)),
        Pair(375f, 400f) to listOf(Pair(375f, 250f), Pair(375f, 600f)),
        Pair(375f, 600f) to listOf(Pair(375f, 400f), Pair(375f, 750f)),
        Pair(375f, 750f) to listOf(Pair(375f, 600f), Pair(375f, 900f)),
        Pair(375f, 900f) to listOf(Pair(375f, 750f), Pair(375f, 975f)),
        Pair(375f, 975f) to listOf(Pair(375f, 900f), Pair(375f, 1050f), Pair(600f, 975f)),
        Pair(375f, 1050f) to listOf(Pair(375f, 975f), Pair(375f, 1200f)),
        Pair(375f, 1200f) to listOf(Pair(375f, 1050f), Pair(375f, 1400f)),
        Pair(375f, 1400f) to listOf(Pair(375f, 1200f)),

        // Horizontal corridor
        Pair(600f, 975f) to listOf(Pair(375f, 975f), Pair(800f, 975f)),
        Pair(800f, 975f) to listOf(Pair(600f, 975f), Pair(1000f, 975f)),
        Pair(1000f, 975f) to listOf(Pair(800f, 975f))
    )


    // Room entrance connection to path
    private val roomToPath = mapOf(
        "A3-207" to Pair(300f, 250f),
        "A3-208" to Pair(300f, 750f),
        "A3-209" to Pair(300f, 1200f),
        "A3-206" to Pair(450f, 200f),
        "A3-205" to Pair(450f, 400f),
        "A3-204" to Pair(450f, 600f),
        "A3-203" to Pair(450f, 800f),
        "A3-202" to Pair(450f, 1250f),
        "Reading Hall" to Pair(800f, 1050f)
    )

    fun updateUserPosition(x: Float, y: Float) {
        // Snap user position to nearest node to make it visually aligned with path
        val snappedNode = findNearestNode(Pair(x, y))
        userLocation = snappedNode

        updatePath()
        invalidate()
    }


    fun setDestination(room: String) {
        destinationRoom = room
        updatePath()
        invalidate()
    }

    private fun updatePath() {
        val start = findNearestNode(userLocation)
        val roomEntrance = roomToPath[destinationRoom] ?: return
        val endNode = findNearestNode(roomEntrance)

        val basePath = findShortestPath(start, endNode)
        if (basePath.isNotEmpty()) {
            currentPath = basePath.toMutableList().apply {
                if (last() != roomEntrance) add(roomEntrance)
            }
        } else {
            currentPath = listOf()  // Clear if no path found
        }
    }



    private fun findNearestNode(point: Pair<Float, Float>): Pair<Float, Float> {
        return graphNodes.minByOrNull { dist(it, point) } ?: point
    }

    private fun findShortestPath(start: Pair<Float, Float>, end: Pair<Float, Float>): List<Pair<Float, Float>> {
        val queue: Queue<List<Pair<Float, Float>>> = LinkedList()
        val visited = mutableSetOf<Pair<Float, Float>>()
        queue.add(listOf(start))

        while (queue.isNotEmpty()) {
            val path = queue.poll()
            val node = path.last()
            if (node == end) return path
            if (node !in visited) {
                visited.add(node)
                graphEdges[node]?.forEach {
                    val newPath = path.toMutableList()
                    newPath.add(it)
                    queue.add(newPath)
                }
            }
        }
        return emptyList()
    }

    private fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
        return (a.first - b.first).let { dx ->
            (a.second - b.second).let { dy ->
                kotlin.math.sqrt(dx * dx + dy * dy)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        // Walls and layout
        canvas.drawLine(100f, 1400f, 100f, 100f, wallPaint)
        canvas.drawLine(100f, 100f, 1000f, 100f, wallPaint)
        canvas.drawLine(1000f, 100f, 1000f, 1400f, wallPaint)
        canvas.drawLine(1000f, 1400f, 100f, 1400f, wallPaint)

        canvas.drawLine(300f, 1400f, 300f, 100f, wallPaint)
        canvas.drawLine(450f, 1400f, 450f, 1050f, wallPaint)
        canvas.drawLine(450f, 1050f, 1000f, 1050f, wallPaint)
        canvas.drawLine(450f, 900f, 1000f, 900f, wallPaint)
        canvas.drawLine(450f, 900f, 450f, 100f, wallPaint)

        canvas.drawLine(100f, 1000f, 300f, 1000f, wallPaint)
        canvas.drawLine(100f, 500f, 300f, 500f, wallPaint)
        canvas.drawLine(100f, 100f, 300f, 100f, wallPaint)

        canvas.drawLine(650f, 100f, 650f, 900f, wallPaint)
        canvas.drawLine(450f, 700f, 650f, 700f, wallPaint)
        canvas.drawLine(450f, 500f, 650f, 500f, wallPaint)
        canvas.drawLine(450f, 300f, 650f, 300f, wallPaint)

        canvas.drawLine(650f, 1050f, 650f, 1400f, wallPaint)

        // Room labels
        val roomLabels = listOf(
            Triple("A3-207", 100f, 350f), Triple("A3-208", 100f, 750f), Triple("A3-209", 100f, 1250f),
            Triple("A3-206", 500f, 200f), Triple("A3-205", 500f, 400f), Triple("A3-204", 500f, 600f),
            Triple("A3-203", 500f, 800f), Triple("A3-202", 500f, 1250f), Triple("Reading Hall (RH)", 680f, 1250f),
            Triple("Lawn", 780f, 500f)
        )
        for ((name, x, y) in roomLabels) {
            canvas.drawText(name, x, y, roomLabelPaint)
        }

        // Beacons
        for ((x, y) in beacons) {
            canvas.drawCircle(x, y, 30f, beaconPaint)
        }

        // Draw user
        canvas.drawCircle(userLocation.first, userLocation.second, 25f, userPaint)

        // Draw destination
        destinationRoom?.let { room ->
            roomToPath[room]?.let { (x, y) ->
                canvas.drawCircle(x, y, 20f, destinationPaint)
            }
        }

        // Draw path
        for (i in 0 until currentPath.size - 1) {
            val (x1, y1) = currentPath[i]
            val (x2, y2) = currentPath[i + 1]
            canvas.drawLine(x1, y1, x2, y2, pathPaint)
        }

        canvas.restore()
    }


}
