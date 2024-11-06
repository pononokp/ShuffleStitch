package ca.unb.mobiledev.shufflestitch

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        Log.d(TAG, "Location activity created")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        lastLocation
    }

    @get:SuppressLint("MissingPermission")
    private val lastLocation: Unit
        get() {
            checkPermissions()
            if (isLocationEnabled) {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation: Location? ->
                    lastLocation?.let {
                        latitude = it.latitude
                        longitude = it.longitude
                    } ?: run {
                        getLocation()
                        Log.d(TAG, "Location updated: Latitude: $latitude, Longitude: $longitude")
                    }
                    val intent = Intent(this@LocationActivity, ShuffleFilterActivity::class.java)
                    intent.putExtra("latitude", latitude)
                    intent.putExtra("longitude", longitude)
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(this, "Please turn location services on", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestRuntimePermissions()
        }
    }

    private fun requestRuntimePermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
            ), LOCATION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
            val intent = Intent(this@LocationActivity, ShuffleFilterActivity::class.java)
            intent.putExtra("latitude", latitude)
            intent.putExtra("longitude", longitude)
            startActivity(intent)
            finish()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Services")
            .setMessage("Location services are off. Do you want to continue without location services?")
            .setPositiveButton("Continue") { _, _ ->
                val intent = Intent(this@LocationActivity, ShuffleFilterActivity::class.java)
                intent.putExtra("latitude", latitude)
                intent.putExtra("longitude", longitude)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Turn On") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        val locationRequest = LocationRequest.Builder(UPDATE_INTERVAL)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setIntervalMillis(UPDATE_INTERVAL)
            .setMaxUpdateDelayMillis(UPDATE_INTERVAL)
            .build()
        if (isLocationEnabled) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback,
                Looper.myLooper()!!
            )
        } else {
            Toast.makeText(this, "Please turn location services on", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                latitude = it.latitude
                longitude = it.longitude
                Log.d(TAG, "Location updated: Latitude: $latitude, Longitude: $longitude")
            }
        }
    }

    private val isLocationEnabled: Boolean
        get() {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        }

    companion object {
        private const val TAG = "LocationActivity"
        private const val UPDATE_INTERVAL: Long = 60 * 60 * 1000
        private const val LOCATION_REQUEST = 101
    }
}
