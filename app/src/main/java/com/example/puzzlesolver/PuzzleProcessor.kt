// PuzzleProcessor.kt
package com.example.puzzlesolver

import android.graphics.Bitmap
import org.opencv.core.Mat

interface PuzzleProcessor {
    fun process(bitmap: Bitmap): Bitmap
    fun showNoGridFoundAlert()
}