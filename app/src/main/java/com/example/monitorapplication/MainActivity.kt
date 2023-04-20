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
    private var gravity: FloatArray = floatArrayOf(0f, 0f, 0f)
    private var geomagnetic: FloatArray = floatArrayOf(0f, 0f, 0f)
    private var azimuth = 0f
    private var lastAzimuth = 0f
    private var stepCount = 0
    private var distance = 0f
    private var strideLength = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val height = 170 // user's height in cm
        strideLength = 0.415f * height // calculate stride length
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

            }
            Sensor.TYPE_MAGNETIC_FIELD -> {

            }
        }
    }

    private fun lowPassFilter(input: FloatArray, output: FloatArray): FloatArray {
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
        return output
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}