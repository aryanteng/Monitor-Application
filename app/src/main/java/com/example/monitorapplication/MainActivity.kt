package com.example.monitorapplication

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
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

    private val recordedTrajectory = mutableListOf<Pair<Double, Double>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val height = 175
        val weight = 80
        strideLength = (0.415 * height.toDouble().pow(1.12) - (weight * 0.036)).toFloat()


        Configuration.getInstance().userAgentValue = packageName

        binding.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        binding.map.setBuiltInZoomControls(true)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(8.0)

        val currentLocation = getCurrentLocation()
        Log.i("BKL", currentLocation.toString())
        if (currentLocation != null) {
            val currentGeoPoint = GeoPoint(currentLocation.latitude, currentLocation.longitude)
            val currentMarker = Marker(binding.map)
            currentMarker.icon = ContextCompat.getDrawable(this, com.google.android.material.R.drawable.mtrl_ic_arrow_drop_up)
            currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            currentMarker.position = currentGeoPoint
            binding.map.overlays.add(currentMarker)
            binding.map.controller.setCenter(currentGeoPoint)
        }
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
                //comment here
                val alpha = 0.08f
                val gravity = FloatArray(3)
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

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

                //comment here
                if (accelerationMagnitude > 1.2 && accelerationMagnitude < 8) {
                    stepCount++
                    distance += strideLength

                    val location = getCurrentLocation()

                    if (location != null) {
                        // Record the user's location in the trajectory recorder
                        recordLocation(location.latitude, location.longitude)

                        // Update the map view with the new trajectory
                        updateMapViewWithTrajectory()
                    }
                }

                if (linearAcceleration[2] > liftThreshold) {
                    isOnLift = true
                    isOnStairs = false
                } else if (linearAcceleration[2] > stairsThreshold) {
                    isOnStairs = true
                    isOnLift = false
                } else {
                    isOnStairs = false
                    isOnLift = false
                }

                // updating the UI with step count
                binding.tvStepCount.text = "Steps: $stepCount"
                // updating the UI with distance
                binding.tvDistance.text = "Distance: $distance m"
                if(isOnLift){
                    binding.tvLiftOrStairs.text = "Lift"
                }
                if(isOnStairs){
                    binding.tvLiftOrStairs.text = "Stairs"
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

    private fun drawPolylineOnMap(points: List<Pair<Double, Double>>) {
        val polyline = Polyline().apply {
            points.map { org.osmdroid.util.GeoPoint(it.first, it.second) }
            isGeodesic = true
            infoWindow = null
        }
        binding.map.overlays.add(polyline)
        binding.map.invalidate()
    }

    private fun updateMapViewWithTrajectory() {
        // Clear any existing overlays on the map
        binding.map.overlays.clear()

        // Draw a polyline representing the user's trajectory
        drawPolylineOnMap(recordedTrajectory)

        // Set the map view center to the last point in the trajectory
        val lastPoint = recordedTrajectory.lastOrNull()
        if (lastPoint != null) {
            binding.map.controller.setCenter(org.osmdroid.util.GeoPoint(lastPoint.first, lastPoint.second))
        }
    }

    private fun recordLocation(latitude: Double, longitude: Double) {
        recordedTrajectory.add(Pair(latitude, longitude))
    }

    private fun getCurrentLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                // Called when the user's location has changed
            }

            override fun onProviderEnabled(provider: String) {
                // Called when the user enables the location provider (e.g. GPS)
            }

            override fun onProviderDisabled(provider: String) {
                // Called when the user disables the location provider (e.g. GPS)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // Called when the location provider status changes (e.g. enabled, disabled)
            }
        }

        // Request location updates
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                locationListener
            )

            // Wait for the callback to return the current location
            val timeout = 5000 // Timeout in milliseconds
            var elapsed = 0
            while (elapsed < timeout) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                Log.i("mkc", location.toString())
                if (location != null) {
                    return location
                } else {
                    Thread.sleep(100)
                    elapsed += 100
                }
            }

            // Timeout waiting for location update
            return null
        } else {
            // Permission denied
            return null
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}