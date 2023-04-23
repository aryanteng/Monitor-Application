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
import kotlin.math.pow
import kotlin.math.sqrt
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

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor

    private var lastMagnetometer = FloatArray(3)
    private var stepCount = 0
    private var distance = 0.0

    private var liftThreshold: Float = 1.5f
    private var stairsThreshold: Float = 2.5f
    private var isOnStairs: Boolean = false
    private var isOnLift: Boolean = false


    private var strideLength = 0f

    private val recordedTrajectory = mutableListOf<GeoPoint>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val height = 177
        val weight = 80
        strideLength = ((0.415 * height.toDouble().pow(1.12) - (weight * 0.036))/100).toFloat()


        Configuration.getInstance().userAgentValue = packageName

        binding.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(20.0)

        recordedTrajectory.clear()

        getCurrentLocation()

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
                //comment here
                val alpha = 0.08f
                val gravity = FloatArray(3)
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                //comment here
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

                binding.tvLiftOrStairs.text = "Magnitude: $accelerationMagnitude"

                //comment here
                if (accelerationMagnitude > 1.1 && accelerationMagnitude < 1.4) {
                    stepCount++
                    distance += strideLength / 2

                    getCurrentLocation()
                }

                if (linearAcceleration[2] > liftThreshold && linearAcceleration[2] < stairsThreshold) {
                    isOnLift = true
                } else if (linearAcceleration[2] > stairsThreshold) {
                    isOnStairs = true
                }

                // updating the UI with step count
                binding.tvStepCount.text = "Steps: $stepCount"
                // updating the UI with distance
                binding.tvDistance.text = "Distance: $distance m"

                if(isOnLift){
                    Toast.makeText(this, "Lift", Toast.LENGTH_SHORT).show()
//                    binding.tvLiftOrStairs.text = "Lift"
                }
                if(isOnStairs){
                    Toast.makeText(this, "Stairs", Toast.LENGTH_SHORT).show()
//                    binding.tvLiftOrStairs.text = "Stairs"
                }

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

                    // updating the UI with the direction
                    binding.tvDirection.text = "Direction: $direction"
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                // comment here
                lastMagnetometer = event.values
            }
        }
    }

    private fun calculateDirection(azimuth: Double): String {
        // comment here
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

    private fun updateMapViewWithTrajectory() {
        Log.d("TAG", "updateMapViewWithTrajectory called")

        val path = Polyline(binding.map)
        path.outlinePaint.color = Color.parseColor("#4286F4")
        path.outlinePaint.strokeWidth = 7.0f
        path.outlinePaint.strokeCap = Paint.Cap.ROUND

        val dashInterval = 20.0f
        val gapInterval = 50.0f
        val effect = DashPathEffect(floatArrayOf(dashInterval, gapInterval), 0.0f)
        path.outlinePaint.pathEffect = effect

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
        updateLocationUI(location)
        // Record the user's location in the trajectory recorder
        recordLocation(location.latitude, location.longitude)
        // Update the map view with the new trajectory
        updateMapViewWithTrajectory()
    }

    private fun updateLocationUI(location: Location) {
        // Update the UI with the current location
        Log.d("LOCATION", "Current location: ${location.latitude}, ${location.longitude}")
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // mandatory hook for sensor event listener
    }
}