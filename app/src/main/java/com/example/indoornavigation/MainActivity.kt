package com.example.indoornavigation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var customMapView: CustomMapView
    private lateinit var userLocationText: TextView
    private lateinit var errorMessage: TextView
    private lateinit var scanButton: Button

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanHandler = Handler(Looper.getMainLooper())
    private val scanResults = mutableMapOf<String, Int>() // Stores RSSI values of verified beacons

    // **List of Verified Beacons (MAC Address & Position)**
    private val verifiedBeacons = listOf(
        Triple("F0:77:C3:F1:3B:A9", 0, 2), // Beacon A
        Triple("74:4C:A1:7A:D1:36", 0, 5), // Beacon B
        Triple("00:45:E2:A6:5F:9E", 0, 8)  // Beacon C
    )

    // **Valid Path Points** (Straight passage from (0,0) to (0,10))
    private val validPath = (0..10).map { Pair(0, it) }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val rssi: Short = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                val macAddress = device?.address ?: return
                val deviceName = device?.name ?: "Unknown Device"

                // **Log Device Name and RSSI Value**
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

        scanButton.setOnClickListener {
            startBluetoothScan()
        }

        // **Register Bluetooth Receiver**
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun startBluetoothScan() {
        scanResults.clear()
        errorMessage.text = ""
        errorMessage.visibility = TextView.GONE

        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }

        bluetoothAdapter?.startDiscovery()

        scanHandler.postDelayed({
            bluetoothAdapter?.cancelDiscovery()

            if (scanResults.size < 3) {
                errorMessage.text = "Not enough verified beacons detected. Move closer."
                errorMessage.visibility = TextView.VISIBLE
                return@postDelayed
            }

            // **Calculate User Position using Grid-based Mapping**
            val estimatedY = estimateUserPosition()
            val userPosition = Pair(0, estimatedY)
            customMapView.updateUserPosition(userPosition.first.toDouble(), userPosition.second.toDouble()) // Update map
            userLocationText.text = "User Location: (0, $estimatedY)"
        }, 10000) // Scan for 10 seconds
    }

    // **Estimate User Position on a Fixed Grid**
    private fun estimateUserPosition(): Int {
        val distances = verifiedBeacons.mapNotNull { (mac, x, y) ->
            scanResults[mac]?.let { rssi ->
                val distance = calculateDistance(rssi)
                Triple(x, y, distance)
            }
        }

        if (distances.isEmpty()) return 0 // Default to start of passage

        // **Weighted Positioning on the Grid**
        val weightedY = distances.sumOf { it.second * (1 / it.third) } / distances.sumOf { 1 / it.third }
        return validPath.minByOrNull { (_, y) -> kotlin.math.abs(y - weightedY) }?.second ?: 0
    }

    // **Convert RSSI to Approximate Distance**
    private fun calculateDistance(rssi: Int): Double {
        val txPower = -59 // Reference RSSI at 1m
        return 10.0.pow((txPower - rssi) / (10 * 2.0))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }
}
