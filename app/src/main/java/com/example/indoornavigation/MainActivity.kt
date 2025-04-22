package com.example.indoornavigation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import kotlin.math.pow
import com.example.indoornavigation.CustomCampusMapView

class MainActivity : AppCompatActivity() {

    private lateinit var customMapView: CustomCampusMapView
    private lateinit var userLocationText: TextView
    private lateinit var errorMessage: TextView
    private lateinit var scanButton: Button
    private lateinit var roomSelector: Spinner

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanHandler = Handler(Looper.getMainLooper())
    private val scanResults = mutableMapOf<String, Int>()

    private val verifiedBeacons = listOf(
        Triple("10:BC:97:BC:18:4A", 300f, 250f),
        Triple("00:45:E2:81:A2:5E", 300f, 750f),
        Triple("00:45:E2:A6:5F:9E", 300f, 1250f),
        Triple("74:4C:A1:7A:D1:36", 1050F, 800f)
    )

    private val bluetoothPermissions = arrayOf(
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val PERMISSION_REQUEST_CODE = 1001

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val rssi: Short = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                val macAddress = device?.address ?: return
                val deviceName = device.name ?: "Unknown Device"

                Log.e("BluetoothScan", "Device Found: Name: $deviceName | MAC: $macAddress | RSSI: $rssi")

                if (verifiedBeacons.any { it.first == macAddress }) {
                    scanResults[macAddress] = rssi.toInt()
                    Log.e("BluetoothScan", "✅ Verified Beacon: $macAddress (RSSI: $rssi)")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        customMapView = findViewById(R.id.indoorMapView)
        userLocationText = findViewById(R.id.userLocationText)
        errorMessage = findViewById(R.id.errorMessage)
        scanButton = findViewById(R.id.scanButton)
        roomSelector = findViewById(R.id.roomSelector)

        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, bluetoothPermissions, PERMISSION_REQUEST_CODE)
        } else {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(bluetoothReceiver, filter)
        }

        scanButton.setOnClickListener {
            startBluetoothScan()
        }

        // Spinner setup for room selection
        val rooms = listOf(
            "A3-208", "A3-209", "A3-207",
            "A3-206", "A3-205", "A3-204",
            "A3-203", "A3-202", "Reading Hall"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rooms)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        roomSelector.adapter = adapter

        // Simplified: pass room name to map view
        roomSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedRoom = parent.getItemAtPosition(position).toString()
                Toast.makeText(this@MainActivity, "Selected: $selectedRoom", Toast.LENGTH_SHORT).show()
                customMapView.setDestination(selectedRoom)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Initial position estimation
        val (x, y) = estimateUserPosition()
        customMapView.updateUserPosition(x, y)
        userLocationText.text = "User Location: ($x, $y)"
    }

    private fun hasPermissions(): Boolean {
        return bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startBluetoothScan() {
        scanResults.clear()
        errorMessage.text = ""
        errorMessage.visibility = TextView.GONE

        if (!hasPermissions()) {
            Toast.makeText(this, "Missing Bluetooth permissions", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (bluetoothAdapter?.isDiscovering == true) bluetoothAdapter.cancelDiscovery()
            bluetoothAdapter?.startDiscovery()

            scanHandler.postDelayed({
                try { bluetoothAdapter?.cancelDiscovery() } catch (_: SecurityException) {}

                if (scanResults.size < 3) {
                    errorMessage.text = "Not enough verified beacons detected. Move closer."
                    errorMessage.visibility = TextView.VISIBLE
                    return@postDelayed
                }

                val (estX, estY) = estimateUserPosition()
                Log.d("POSITION_DEBUG", "Estimated: ($estX, $estY)")
                customMapView.updateUserPosition(estX, estY)
                userLocationText.text = "User Location: ($estX, $estY)"

            }, 10000)
        } catch (e: SecurityException) {
            Log.e("BluetoothScan", "Start discovery failed: ${e.message}")
            Toast.makeText(this, "Bluetooth permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun estimateUserPosition(): Pair<Float, Float> {
        val distances = verifiedBeacons.mapNotNull { (mac, x, y) ->
            scanResults[mac]?.let { rssi ->
                val distance = calculateDistance(rssi)
                Triple(x, y, distance)
            }
        }
        if (distances.isEmpty()) return Pair(0f, 0f)

        val weightSum = distances.sumOf { 1 / it.third }
        if (weightSum == 0.0) return Pair(0f, 0f)

        val weightedX = distances.sumOf { it.first * (1 / it.third) } / weightSum
        val weightedY = distances.sumOf { it.second * (1 / it.third) } / weightSum
        return Pair(weightedX.toFloat(), weightedY.toFloat())
    }

    private fun calculateDistance(rssi: Int): Double {
        val txPower = -59
        return 10.0.pow((txPower - rssi) / (10 * 2.0))
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(bluetoothReceiver) } catch (_: IllegalArgumentException) {}
    }
}
