// NonogramProcessor.kt
package com.example.puzzlesolver

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class NonogramProcessor(private val context: Context) : PuzzleProcessor {
    override fun process(bitmap: Bitmap): Bitmap {
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
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            15,
            2.0
        )

        // Find Contours
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            thresholded, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )

        // Find the largest rectangular contour that is likely to be the Nonogram grid
        var maxArea = 0.0
        var nonogramContour: MatOfPoint? = null
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
                    nonogramContour = contour
                    approxCorners = approx
                }
            }
        }

        if (nonogramContour == null || approxCorners == null) {
            showNoGridFoundAlert()
            return bitmap
        }

        // Draw detected Nonogram grid
        val corners = approxCorners.toArray()
        val orderedCorners = orderPoints(corners)

        // Define the destination points for the perspective transform
        val outputSize = 900 // Size of the output Nonogram grid
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),                         // top-left
            Point(outputSize.toDouble(), 0.0),      // top-right
            Point(outputSize.toDouble(), outputSize.toDouble()), // bottom-right
            Point(0.0, outputSize.toDouble())       // bottom-left
        )

        // Convert source points to MatOfPoint2f
        val srcPoints = MatOfPoint2f(*orderedCorners)

        // Calculate the homography matrix
        val homography = Calib3d.findHomography(srcPoints, dstPoints)

        // Apply the perspective transform
        val warped = Mat()
        Imgproc.warpPerspective(
            mat, warped, homography, Size(outputSize.toDouble(), outputSize.toDouble())
        )

        // Additional processing specific to Nonograms
        val gridLines = detectGridLines(warped)
        val processed = enhanceNonogramImage(warped, gridLines)

        val processedBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(processed, processedBitmap)
        return processedBitmap
    }

    override fun showNoGridFoundAlert() {
        Toast.makeText(context, "No Nonogram grid detected", Toast.LENGTH_SHORT).show()
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

    private fun detectGridLines(image: Mat): Pair<List<Double>, List<Double>> {
        // Convert to grayscale if needed
        val gray = Mat()
        if (image.channels() > 1) {
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            gray.assignTo(image)
        }

        // Apply threshold
        val binary = Mat()
        Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)

        // Detect horizontal and vertical lines
        val horizontal = detectLines(binary, Imgproc.MORPH_RECT, Size(binary.cols() / 2.0, 1.0))
        val vertical = detectLines(binary, Imgproc.MORPH_RECT, Size(1.0, binary.rows() / 2.0))

        return Pair(horizontal, vertical)
    }

    private fun detectLines(image: Mat, morphShape: Int, kernelSize: Size): List<Double> {
        val kernel = Imgproc.getStructuringElement(morphShape, kernelSize)
        val morphed = Mat()
        Imgproc.morphologyEx(image, morphed, Imgproc.MORPH_CLOSE, kernel)

        // Project the image to one axis
        val projection = Mat()
        Core.reduce(morphed, projection, if (kernelSize.width > kernelSize.height) 1 else 0, Core.REDUCE_AVG)

        // Find peaks in the projection (grid lines)
        val lines = mutableListOf<Double>()
        val threshold = 50.0 // Adjust based on your needs
        for (i in 0 until projection.cols()) {
            val value = projection.get(0, i)[0]
            if (value > threshold) {
                lines.add(i.toDouble())
            }
        }

        return lines
    }

    private fun enhanceNonogramImage(image: Mat, gridLines: Pair<List<Double>, List<Double>>): Mat {
        val (horizontalLines, verticalLines) = gridLines

        // Create a mask for the grid cells
        val mask = Mat.zeros(image.size(), CvType.CV_8UC1)

        // Draw grid lines on the mask
        for (y in horizontalLines) {
            Imgproc.line(mask, Point(0.0, y), Point(mask.cols().toDouble(), y), Scalar(255.0), 2)
        }
        for (x in verticalLines) {
            Imgproc.line(mask, Point(x, 0.0), Point(x, mask.rows().toDouble()), Scalar(255.0), 2)
        }

        // Enhance the contrast of the original image
        val enhanced = Mat()
        Imgproc.cvtColor(image, enhanced, Imgproc.COLOR_BGR2GRAY)
        Imgproc.equalizeHist(enhanced, enhanced)

        // Combine with the mask
        val result = Mat()
        Core.bitwise_and(enhanced, enhanced, result, mask)

        return result
    }
}