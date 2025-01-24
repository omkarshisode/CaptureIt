package com.example.captureit.helper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * PermissionHelper is a utility class that simplifies the process of checking and requesting
 * runtime permissions for various functionalities in the app, such as camera, location, and notifications.
 *
 * @param context The context used for checking permissions and requesting them from the system.
 */
class PermissionHelper(private val context: Context) {

    companion object {
        // Define the permissions required by the app
        private val notificationPermission = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        private val locationPermission = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private val cameraPermission = arrayOf(Manifest.permission.CAMERA)
    }

    /**
     * Checks if all the required permissions are granted.
     *
     * @param requiredPermission An array of permissions to check.
     * @return true if all permissions are granted, false otherwise.
     */
    private fun allPermissionRequired(requiredPermission: Array<String>) = requiredPermission.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests the required permissions from the user.
     *
     * @param requiredPermission An array of permissions to request.
     * @param requestCode The request code to identify the permission request in onRequestPermissionsResult.
     */
    private fun requestPermission(requiredPermission: Array<String>, requestCode: Int) {
        (context as Activity).requestPermissions(requiredPermission, requestCode)
    }

    /**
     * Checks if notification permission is granted.
     *
     * @return true if notification permission is granted, false otherwise.
     */
    fun isNotificationPermissionGranted(): Boolean {
        return allPermissionRequired(notificationPermission)
    }

    /**
     * Requests notification permission from the user.
     *
     * @param requestCode The request code to identify the notification permission request.
     */
    fun requestNotificationPermission(requestCode: Int) {
        requestPermission(notificationPermission, requestCode)
    }

    /**
     * Checks if location permissions are granted (both fine and coarse location).
     *
     * @return true if location permissions are granted, false otherwise.
     */
    fun isLocationPermissionGranted(): Boolean {
        return allPermissionRequired(locationPermission)
    }

    /**
     * Requests location permissions from the user.
     *
     * @param requestCode The request code to identify the location permission request.
     */
    fun requestLocationPermission(requestCode: Int) {
        requestPermission(locationPermission, requestCode)
    }

    /**
     * Checks if camera permission is granted.
     *
     * @return true if camera permission is granted, false otherwise.
     */
    fun isCameraPermissionGranted(): Boolean {
        return allPermissionRequired(cameraPermission)
    }

    /**
     * Requests camera permission from the user.
     *
     * @param requestCode The request code to identify the camera permission request.
     */
    fun requestCameraPermission(requestCode: Int) {
        requestPermission(cameraPermission, requestCode)
    }
}
