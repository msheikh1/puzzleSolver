package com.example.puzzlesolver

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class BinaryProcessor(private val context: Context) : PuzzleProcessor {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val TAG = "BinaryPuzzleProcessor"
    private val debugDir by lazy { createDebugDirectory() }
    private val DEBUG_MODE = true
    enum class BinaryCellState {
        ZERO, ONE, EMPTY
    }

    data class BinaryCell(
        val row: Int,
        val col: Int,
        var state: BinaryCellState = BinaryCellState.EMPTY,
        var isFixed: Boolean = false
    ) {
        // Add this property to handle numeric values
        var value: Int
            get() = when (state) {
                BinaryCellState.ZERO -> 0
                BinaryCellState.ONE -> 1
                BinaryCellState.EMPTY -> -1
            }
            set(newValue) {
                state = when (newValue) {
                    0 -> BinaryCellState.ZERO
                    1 -> BinaryCellState.ONE
                    else -> BinaryCellState.EMPTY
                }
            }
    }

    private fun createDebugDirectory(): File {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            // Use public Downloads directory for easier access
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "PuzzleSolver_Debug").apply { mkdirs() }
        } else {
            // Fallback to app-specific storage
            context.getExternalFilesDir(null)?.apply { mkdirs() } ?: context.filesDir
        }
    }

    private fun saveDebugImage(bitmap: Bitmap, prefix: String) {
        if (!DEBUG_MODE) return

        try {
            // Save to app-specific files directory (no permissions needed)
            val dir = context.filesDir
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                Log.d(TAG, "Debug image saved to app storage: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Debug image save failed (this is non-critical)", e)
        }
    }

    suspend fun extractBinaryPuzzle(bitmap: Bitmap): Array<Array<BinaryCell>> {
        Log.d(TAG, "Starting puzzle extraction")
        saveDebugImage(bitmap, "original_input")

        val gridSize = 10
        val cellWidth = bitmap.width / gridSize
        val cellHeight = bitmap.height / gridSize

        // Validate cell size
        require(cellWidth > 10 && cellHeight > 10) {
            "Cell size too small (${cellWidth}x$cellHeight)"
        }

        val binaryBoard = Array(gridSize) { row ->
            Array(gridSize) { col -> BinaryCell(row, col) }
        }

        coroutineScope {
            (0 until gridSize).map { row ->
                (0 until gridSize).map { col ->
                    async {
                        val cellBitmap = try {
                            // Define padding (10% of cell size)
                            val padding = (cellWidth * 0.2f).toInt()

                            // Calculate crop coordinates with bounds checking
                            val left = (col * cellWidth + padding).coerceAtLeast(0)
                            val top = (row * cellHeight + padding).coerceAtLeast(0)
                            val right = (left + cellWidth - 2*padding).coerceAtMost(bitmap.width)
                            val bottom = (top + cellHeight - 2*padding).coerceAtMost(bitmap.height)
                            val width = right - left
                            val height = bottom - top

                            // Only proceed if we have valid dimensions
                            if (width > 0 && height > 0) {
                                Bitmap.createBitmap(bitmap, left, top, width, height)
                            } else {
                                Log.w(TAG, "Invalid cell dimensions at [$row,$col]")
                                null
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error cropping cell [$row,$col]", e)
                            null
                        }.also {
                            if (it != null) {
                                saveDebugImage(it, "cell_raw_${row}_${col}")
                            }
                        }
                        binaryBoard[row][col] = cellBitmap?.let { processBinaryCell(it, row, col) }!!
                    }
                }
            }.flatten().awaitAll()
        }

        binaryBoard.printToConsole()
        Log.d(TAG, "Puzzle extraction completed")
        return binaryBoard
    }

    private suspend fun processBinaryCell(cellBitmap: Bitmap, row: Int, col: Int): BinaryCell {
        return try {
            val mat = Mat().apply {
                Utils.bitmapToMat(cellBitmap, this)
                Imgproc.cvtColor(this, this, Imgproc.COLOR_BGR2GRAY)
                saveMatAsDebugImage(this, "gray_${row}_${col}")

                // Enhanced preprocessing
                Imgproc.GaussianBlur(this, this, Size(5.0, 5.0), 0.0)
                saveMatAsDebugImage(this, "blur_${row}_${col}")

                Imgproc.threshold(this, this, 0.0, 255.0,
                    Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
                saveMatAsDebugImage(this, "thresh_${row}_${col}")

                // Improved empty cell detection

                Imgproc.dilate(this, this,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(1.5, 1.5)))
                saveMatAsDebugImage(this, "dilated_${row}_${col}")
            }

            // Convert the processed Mat back to Bitmap for ML Kit
            val processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(mat, processedBitmap)
            saveDebugImage(processedBitmap, "final_${row}_${col}")

            // Use ML Kit to recognize text in the cell
            val inputImage = InputImage.fromBitmap(processedBitmap, 0)
            val result = try {
                textRecognizer.process(inputImage).await()
            } catch (e: Exception) {
                Log.e(TAG, "Text recognition failed for cell [$row,$col]", e)
                return BinaryCell(row, col, BinaryCellState.EMPTY, false)
            }

            // Determine cell state based on recognized text
            val recognizedText = result.text.trim()
            Log.d(TAG, "Cell [$row,$col] recognized text: '$recognizedText'")

            val cellState = when {
                recognizedText.equals("0", ignoreCase = true) -> {
                    BinaryCellState.ZERO
                }
                recognizedText.equals("1", ignoreCase = true) -> {
                    BinaryCellState.ONE
                }
                recognizedText.equals("o", ignoreCase = true) -> {
                    // Handle case where 'O' might be mistaken for zero
                    BinaryCellState.ZERO
                }
                recognizedText.equals("l", ignoreCase = true) -> {
                    // Handle case where 'L' might be mistaken for one
                    BinaryCellState.ONE
                }
                else -> {
                    Log.d(TAG, "Cell [$row,$col] empty or unrecognized: '$recognizedText'")
                    BinaryCellState.EMPTY
                }
            }

            // Determine if the cell is fixed (pre-filled) or empty
            val isFixed = cellState != BinaryCellState.EMPTY

            BinaryCell(row, col, cellState, isFixed)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing cell [$row,$col]", e)
            BinaryCell(row, col, BinaryCellState.EMPTY, false)
        }
    }



    private fun saveMatAsDebugImage(mat: Mat, prefix: String) {
        Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888).run {
            Utils.matToBitmap(mat, this)
            saveDebugImage(this, prefix)
            recycle()
        }
    }

    override suspend fun process(bitmap: Bitmap): Bitmap = bitmap

    override fun showNoGridFoundAlert() {
        Toast.makeText(context, "Unable to process Binary Puzzle grid", Toast.LENGTH_LONG).show()
    }
}

// Extension function to print the binary puzzle to console
fun Array<Array<BinaryProcessor.BinaryCell>>.printToConsole() {
    forEachIndexed { rowIdx, row ->
        val rowString = row.joinToString(" ") { cell ->
            when (cell.state) {
                BinaryProcessor.BinaryCellState.ZERO -> "0"
                BinaryProcessor.BinaryCellState.ONE -> "1"
                BinaryProcessor.BinaryCellState.EMPTY -> "."
            } + if (cell.isFixed) "" else "?"
        }
        Log.d("BinaryPuzzleProcessor", "Row $rowIdx: $rowString")
    }
}