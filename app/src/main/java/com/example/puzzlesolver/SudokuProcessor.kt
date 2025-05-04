package com.example.puzzlesolver

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SudokuProcessor(private val context: Context) : PuzzleProcessor {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val TAG = "SudokuProcessor"

    override suspend fun process(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to Grayscale
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGRA2GRAY)

        // Apply Adaptive Threshold
        val thresholded = Mat()
        Imgproc.adaptiveThreshold(
            gray,
            thresholded,
            255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY_INV,
            11,
            2.0
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
                Imgproc.approxPolyDP(
                    MatOfPoint2f(*contour.toArray()),
                    approx,
                    0.02 * peri,
                    true
                )

                if (approx.toArray().size == 4) {
                    maxArea = area
                    sudokuContour = contour
                    approxCorners = approx
                }
            }
        }

        if (sudokuContour == null || approxCorners == null) {
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

        val processedBitmap = Bitmap.createBitmap(sideLength, sideLength, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warped, processedBitmap)
        return processedBitmap
    }

    suspend fun extractSudokuNumbers(bitmap: Bitmap): Array<Array<SudokuCell>> {
        return try {
            val processedBitmap = process(bitmap)
            val cellWidth = processedBitmap.width / 9
            val cellHeight = processedBitmap.height / 9

            val sudokuBoard = emptySudoku2DArray()

            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    val cellBitmap = Bitmap.createBitmap(
                        processedBitmap,
                        j * cellWidth,
                        i * cellHeight,
                        cellWidth,
                        cellHeight
                    )

                    val preprocessed = preprocessCell(cellBitmap)

                    val image = InputImage.fromBitmap(preprocessed, 0)
                    val result = textRecognizer.process(image).await()

                    val digitText = result.text.trim()
                    val digit = digitText.toIntOrNull() ?: 0

                    sudokuBoard[i][j].let {
                        it.number = digit
                        it.type = if (digit != 0) SUDOKU_CELL_TYPE_GIVEN else SUDOKU_CELL_TYPE_EMPTY
                    }
                }
            }

            sudokuBoard.printToConsole()
            sudokuBoard
        } catch (e: Exception) {
            Log.e(TAG, "Error in extractSudokuNumbers: ${e.message}", e)
            emptySudoku2DArray().also { it.printToConsole() }
        }
    }


    private fun emptySudoku2DArray(): Array<Array<SudokuCell>> {
        return Array(9) { Array(9) { SudokuCell() } }
    }

    override fun showNoGridFoundAlert() {
        Toast.makeText(context, "No Sudoku grid detected", Toast.LENGTH_SHORT).show()
    }

    private fun orderPoints(points: Array<Point>): Array<Point> {
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
    suspend fun solveSudoku(board: Array<Array<SudokuCell>>): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                SudokuSolver.solve(board)
            } catch (e: Exception) {
                Log.e(TAG, "Error solving Sudoku: ${e.message}", e)
                false
            }
        }
    }

    private fun preprocessCell(cellBitmap: Bitmap): Bitmap {
        // Resize for uniformity
        val resized = Bitmap.createScaledBitmap(cellBitmap, 100, 100, true)
        val mat = Mat()
        Utils.bitmapToMat(resized, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)

        // Blur to reduce noise
        Imgproc.GaussianBlur(mat, mat, Size(3.0, 3.0), 0.0)

        // Adaptive thresholding for contrast
        Imgproc.adaptiveThreshold(
            mat, mat, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV, 11, 2.0
        )

        // Dilation to enhance digit lines
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
        Imgproc.dilate(mat, mat, kernel)

        // Convert back to Bitmap
        val processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, processedBitmap)

        return processedBitmap
    }


    data class SudokuCell(
        var number: Int = 0,
        var type: Int = SUDOKU_CELL_TYPE_EMPTY
    )

    companion object {
        const val SUDOKU_CELL_TYPE_EMPTY = 0
        const val SUDOKU_CELL_TYPE_GIVEN = 1
        const val SUDOKU_CELL_TYPE_GUESS = 2
    }
}