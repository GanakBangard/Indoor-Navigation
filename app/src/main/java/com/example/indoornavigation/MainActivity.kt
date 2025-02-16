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
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var customMapView: CustomMapView
    private lateinit var userLocationText: TextView
    private lateinit var errorMessage: TextView
    private lateinit var scanButton: Button

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanResults = mutableMapOf<String, Int>() // Store RSSI values of verified beacons
    private val scanHandler = Handler(Looper.getMainLooper())

    // **List of Verified Beacons (Classic Bluetooth MAC Addresses & Positions)**
    private val verifiedBeacons = listOf(
        Triple("F0:77:C3:F1:3B:A9", 450.0, 100.0), // Beacon 1 (Top)
        Triple("13:30:4C:CB:95:E6", 500.0, 500.0), // Beacon 2 (Middle)
        Triple("00:45:E2:A6:5F:9E", 450.0, 900.0)  // Beacon 3 (Bottom)
    )

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

        // **Register Receiver for Classic Bluetooth Discovery**
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

        Log.d("BluetoothScan", "Starting Classic Bluetooth Discovery...")
        bluetoothAdapter?.startDiscovery()

        // **Stop Discovery After 10 Seconds**
        scanHandler.postDelayed({
            bluetoothAdapter?.cancelDiscovery()
            processScanResults()
        }, 10000)
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val rssi: Int = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                device?.address?.let { macAddress ->
                    if (verifiedBeacons.any { it.first == macAddress }) {
                        scanResults[macAddress] = rssi
                        Log.d("BluetoothScan", "Verified Device Found: $macAddress (RSSI: $rssi)")
                    }
                }
            }
        }
    }

    private fun processScanResults() {
        if (scanResults.size < 3) {
            errorMessage.text = "Not enough verified beacons detected. Move closer."
            errorMessage.visibility = TextView.VISIBLE
            return
        }

        // **Map Beacons to Their Positions & RSSI Values**
        val distances = verifiedBeacons.mapNotNull { (mac, x, y) ->
            scanResults[mac]?.let { rssi ->
                val distance = calculateDistance(rssi)
                if (distance.isNaN() || distance.isInfinite() || distance > 100) {  // Ignore invalid distances
                    Log.e("ProcessScan", "Ignoring invalid distance for $mac: $distance")
                    null
                } else {
                    Triple(x, y, distance)
                }
            }
        }

        // **Calculate User Position**
        if (distances.size == 3) {
            val userPosition = trilaterate(distances)
            if (!userPosition.first.isNaN() && !userPosition.second.isNaN()) {
                customMapView.updateUserPosition(userPosition.first, userPosition.second)
                userLocationText.text = "User Location: (${userPosition.first}, ${userPosition.second})"
            } else {
                errorMessage.text = "Beacon signals too weak for accurate positioning."
                errorMessage.visibility = TextView.VISIBLE
            }
        } else {
            errorMessage.text = "Beacon signals too weak for positioning."
            errorMessage.visibility = TextView.VISIBLE
        }
    }


    // **Convert RSSI to Distance**
    private fun calculateDistance(rssi: Int): Double {
        val txPower = -59 // Reference RSSI at 1m
        return 10.0.pow((txPower - rssi) / (10 * 2.0))
    }

    // **Trilateration to Find User Position**
    // **Trilateration with Validity Checks**
    private fun trilaterate(beacons: List<Triple<Double, Double, Double>>): Pair<Double, Double> {
        if (beacons.size < 3) {
            Log.e("Trilateration", "Not enough beacons for trilateration.")
            return Pair(Double.NaN, Double.NaN)
        }

        val (x1, y1, d1) = beacons[0]
        val (x2, y2, d2) = beacons[1]
        val (x3, y3, d3) = beacons[2]

        // **Avoid Invalid Distance Values**
        if (d1.isNaN() || d2.isNaN() || d3.isNaN() || d1 <= 0 || d2 <= 0 || d3 <= 0) {
            Log.e("Trilateration", "Invalid distances: ($d1, $d2, $d3)")
            return Pair(Double.NaN, Double.NaN)
        }

        val A = 2 * (x2 - x1)
        val B = 2 * (y2 - y1)
        val C = d1.pow(2) - d2.pow(2) - x1.pow(2) + x2.pow(2) - y1.pow(2) + y2.pow(2)

        val D = 2 * (x3 - x1)
        val E = 2 * (y3 - y1)
        val F = d1.pow(2) - d3.pow(2) - x1.pow(2) + x3.pow(2) - y1.pow(2) + y3.pow(2)

        val denominator = (E * A - B * D)
        if (denominator == 0.0) {
            Log.e("Trilateration", "Denominator is zero, causing division error.")
            return Pair(Double.NaN, Double.NaN)
        }

        val x = (C * E - F * B) / denominator
        val y = (C * D - A * F) / (B * D - A * E)

        if (x.isInfinite() || y.isInfinite() || x.isNaN() || y.isNaN()) {
            Log.e("Trilateration", "Invalid result: ($x, $y)")
            return Pair(Double.NaN, Double.NaN)
        }

        return Pair(x, y)
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver) // Unregister Receiver when App is Closed
    }
}
