package com.example.puzzlesolver

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var captureButton: Button
    private lateinit var processButton: Button
    private lateinit var resultTextView: TextView

    private lateinit var imageProcessor: ImageProcessor

    private var capturedImage: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        captureButton = findViewById(R.id.captureButton)
        processButton = findViewById(R.id.processButton)
        resultTextView = findViewById(R.id.resultTextView)

        imageProcessor = ImageProcessor()

        captureButton.setOnClickListener {
            openCamera()
        }

        processButton.setOnClickListener {
            processImageOnly()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun openCamera() {
        cameraLauncher.launch(null)
    }

    private fun processImageOnly() {
        capturedImage?.let { bitmap ->
            val processedMat = imageProcessor.preprocessImage(bitmap) // Returns Mat

            // Convert Mat to Bitmap
            val processedBitmap = Bitmap.createBitmap(
                processedMat.cols(), processedMat.rows(), Bitmap.Config.ARGB_8888
            )
            org.opencv.android.Utils.matToBitmap(processedMat, processedBitmap) // Convert Mat to Bitmap

            imageView.setImageBitmap(processedBitmap)  // Display the processed image
            resultTextView.text = "Image processed successfully!"
        } ?: run {
            resultTextView.text = "Please capture an image first."
        }
    }

}
