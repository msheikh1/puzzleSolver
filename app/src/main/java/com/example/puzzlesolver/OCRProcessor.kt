package com.example.puzzlesolver

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI

class OCRProcessor(private val context: Context) {
    private val tessBaseAPI: TessBaseAPI = TessBaseAPI()

    init {
        val dataPath = context.getExternalFilesDir(null).toString() + "/tesseract/"
        tessBaseAPI.init(dataPath, "eng")
    }

    fun extractTextFromImage(bitmap: Bitmap): String {
        tessBaseAPI.setImage(bitmap)
        return tessBaseAPI.utF8Text
    }

    fun stop() {
        tessBaseAPI.stop()
    }
}