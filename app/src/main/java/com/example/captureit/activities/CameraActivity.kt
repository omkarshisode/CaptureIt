package com.example.captureit.activities

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.captureit.R
import com.example.captureit.databinding.ActivityCameraBinding
import com.example.captureit.helper.CustomOverlayView
import com.example.captureit.helper.PermissionHelper
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService

/**
 * CameraActivity provides the functionality to capture photos and save them to the device's media store.
 * It uses CameraX to interface with the camera, handling permissions and displaying a live camera preview.
 * The captured photo can be cropped based on the viewPort and saved as a JPEG.
 */
class CameraActivity : AppCompatActivity() {

    // View binding for the camera activity layout
    private var binding: ActivityCameraBinding? = null

    // Camera properties
    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService? = null

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        // Request code for camera permission
        private const val REQUEST_CODE = 1
    }

    /**
     * Called when the activity is created. Sets up the camera and permission handling.
     * It initializes the camera preview and sets up the capture button to take a photo.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // Handle edge-to-edge layout for modern devices
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val permissionHelper = PermissionHelper(this)

        // Check if camera permission is granted and start camera if true
        if (permissionHelper.isCameraPermissionGranted()) {
            startCamera()
        } else {
            permissionHelper.requestCameraPermission(REQUEST_CODE)
        }

        // Set click listener to capture a photo
        binding?.captureButton?.setOnClickListener { takePhoto() }
    }

    /**
     * Initializes the camera and binds the preview and image capture use cases.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Select the camera (back camera in this case)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Build the preview and image capture use cases
            val preview = Preview.Builder().build()
            imageCapture = ImageCapture.Builder().build()

            try {
                // Unbind any previously bound use cases
                cameraProvider.unbindAll()

                // Bind the use cases to the lifecycle of the activity
                cameraProvider.bindToLifecycle(
                    this, // LifecycleOwner
                    cameraSelector,
                    preview,
                    imageCapture
                )

                // Attach the preview to the surface provider of the viewFinder
                preview.surfaceProvider = binding?.viewFinder?.surfaceProvider

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Captures a photo when the capture button is clicked and saves it to the media store.
     */
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Prepare output options with media store URI and file metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues()
            )
            .build()

        // Set up the listener to handle the result of the photo capture
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    // After capture, crop and save the image
                    output.savedUri?.let { cropAndSaveImage(it) }
                }
            }
        )
    }

    /**
     * Crops the captured image according to the viewport's dimensions and saves it as a file.
     *
     * @param imageUri URI of the captured image
     */
    private fun cropAndSaveImage(imageUri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val customOverlayView = findViewById<CustomOverlayView>(R.id.viewPort)
            val rect = customOverlayView.getViewportRect()

            // Scale factors to adjust for bitmap and viewport size differences
            val scaleX = bitmap.width.toFloat() / customOverlayView.width
            val scaleY = bitmap.height.toFloat() / customOverlayView.height

            // Adjust the viewport rectangle coordinates to match the bitmap's resolution
            val adjustedLeft = (rect?.left!! * scaleX).toInt()
            val adjustedTop = (rect.top * scaleY).toInt()
            val adjustedWidth = (rect.width() * scaleX).toInt()
            val adjustedHeight = (rect.height() * scaleY).toInt()

            // Create a new cropped bitmap
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                adjustedLeft.coerceIn(0, bitmap.width),
                adjustedTop.coerceIn(0, bitmap.height),
                adjustedWidth.coerceAtMost(bitmap.width - scaleY.toInt()),
                adjustedHeight.coerceAtMost(bitmap.height - scaleX.toInt())
            )

            // Save the cropped image to a file
            val fileName = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
            val outputStream = openFileOutput(fileName, MODE_PRIVATE)
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
            Toast.makeText(this, "Image saved: $fileName", Toast.LENGTH_SHORT).show()

            // Launch the ImagePreviewActivity to preview the saved image
            val intent = Intent(this, ImagePreviewActivity::class.java)
            intent.putExtra("imagePath", fileName)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString())
        }
    }

    /**
     * Handles the result of the permission request. Starts the camera if permission is granted.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE -> startCamera()
            else -> {
                Toast.makeText(
                    this,
                    "Permissions not granted. Please grant the requested permissions!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Releases the camera executor when the activity is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }
}
