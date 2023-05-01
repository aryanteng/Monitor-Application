package com.example.monitorapplication

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.monitorapplication.databinding.ActivityMainBinding
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.config.Configuration
import android.Manifest
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.*
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlin.math.*

class MainActivity : AppCompatActivity(), SensorEventListener {
    // defining binding
    private lateinit var binding: ActivityMainBinding

    // defining sensors
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor

    // defining variable for storing magnetometer values
    private var magnetometerValues = FloatArray(3)

    private var gravity = FloatArray(3)
    private var linearAcceleration = FloatArray(3)
    private var lastAzimuth: Double = 0.0
    private var accelerationMagnitudeList : MutableList<Double> = mutableListOf()
    private var zAxisMagnitudeList : MutableList<Float> = mutableListOf()

    // defining variable for step count
    private var stepCount = 0

    // defining variables for distance
    private var distance = 0.0

    // defining variable for stride length
    private var strideLength = 0f

    // defining variable for storing recorded geopoints
    private val recordedTrajectory = mutableListOf<GeoPoint>()

    // Debounce mechanism to prevent multiple steps from being detected in quick succession
    private var lastStepTime: Long = 0
    private val stepDebounceTime = 250 // milliseconds

    // Debounce mechanism to prevent multiple toasts to show in quick succession
    private var lastStairsToastTime: Long = 0
    private var stairsToastDebounceTime = 2000 // milliseconds

    private var accelerometerReadings = FloatArray(3)

    // add the following variables for tracking the user's path
    private val xValues = ArrayList<Float>()
    private val yValues = ArrayList<Float>()
    private var previousX = 0f
    private var previousY = 0f
    private var currX = 0f
    private var currY = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // initialising sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // stride length calculation
        val height = 177
        val weight = 85
        strideLength = 0.415f * height

        // initialising map
        Configuration.getInstance().userAgentValue = packageName
        binding.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(20.0)

        // clearing the recorded trajectory
        recordedTrajectory.clear()

        // calling for location
        getCurrentLocation()

        binding.tvDisplacement.text = "Displacement: 0.0 m"

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
        when (event.sensor.type){
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerReadings = event.values
                // a low pass filter to smooth out the accelerometer readings and to remove the contribution of gravity
                val alpha = 0.8f
                for (i in gravity.indices){
                    gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i]
                }

                // removing gravity component from the accelerometer readings
                for (i in linearAcceleration.indices){
                    linearAcceleration[i] = event.values[i] - gravity[i]
                }

                // calculating the magnitude of acceleration
                val accelerationMagnitude = calculateMagnitude(linearAcceleration)

                binding.tvAccelerometer.text = "Accelerometer: ${accelerationMagnitude.toFloat()}"

                // threshold for detecting steps
                if (isStep(accelerationMagnitude = accelerationMagnitude)) {
                    // increment step count
                    stepCount++
                    // stride length is distance covered in 2 steps so after one step it should be half the stride length
                    distance += strideLength / 2 / 100

                    // calling the current location hook
                    getCurrentLocation()

                    val displacement = strideLength / 2
                    val deltaX = (displacement * sin(Math.toRadians(lastAzimuth))).toFloat()
                    val deltaY = (displacement * cos(Math.toRadians(lastAzimuth))).toFloat()

                    // add the displacement to the previous coordinates to get the current coordinates
                    currX = previousX + deltaX
                    currY = previousY + deltaY

                    // add the current coordinates to the list of x and y values
                    xValues.add(currX)
                    yValues.add(currY)

                    binding.userTrajectory.addPoint(currX, currY)
                    binding.userTrajectory.invalidate()

                    // update the previous coordinates with the current coordinates
                    previousX = currX
                    previousY = currY

                    binding.tvDisplacement.text = "Displacement: ${sqrt(previousX.pow(2) + previousY.pow(2))/100} m"

                    Log.i("DISPLACEMENT", "${sqrt(previousX.pow(2) + previousY.pow(2))} cm")

                }

                if(isStairs(zAxisMagnitude = linearAcceleration[2])){
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastStairsToastTime > stairsToastDebounceTime){
                        Toast.makeText(this, "Stairs", Toast.LENGTH_SHORT).show()
                        lastStairsToastTime = currentTime
                    }
                }

                if (magnetometerValues.isNotEmpty()) {
                    val magnetometerMagnitude = calculateMagnitude(magnetometerValues)
                    if (isLift(magnetometerMagnitude = magnetometerMagnitude)){
                        binding.tvLift.text = "Lift"
                    }
                    else {
                        binding.tvLift.text = ""
                    }
                }

                // updating the UI with step count
                binding.tvStepCount.text = "Steps: $stepCount"
                // updating the UI with distance
                binding.tvDistance.text = "Distance: ${distance.toFloat()} m"

                if (magnetometerValues.isNotEmpty()) {
                    val rotationMatrix = FloatArray(9)
                    val inclinationMatrix = FloatArray(9)

                    // Get rotation matrix from inclination matrix, gravity and magnetometer values
                    SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, magnetometerValues)

                    // From the rotation matrix we extract orientation angles
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    // converting orientation angle from radian to degrees
                    val azimuth = Math.toDegrees(orientation[0].toDouble())
                    lastAzimuth = azimuth

                    // calculating the direction from the azimuth
                    val direction = calculateDirection(azimuth)

                    // updating the UI with the direction
                    binding.tvDirection.text = "Direction: $direction"

                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                // storing the magnetometer sensor readings in a list to use it in accelerometer
                magnetometerValues = event.values
            }
        }
    }

    private fun isLift(magnetometerMagnitude: Double): Boolean{
        val magnitudeThreshold = 22.0f

        binding.tvMagnetometer.text = "Magnetometer: ${magnetometerMagnitude.toFloat()}"

        // Check if the magnetometer magnitude is lesser than threshold
        if (magnetometerMagnitude < magnitudeThreshold) {
            return true
        }

        return false
    }

    private fun isStairs(zAxisMagnitude: Float): Boolean{
        val slidingWindowSize = 10
        val stairsThreshold = 5

        // Add the current acceleration magnitude to the list
        zAxisMagnitudeList.add(zAxisMagnitude)

        // Remove the oldest data point if the list size exceeds the window size
        if (zAxisMagnitudeList.size > slidingWindowSize) {
            zAxisMagnitudeList.removeAt(0)
        }

        // Calculate the moving average
        val movingAverage = zAxisMagnitudeList.average()

        // Check if the acceleration magnitude is greater than the moving average plus the threshold factor
        if (zAxisMagnitude > movingAverage + stairsThreshold) {
            return true
        }

        return false
    }

    private fun isStep(accelerationMagnitude: Double): Boolean {
        val movingAverageWindowSize = 10
        val peakThresholdFactor = 1.1
        val currentTimestamp = System.currentTimeMillis()

        // Add the current acceleration magnitude to the list
        accelerationMagnitudeList.add(accelerationMagnitude)

        // Remove the oldest data point if the list size exceeds the window size
        if (accelerationMagnitudeList.size > movingAverageWindowSize) {
            accelerationMagnitudeList.removeAt(0)
        }

        // Calculate the moving average
        val movingAverage = accelerationMagnitudeList.average()

        // Check if the acceleration magnitude is greater than the moving average multiplied by the threshold factor
        if (accelerationMagnitude > movingAverage + peakThresholdFactor && currentTimestamp - lastStepTime > stepDebounceTime) {
            lastStepTime = currentTimestamp
            Log.i("LIST", accelerationMagnitudeList.toString())
            Log.i("AVERAGE", movingAverage.toString())
            return true
        }

        return false
    }

    private fun calculateMagnitude(array: FloatArray): Double {
        return sqrt(
            array[0].toDouble().pow(2) +
                    array[1].toDouble().pow(2) +
                    array[2].toDouble().pow(2)
        )
    }

    private fun calculateDirection(azimuth: Double): String {
        // generic function for calculating the direction using azimuth angle
        var direction = ""
        if (azimuth >= -22.5 && azimuth < 22.5) {
            direction = "North"
        }
        else if (azimuth >= 22.5 && azimuth < 67.5) {
            direction = "Northeast"
        }
        else if (azimuth >= 67.5 && azimuth < 112.5) {
            direction = "East"
        }
        else if (azimuth >= 112.5 && azimuth < 157.5) {
            direction = "Southeast"
        }
        else if (azimuth >= -112.5 && azimuth< -67.5) {
            direction = "West"
        }
        else if (azimuth >= -67.5 && azimuth < -22.5) {
            direction = "Northwest"
        }
        else if (azimuth >= 157.5 || azimuth < -157.5) {
            direction = "South"
        }
        else if (azimuth >= -157.5 && azimuth < -112.5) {
            direction = "Southwest"
        }
        return direction
    }

    private fun updateMapViewWithTrajectory() {
        Log.d("TAG", "updateMapViewWithTrajectory called")

        val path = Polyline(binding.map)
        path.outlinePaint.color = Color.parseColor("#4286F4")
        path.outlinePaint.strokeWidth = 7.0f
        path.outlinePaint.strokeCap = Paint.Cap.ROUND

        // making the path dashed
        val dashInterval = 20.0f
        val gapInterval = 50.0f
        val effect = DashPathEffect(floatArrayOf(dashInterval, gapInterval), 0.0f)
        path.outlinePaint.pathEffect = effect

        // dummy list of coordinates for testing polyline
        val dummy = listOf(
            GeoPoint(28.519866666666665, 77.21405833333333),
            GeoPoint(28.519899, 77.214297),
            GeoPoint(28.519936, 77.214478),
            GeoPoint(28.519996, 77.214717),
            GeoPoint(28.520074, 77.214964),
            GeoPoint(28.520151, 77.215216),
            GeoPoint(28.520231, 77.215461),
            GeoPoint(28.520315, 77.215703),
            GeoPoint(28.520397, 77.215948),
            GeoPoint(28.520473, 77.216193),
            GeoPoint(28.520554, 77.216441),
            GeoPoint(28.520627, 77.216684),
            GeoPoint(28.520702, 77.216932),
            GeoPoint(28.520774, 77.21718),
            GeoPoint(28.520847, 77.217427),
            GeoPoint(28.520925, 77.217672),
            GeoPoint(28.520999, 77.21792),
            GeoPoint(28.521072, 77.218167),
            GeoPoint(28.521145, 77.218414),
            GeoPoint(28.521221, 77.218661),
            GeoPoint(28.521295, 77.218908),
            GeoPoint(28.521365, 77.219156),
            GeoPoint(28.521435, 77.219403),
            GeoPoint(28.521506, 77.21965),
            GeoPoint(28.521575, 77.219897),
            GeoPoint(28.521645, 77.220144),
            GeoPoint(28.521712, 77.220391),
            GeoPoint(28.521779, 77.220638),
            GeoPoint(28.521847, 77.220885),
            GeoPoint(28.521915, 77.221132),
            GeoPoint(28.521983, 77.221379),
            GeoPoint(28.522051, 77.221626),
            GeoPoint(28.522119, 77.221873),
            GeoPoint(28.522187, 77.22212),
            GeoPoint(28.522255, 77.222367),
            GeoPoint(28.522323, 77.222614),
            GeoPoint(28.522391, 77.222861),
            GeoPoint(28.522459, 77.223108),
            GeoPoint(28.522527, 77.223355),
            GeoPoint(28.522595, 77.223602),
            GeoPoint(28.522663, 77.223849),
            GeoPoint(28.522731, 77.224096),
            GeoPoint(28.522799, 77.224343),
            GeoPoint(28.522867, 77.22459),
            GeoPoint(28.522935, 77.224837),
            GeoPoint(28.523002, 77.225084),
            GeoPoint(28.52307, 77.225331),
            GeoPoint(28.523138, 77.225578),
            GeoPoint(28.523206, 77.225825),
        )

        for (item in recordedTrajectory) {
            path.addPoint(item)
        }

        binding.map.overlays.add(path)
        binding.map.invalidate()
    }

    private fun recordLocation(latitude: Double, longitude: Double) {
        recordedTrajectory.add(GeoPoint(latitude, longitude))
    }

    private fun getCurrentLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission to access the location if it is not already granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 123)
            return
        }
        // Get the last known location from the location manager
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location != null) {
            // If the location is not null, update the UI with the current location
            updateMapUI(location)
        } else {
            // If the location is null, request location updates
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 10000
                fastestInterval = 5000
            }
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        updateMapUI(location)
                    }
                }
            }
            // Request location updates
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
    }

    private fun updateMapUI(location: Location){
        val currentMarker = Marker(binding.map)
        currentMarker.icon = ContextCompat.getDrawable(applicationContext, R.drawable.location)
        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        currentMarker.position = GeoPoint(location.latitude, location.longitude)
        binding.map.overlays.add(currentMarker)
        binding.map.controller.setCenter(currentMarker.position)
        // Record the user's location in the trajectory recorder
        recordLocation(location.latitude, location.longitude)
        // Update the map view with the new trajectory
        updateMapViewWithTrajectory()
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // mandatory hook for sensor event listener
    }
}