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
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.model.LatLng


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

        setContentView(R.layout.activity_main)
        setContentView(view)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val height = 175
        val weight = 80
        strideLength = (0.415 * height.toDouble().pow(1.12) - (weight * 0.036)).toFloat()


        Configuration.getInstance().userAgentValue = packageName

        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setBuiltInZoomControls(true)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(20.0)

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
                if (accelerationMagnitude > 1 && accelerationMagnitude < 3) {
                    stepCount++
                    distance += strideLength / 2

                    getCurrentLocation()
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

    private fun updateMapViewWithTrajectory() {
        Log.d("TAG", "updateMapViewWithTrajectory called")

        val path = Polyline(binding.map)
        path.outlinePaint.color = Color.BLUE
        path.outlinePaint.strokeWidth = 7.0f
        path.outlinePaint.strokeCap = Paint.Cap.ROUND

        val dashInterval = 20.0f
        val gapInterval = 50.0f
        val effect = DashPathEffect(floatArrayOf(dashInterval, gapInterval), 0.0f)
        path.outlinePaint.pathEffect = effect

//        val dummy = listOf(
//            LatLng(37.422160, -122.084270),
//            LatLng(37.422180, -122.084290),
//            LatLng(37.422200, -122.084310),
//            LatLng(37.422220, -122.084330),
//            LatLng(37.422240, -122.084350),
//            LatLng(37.422260, -122.084370),
//            LatLng(37.422280, -122.084390),
//            LatLng(37.422300, -122.084410),
//            LatLng(37.422320, -122.084430),
//            LatLng(37.422340, -122.084450),
//            LatLng(37.422360, -122.084470),
//            LatLng(37.422380, -122.084490),
//            LatLng(37.422400, -122.084510),
//            LatLng(37.422420, -122.084530),
//            LatLng(37.422440, -122.084550),
//            LatLng(37.422460, -122.084570),
//            LatLng(37.422480, -122.084590),
//            LatLng(37.422500, -122.084610),
//            LatLng(37.422520, -122.084630),
//            LatLng(37.422540, -122.084650),
//            LatLng(37.422560, -122.084670),
//            LatLng(37.422580, -122.084690),
//            LatLng(37.422600, -122.084710),
//            LatLng(37.422610, -122.084700), // slight left turn
//            LatLng(37.422620, -122.084690), // slight left turn
//            LatLng(37.422640, -122.084670), // slight right turn
//            LatLng(37.422660, -122.084650), // sharp right turn
//            LatLng(37.422680, -122.084630), // sharp left turn
//            LatLng(37.422700, -122.084610), // slight left turn
//            LatLng(37.422720, -122.084590),
//            LatLng(37.422740, -122.084570),
//            LatLng(37.422760, -122.084550),
//            LatLng(37.422780, -122.084530),
//            LatLng(37.422800, -122.084510),
//            LatLng(37.422820, -122.084490),
//            LatLng(37.422840, -122.084470),
//            LatLng(37.422860, -122.084450),
//            LatLng(37.422880, -122.084430),
//            LatLng(37.422900, -122.084410),
//            LatLng(37.422920, -122.084390),
//            LatLng(37.422940, -122.084370),
//            LatLng(37.422960, -122.084350),
//            LatLng(37.422980, -122.084330),
//            LatLng(37.423000, -122.084310),
//            LatLng(37.423020, -122.084290),
//            LatLng(37.423040, -122.084270),
//            LatLng(37.423060, -122.084250), // slight left turn
//            LatLng(37.423080, -122.084230), // slight right turn
//            LatLng(37.423100, -122.084210),
//            LatLng(37.423120, -122.084190),
//        )
//
        for (item in recordedTrajectory) {
            val point = GeoPoint(item.first, item.second)
            path.addPoint(point)
        }

        binding.map.overlays.add(path)
        binding.map.invalidate()
    }

    private fun recordLocation(latitude: Double, longitude: Double) {
        recordedTrajectory.add(Pair(latitude, longitude))
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
            val currentMarker = Marker(binding.map)
            currentMarker.icon = ContextCompat.getDrawable(applicationContext, com.google.android.material.R.drawable.ic_clock_black_24dp)
            currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            currentMarker.position = GeoPoint(location.latitude, location.longitude)
            binding.map.overlays.add(currentMarker)
            binding.map.controller.setCenter(GeoPoint(location.latitude, location.longitude))
//            currentMarker.position = GeoPoint(37.422160, -122.084270)
//            binding.map.overlays.add(currentMarker)
//            binding.map.controller.setCenter(GeoPoint(37.422160,-122.084270 ))
            updateLocationUI(location)
            recordLocation(location.latitude, location.longitude)
            updateMapViewWithTrajectory()

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
                        val currentMarker = Marker(binding.map)
                        currentMarker.icon = ContextCompat.getDrawable(applicationContext, com.google.android.material.R.drawable.mtrl_ic_arrow_drop_up)
                        currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        currentMarker.position = GeoPoint(location.latitude, location.longitude)
                        binding.map.overlays.add(currentMarker)
                        binding.map.controller.setCenter(GeoPoint(location.latitude, location.longitude))

                        // Record the user's location in the trajectory recorder
                        recordLocation(location.latitude, location.longitude)
                        // Update the map view with the new trajectory
                        updateMapViewWithTrajectory()
                    }
                }
            }

            // Request location updates
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
    }

    private fun updateLocationUI(location: Location) {
        // Update the UI with the current location
        Log.d("LOCATION", "Current location: ${location.latitude}, ${location.longitude}")
        // You can use the location object to perform your desired functionality here
    }



    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // mandatory hook for sensor event listener
    }
}