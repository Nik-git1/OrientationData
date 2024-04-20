package com.example.myapplication
import android.content.Context
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.example.myapplication.LineChartActivity
import androidx.compose.ui.Modifier
import java.io.File
import java.io.FileWriter
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember

import androidx.compose.ui.graphics.Path

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private lateinit var database: FirebaseDatabase
    private lateinit var orientationDataRef: DatabaseReference

    private var livePitch by mutableStateOf(0f)
    private var liveRoll by mutableStateOf(0f)
    private var liveYaw by mutableStateOf(0f)

    // State variable for historical orientation data (latest 500 values)
    private var historicalData by mutableStateOf<List<Pair<Triple<Double?, Double?, Double?>, Long>>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing")

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        database = FirebaseDatabase.getInstance()
        orientationDataRef = database.getReference("orientation_data")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            Column {
                // Live data display
                LiveRotationValues(livePitch, liveRoll, liveYaw)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { exportDataToFile() },
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text("Export Data")
                }

                // Historical data display
                LineChartActivity(historicalData.map { it.first.first?.toFloat() ?: 0f }, Color.Blue)
                Spacer(modifier = Modifier.weight(1f))
                Text(text ="PITCH")
                LineChartActivity(historicalData.map { it.first.second?.toFloat() ?: 0f }, Color.Red)
                Spacer(modifier = Modifier.weight(1f))
                Text(text ="ROLL")
                LineChartActivity(historicalData.map { it.first.third?.toFloat() ?: 0f }, Color.Green)
                Spacer(modifier = Modifier.weight(1f))
                Text(text ="YAW")

                // Export button

            }
        }

        // Listen for changes in Firebase Realtime Database
        orientationDataRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear historical data
                historicalData = emptyList()

                // Iterate over each child node under the "orientation_data" key
                snapshot.children.reversed().take(10).forEach { data ->
                    // Extract orientation data map and timestamp
                    val orientationData = data.child("orientation").value as? Map<String, Double>
                    val timestamp = data.child("timestamp").value as? Long ?: 0L

                    // Check if orientationData is not null
                    if (orientationData != null) {
                        // Extract pitch, roll, and yaw values
                        val pitch = orientationData["pitch"]
                        val roll = orientationData["roll"]
                        val yaw = orientationData["yaw"]

                        historicalData += Triple(pitch, roll, yaw) to timestamp
                    } else {
                        // Handle the case where orientationData is null
                        Log.e(TAG, "Orientation data is null for data: $data")
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    @Composable
    fun LiveRotationValues(pitch: Float, roll: Float, yaw: Float) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Live Orientation Data:")
            Text("Pitch: $pitch")
            Text("Roll: $roll")
            Text("Yaw: $yaw")
        }
    }

    // Function to export data to a text file
    private fun exportDataToFile() {
        val fileName = "orientation_data3.txt"
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(directory, fileName)

        try {
            FileWriter(file).use { writer ->
                historicalData.forEachIndexed { index, data ->
                    val (orientation, timestamp) = data
                    val line = "Index: $index, Orientation: $orientation, Timestamp: $timestamp\n"
                    writer.write(line)
                }
            }
            Log.d(TAG, "Data exported successfully to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting data: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            val orientationValues = FloatArray(3)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationValues)
            livePitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
            liveRoll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()
            liveYaw = Math.toDegrees(orientationValues[0].toDouble()).toFloat()

            // Push orientation data to Firebase Realtime Database with timestamp
            val timestamp = System.currentTimeMillis()
            orientationDataRef.push().setValue(
                mapOf(
                    "orientation" to mapOf(
                        "pitch" to livePitch,
                        "roll" to liveRoll,
                        "yaw" to liveYaw
                    ),
                    "timestamp" to timestamp
                )
            )

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_FASTEST)

    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
