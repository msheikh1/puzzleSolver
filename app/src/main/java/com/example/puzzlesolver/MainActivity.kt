package com.example.puzzlesolver

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class ImageProcessor {
    init {
        OpenCVLoader.initDebug()
    }

    fun preprocessImage(bitmap: Bitmap): Mat {
        val mat = Mat()
        org.opencv.android.Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)

        // Apply Gaussian Blur
        Imgproc.GaussianBlur(mat, mat, Size(5.0, 5.0), 0.0)

        // Apply Adaptive Thresholding
        Imgproc.adaptiveThreshold(mat, mat, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2.0)

        return mat
    }
}
