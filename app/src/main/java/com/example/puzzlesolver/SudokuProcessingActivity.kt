package com.example.puzzlesolver

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

class SudokuProcessingActivity : AppCompatActivity() {
    private lateinit var processedImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sudoku_processing)

        processedImageView = findViewById(R.id.processedImageView)

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed.")
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully.")
        }

        // Get image URI from intent
        val imageUriString = intent.getStringExtra("imageUri")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            val bitmap = uriToBitmap(imageUri)

            // Process the image
            val processedBitmap = processSudoku(bitmap)
            processedImageView.setImageBitmap(processedBitmap)
        }
    }

    // Convert URI to Bitmap
    private fun uriToBitmap(imageUri: Uri): Bitmap {
        return contentResolver.openInputStream(imageUri)?.use {
            android.graphics.BitmapFactory.decodeStream(it)
        } ?: throw IllegalArgumentException("Failed to decode bitmap from URI")
    }

//         Process the Sudoku Image
    private fun processSudoku(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to Grayscale
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGRA2GRAY)

        // Apply Gaussian Blur
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 3.0)

        // Apply Edge Detection (Canny)
//        val edges = Mat()
//        Imgproc.Canny(blurred, edges, 50.0, 150.0)
//
//        // Apply Morphological Dilation
//        val dilated = Mat()
//        val kernel = Mat.ones(5, 5, CvType.CV_8U) // Create a 3x3 kernel for dilation
//        Imgproc.dilate(edges, dilated, kernel)

        val thresholded = Mat()
        Imgproc.adaptiveThreshold(gray, thresholded, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0
        )

        // Find Contours
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            thresholded, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )

// Find the largest contour that is likely to be the Sudoku grid
        var maxArea = 0.0
        var sudokuContour: MatOfPoint? = null
        var approxCorners: MatOfPoint2f? = null

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > maxArea) {
                val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.02 * peri, true)

                // Check if it's a quadrilateral
                if (approx.toArray().size == 4) {
                    maxArea = area
                    sudokuContour = contour
                    approxCorners = approx
                }
            }
        }

        if (sudokuContour == null) {
            showNoGridFoundAlert()
            return bitmap // Return the original bitmap or any default image if no grid is found
        }
        if (approxCorners == null) {
            showNoGridFoundAlert()
            return bitmap
        }

        // Draw detected Sudoku grid
        val corners = approxCorners.toArray()
        val orderedCorners = orderPoints(corners)

        // Define the destination points for the perspective transform (a square)
        val sideLength = 900 // Size of the output Sudoku grid
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),                         // top-left
            Point(sideLength.toDouble(), 0.0),       // top-right
            Point(sideLength.toDouble(), sideLength.toDouble()), // bottom-right
            Point(0.0, sideLength.toDouble())        // bottom-left
        )

        // Convert source points to MatOfPoint2f
        val srcPoints = MatOfPoint2f(*orderedCorners)

        // Calculate the homography matrix
        val homography = Calib3d.findHomography(srcPoints, dstPoints)

        // Apply the perspective transform
        val warped = Mat()
        Imgproc.warpPerspective(
            mat, warped, homography, Size(sideLength.toDouble(), sideLength.toDouble())
        )

        // Return edge-detected image if no grid is found
        val processedBitmap = Bitmap.createBitmap(sideLength, sideLength, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warped, processedBitmap)

    val classifier = PuzzleClassifier()
    classifier.classify(processedBitmap) { prediction, confidence, error ->
        runOnUiThread {
            if (error != null) {
                Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Prediction: $prediction\nConfidence: $confidence", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Return the processed image (after perspective transformation)
    return processedBitmap

}


    //     Process the Sudoku Image
//        private fun processSudoku(bitmap: Bitmap): Bitmap {
//            val mat = Mat()
//            Utils.bitmapToMat(bitmap, mat)
//
//            // Convert to Grayscale
//            val gray = Mat()
//            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGRA2GRAY)
//
//            // Apply Gaussian Blur
//            val blurred = Mat()
//            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 3.0)
//
//    val thresholded = Mat()
//    Imgproc.adaptiveThreshold(gray, thresholded, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0
//    )
//
//
//            // Convert the dilated image to Bitmap
//            val processedBitmap =
//                Bitmap.createBitmap(thresholded.cols(), thresholded.rows(), Bitmap.Config.ARGB_8888)
//            Utils.matToBitmap(thresholded, processedBitmap)
//
//            // Return the dilated image
//            return processedBitmap
//        }


    private fun showNoGridFoundAlert() {
        Toast.makeText(this, "No Sudoku grid detected", Toast.LENGTH_SHORT).show()
    }

    private fun orderPoints(points: Array<Point>): Array<Point> {
        // Sort points by y-coordinate to get top and bottom rows
        points.sortBy { it.y }

        val topRow = arrayOf(points[0], points[1]).sortedBy { it.x }.toTypedArray()
        val bottomRow = arrayOf(points[2], points[3]).sortedBy { it.x }.toTypedArray()

        return arrayOf(
            topRow[0],  // top-left
            topRow[1],  // top-right
            bottomRow[1], // bottom-right
            bottomRow[0]  // bottom-left
        )
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }




}


