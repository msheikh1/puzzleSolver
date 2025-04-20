package com.example.puzzlesolver

import android.content.Context
import android.graphics.*
import android.util.Log
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import kotlin.math.roundToInt

class SudokuProcessor(private val context: Context) : PuzzleProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    var sudokuBoard: Array<IntArray> = Array(9) { IntArray(9) { 0 } }

    override fun process(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGRA2GRAY)

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

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresholded, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        var maxArea = 0.0
        var approxCorners: MatOfPoint2f? = null

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area > maxArea) {
                val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.02 * peri, true)

                if (approx.toArray().size == 4) {
                    maxArea = area
                    approxCorners = approx
                }
            }
        }

        if (approxCorners == null) {
            showNoGridFoundAlert()
            return bitmap
        }

        val corners = approxCorners.toArray()
        val orderedCorners = orderPoints(corners)

        val sideLength = 900
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(sideLength.toDouble(), 0.0),
            Point(sideLength.toDouble(), sideLength.toDouble()),
            Point(0.0, sideLength.toDouble())
        )

        val srcPoints = MatOfPoint2f(*orderedCorners)
        val homography = Calib3d.findHomography(srcPoints, dstPoints)

        val warped = Mat()
        Imgproc.warpPerspective(mat, warped, homography, Size(sideLength.toDouble(), sideLength.toDouble()))

        val warpedBitmap = Bitmap.createBitmap(sideLength, sideLength, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warped, warpedBitmap)

        extractDigitsFromGrid(warpedBitmap)

        return warpedBitmap
    }

    private fun extractDigitsFromGrid(bitmap: Bitmap) {
        val cellSize = bitmap.width / 9
        sudokuBoard = Array(9) { IntArray(9) { 0 } }

        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val x = col * cellSize
                val y = row * cellSize
                val cellBitmap = Bitmap.createBitmap(bitmap, x, y, cellSize, cellSize)

                val preprocessedCell = preprocessCell(cellBitmap)
                val inputImage = InputImage.fromBitmap(preprocessedCell, 0)

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val detected = visionText.textBlocks.joinToString("") { it.text }
                        val number = detected.trim().toIntOrNull() ?: 0
                        sudokuBoard[row][col] = number
                        Log.d("SudokuCell", "[$row][$col] = $number")
                    }
                    .addOnFailureListener {
                        Log.e("SudokuError", "Failed to recognize digit at [$row][$col]")
                    }
            }
        }
    }

    private fun preprocessCell(cellBitmap: Bitmap): Bitmap {
        val resized = Bitmap.createScaledBitmap(cellBitmap, 100, 100, true)
        val mat = Mat()
        Utils.bitmapToMat(resized, mat)

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(mat, mat, Size(3.0, 3.0), 0.0)
        Imgproc.adaptiveThreshold(
            mat, mat, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV, 11, 2.0
        )

        val processedBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, processedBitmap)
        return processedBitmap
    }


    override fun showNoGridFoundAlert() {
        Toast.makeText(context, "No Sudoku grid detected", Toast.LENGTH_SHORT).show()
    }

    private fun orderPoints(points: Array<Point>): Array<Point> {
        points.sortBy { it.y }
        val topRow = arrayOf(points[0], points[1]).sortedBy { it.x }.toTypedArray()
        val bottomRow = arrayOf(points[2], points[3]).sortedBy { it.x }.toTypedArray()
        return arrayOf(
            topRow[0], topRow[1],
            bottomRow[1], bottomRow[0]
        )
    }
}
