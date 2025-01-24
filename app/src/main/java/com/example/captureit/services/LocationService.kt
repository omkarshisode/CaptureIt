package com.example.captureit.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.captureit.R
import com.example.captureit.helper.PermissionHelper
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * A foreground service that tracks user location and saves it to a CSV file.
 * The service broadcasts location updates and maintains a persistent notification.
 */
class LocationService : Service() {

    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private val csvFileName = "location_data_${System.currentTimeMillis()}.csv"
    private var locationListener: LocationListener? = null

    private val permissionHelper = PermissionHelper(this)

    companion object {
        private const val CHANNEL_ID = "location_service_channel"
        private const val NOTIFICATION_ID = 1

        // Broadcast action for location updates
        const val LOCATION_UPDATE_ACTION = "com.example.captureit.LOCATION_UPDATE"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for foreground location service"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ServiceAction.START_SERVICE.toString() -> startForegroundService()
            ServiceAction.STOP_SERVICE.toString() -> stopService()
        }
        return START_NOT_STICKY
    }

    /**
     * Starts the foreground service and begins location updates.
     */
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Tracking your location in real-time.")
            .setSmallIcon(R.drawable.baseline_add_location_alt_24)
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        startLocationUpdate()
    }

    /**
     * Stops the service and cleans up resources.
     */
    private fun stopService() {
        stopForeground(true)
        locationListener?.let {
            locationManager.removeUpdates(it)
        }
        stopSelf()
    }

    /**
     * Begins requesting location updates from the LocationManager.
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdate() {
        locationListener = LocationListener { location ->
            updateNotification(location)
            saveLocationToCSV(location)
            broadcastLocationUpdate(location)
        }

        try {
            locationListener?.let {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    0f,
                    it
                )
            }
        } catch (e: SecurityException) {
            Log.e("LocationService", "Failed to request location updates: ${e.message}", e)
        }
    }

    /**
     * Saves location data to a CSV file.
     */
    private fun saveLocationToCSV(location: Location) {
        try {
            val file = File(filesDir, csvFileName)
            val writer = FileWriter(file, true)
            writer.append("${System.currentTimeMillis()},${location.latitude},${location.longitude}\n")
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            Log.e("LocationService", "Failed to save location to CSV: ${e.message}", e)
        }
    }

    /**
     * Updates the persistent notification with the latest location.
     */
    @SuppressLint("MissingPermission")
    private fun updateNotification(location: Location) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Lat: ${location.latitude}, Lon: ${location.longitude}")
            .setSmallIcon(R.drawable.baseline_add_location_alt_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        if (permissionHelper.isNotificationPermissionGranted()) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Broadcasts the updated location to other components.
     *
     * @param location The updated location.
     */
    private fun broadcastLocationUpdate(location: Location) {
        val intent = Intent(LOCATION_UPDATE_ACTION).apply {
            putExtra(EXTRA_LATITUDE, location.latitude)
            putExtra(EXTRA_LONGITUDE, location.longitude)
        }
        sendBroadcast(intent)
    }

    /**
     * Cleans up resources when the service is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        locationListener?.let {
            locationManager.removeUpdates(it)
        }
        Log.i("LocationService", "Service destroyed, location updates stopped.")
    }
}
