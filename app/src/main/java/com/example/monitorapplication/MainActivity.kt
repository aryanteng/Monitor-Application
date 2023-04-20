package com.example.monitorapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.monitorapplication.databinding.ActivityMainBinding
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor

    private val alpha = 0.8f
    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var stepCount = 0
    private var distance = 0.0

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)


    private var strideLength = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val height = 170
        val weight = 80
        strideLength = (0.415 * height.toDouble().pow(1.12) - (weight * 0.036)).toFloat()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when(event.sensor.type){
            Sensor.TYPE_ACCELEROMETER -> {
                val gravity = FloatArray(3)
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                val linearAcceleration = FloatArray(3)
                linearAcceleration[0] = event.values[0] - gravity[0]
                linearAcceleration[1] = event.values[1] - gravity[1]
                linearAcceleration[2] = event.values[2] - gravity[2]

                // Compute the acceleration magnitude
                val accelerationMagnitude = sqrt(
                    linearAcceleration[0].toDouble().pow(2.0) +
                            linearAcceleration[1].toDouble().pow(2.0) +
                            linearAcceleration[2].toDouble().pow(2.0)
                )

                if (accelerationMagnitude > 12 && accelerationMagnitude < 20) { // adjust this threshold to suit your needs
                    stepCount++
                    distance += strideLength
                }

                binding.tvStepCount.text = "Steps: $stepCount"
                binding.tvDistance.text = "Distance: $distance"

                if (lastMagnetometer.isNotEmpty()) {
                    val rotationMatrix = FloatArray(9)
                    val inclinationMatrix = FloatArray(9)

                    // Compute the rotation matrix and inclination matrix
                    SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, lastMagnetometer)

                    // Get the orientation angles from the rotation matrix
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    // Convert the orientation angles from radians to degrees
                    val azimuth = Math.toDegrees(orientation[0].toDouble())

                    // Compute the direction of movement
                    val direction = calculateDirection(azimuth)

                    // Update the UI with the direction
                    binding.tvDirection.text = "Direction: $direction"
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lastMagnetometer = event.values
            }
        }
    }

    private fun calculateDirection(azimuth: Double): String {
        var direction = ""
        if (azimuth >= -22.5 && azimuth < 22.5) {
            direction = "North"
        } else if (azimuth >= 22.5 && azimuth < 67.5) {
            direction = "Northeast"
        } else if (azimuth >= 67.5 && azimuth < 112.5) {
            direction = "East"
        } else if (azimuth >= 112.5 && azimuth < 157.5) {
            direction = "Southeast"
        } else if (azimuth >= 157.5 || azimuth < -157.5) {
            direction = "South"
        } else if (azimuth >= -157.5 && azimuth < -112.5) {
            direction = "Southwest"
        } else if (azimuth >= -112.5 && azimuth< -67.5) {
            direction = "West"
        } else if (azimuth >= -67.5 && azimuth < -22.5) {
            direction = "Northwest"
        }
        return direction
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}