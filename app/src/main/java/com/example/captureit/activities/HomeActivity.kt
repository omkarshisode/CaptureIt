package com.example.captureit.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.captureit.R
import com.example.captureit.databinding.ActivityMainBinding
import com.example.captureit.helper.PermissionHelper

/**
 * HomeActivity serves as the entry point of the application after a user logs in or launches the app.
 * This activity manages the main UI, handles permissions, and provides navigation to other features like Camera.
 */
class HomeActivity : AppCompatActivity() {

    /** View binding for accessing views in the activity layout */
    private var binding: ActivityMainBinding? = null

    /** Helper for managing runtime permissions */
    private val permissionHelper = PermissionHelper(this)

    /**
     * Called when the activity is first created.
     * This initializes the UI, sets up edge-to-edge display, and checks for necessary permissions.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state, if any.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enable full-screen immersive mode
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // Adjust the padding to account for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Request notification permissions if not already granted
        if (!permissionHelper.isNotificationPermissionGranted()) {
            permissionHelper.requestNotificationPermission(REQUEST_CODE_NOTIFICATION)
        }

        // Set up button click listener for launching the CameraActivity
        binding?.btnCamera?.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Handles the result of permission requests.
     * Specifically checks for notification permissions and requests location permissions if needed.
     *
     * @param requestCode The request code passed in the permission request.
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_NOTIFICATION -> {
                // If notification permission is granted, check location permissions
                if (!permissionHelper.isLocationPermissionGranted()) {
                    permissionHelper.requestLocationPermission(REQUEST_CODE_LOCATION)
                }
            }
        }
    }

    /**
     * Cleans up the activity resources when it's destroyed.
     * Ensures that the binding reference is cleared to prevent memory leaks.
     */
    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATION = 2
        private const val REQUEST_CODE_LOCATION = 3
    }
}
