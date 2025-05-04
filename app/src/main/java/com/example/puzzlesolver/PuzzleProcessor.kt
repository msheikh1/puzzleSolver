// PuzzleProcessor.kt
package com.example.puzzlesolver

import android.graphics.Bitmap
import org.opencv.core.Mat

interface PuzzleProcessor {
    suspend fun process(bitmap: Bitmap): Bitmap
    fun showNoGridFoundAlert()
}