package com.example.captureit.activities

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.captureit.R
import com.example.captureit.databinding.ActivityImagePreviewBinding

/**
 * ImagePreviewActivity displays an image preview. It handles loading the image, rotating it based on EXIF orientation,
 * and displaying it in an ImageView.
 */
class ImagePreviewActivity : AppCompatActivity() {

    // View binding for the image preview layout
    private lateinit var binding: ActivityImagePreviewBinding

    /**
     * Called when the activity is created. It initializes the layout, handles edge-to-edge layout for modern devices,
     * and loads the image passed in the intent.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle edge-to-edge layout for modern devices
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve the image path from the intent
        val imagePath = intent.getStringExtra("imagePath")
        if (imagePath == null) {
            Toast.makeText(this, "Image not found", Toast.LENGTH_LONG).show()
        }

        // Decode the image and set it on the ImageView
        val bitmap = BitmapFactory.decodeFile(filesDir.absolutePath + "/" + imagePath)
        binding.previewImage.setImageBitmap(bitmap)
    }
}
