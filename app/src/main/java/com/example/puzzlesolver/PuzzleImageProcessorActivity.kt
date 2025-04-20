// SudokuProcessingActivity.kt
package com.example.puzzlesolver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader

class PuzzleImageProcessorActivity : AppCompatActivity() {
    private lateinit var processedImageView: ImageView
    private lateinit var solveButton: Button
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var sudokuBoard: Array<Array<SudokuProcessor.SudokuCell>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle_processing)

        processedImageView = findViewById(R.id.processedImageView)
        solveButton = findViewById(R.id.solveButton)

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed.")
            Toast.makeText(this, "OpenCV initialization failed", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("OpenCV", "OpenCV initialized successfully.")

        // Load test image
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.sudoku_test)

        // Show original image first for debugging
        processedImageView.setImageBitmap(bitmap)

        // Process after a short delay to ensure OpenCV is fully loaded
        processedImageView.postDelayed({
            processImage(bitmap)
        }, 500)

        solveButton.setOnClickListener {
            sudokuBoard?.let { board ->
                coroutineScope.launch {
                    val processor = SudokuProcessor(this@PuzzleImageProcessorActivity)
                    val isSolved = processor.solveSudoku(board)
                    if (isSolved) {
                        board.printToConsole()
                        Toast.makeText(
                            this@PuzzleImageProcessorActivity,
                            "Sudoku solved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@PuzzleImageProcessorActivity,
                            "Failed to solve Sudoku",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } ?: run {
                Toast.makeText(
                    this,
                    "Please process the Sudoku first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun processImage(originalBitmap: Bitmap) {
        try {
            val puzzleType = "sudoku" // Change this to test different processors

            val processor = when {
                puzzleType.contains("sudoku", ignoreCase = true) -> SudokuProcessor(this)
                puzzleType.contains("kakuro", ignoreCase = true) -> KakuroProcessor(this)
                puzzleType.contains("nonogram", ignoreCase = true) -> NonogramProcessor(this)
                else -> null
            }

            if (processor != null && processor is SudokuProcessor) {
                // First process the image to get the cleaned bitmap
                val processedBitmap = processor.process(originalBitmap)
                runOnUiThread {
                    processedImageView.setImageBitmap(processedBitmap)
                    Toast.makeText(this, "Processing complete", Toast.LENGTH_SHORT).show()
                }

                // Then extract the numbers using coroutine
                coroutineScope.launch {
                    try {
                        sudokuBoard = processor.extractSudokuNumbers(processedBitmap)
                        Log.d("SudokuProcessor", "Numbers extracted successfully")
                    } catch (e: Exception) {
                        Log.e("SudokuProcessor", "Error extracting numbers", e)
                        runOnUiThread {
                            Toast.makeText(
                                this@PuzzleImageProcessorActivity,
                                "Error extracting numbers: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Unsupported puzzle type", Toast.LENGTH_SHORT).show()
                    processedImageView.setImageBitmap(originalBitmap)
                }
            }
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Error processing image", e)
            runOnUiThread {
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
                processedImageView.setImageBitmap(originalBitmap)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}


//val imageUri = Uri.parse(imageUriString)
//val bitmap = uriToBitmap(imageUri) ?: run {
//    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
//    finish()
//    return
//}
//PuzzleClassifier.initialize(this)
//
//// Get image URI from intent
//val imageUriString = intent.getStringExtra("imageUri") ?: run {
//    Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
//    finish()
//    return
//}
//val (prediction, confidence) = PuzzleClassifier.classify(bitmap)
//val confidencePercent = (confidence * 100).toInt()
//
//runOnUiThread {
//    Toast.makeText(
//        this,
//        "Prediction: $prediction\nConfidence: $confidencePercent%",
//        Toast.LENGTH_LONG
//    ).show()
//
//    val processedBitmap = when {
//        prediction.contains("sudoku", ignoreCase = true) -> SudokuProcessor(this).process(bitmap)
//        prediction.contains("kakuro", ignoreCase = true) -> KakuroProcessor(this).process(bitmap)
//        prediction.contains("nonogram", ignoreCase = true) -> NonogramProcessor(this).process(bitmap)
//        prediction == "unknown" -> {
//            Toast.makeText(
//                this,
//                "Unrecognized puzzle type. Try another image.",
//                Toast.LENGTH_SHORT
//            ).show()
//            bitmap
//        }
//        else -> {
//            Toast.makeText(this, "Unexpected label: $prediction", Toast.LENGTH_SHORT)
//                .show()
//            bitmap
//        }
//    }