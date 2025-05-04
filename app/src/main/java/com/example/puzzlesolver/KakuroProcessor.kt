package com.example.puzzlesolver

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.example.puzzlesolver.KakuroProcessor.KakuroCell
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class KakuroProcessor(private val context: Context) : PuzzleProcessor {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val TAG = "KakuroProcessor"

    enum class KakuroCellType {
        BLOCKED, CLUE_ONE, CLUE_TWO, EMPTY
    }

    data class KakuroCell(
        val row: Int,
        val col: Int,
        var type: KakuroCellType = KakuroCellType.EMPTY,
        var clues: List<String> = listOf(),
        var downRuns: MutableList<Run> = mutableListOf(),
        var acrossRuns: MutableList<Run> = mutableListOf(),
        var number: Int = 0  // 0 represents empty
    )

    suspend fun extractKakuroCells(bitmap: Bitmap): Array<Array<KakuroCell>> {
        val cellWidth = bitmap.width / 5
        val cellHeight = bitmap.height / 5
        val kakuroBoard = Array(5) { row -> Array(5) { col -> KakuroCell(row, col) } }

        for (row in 0 until 5) {
            for (col in 0 until 5) {
                val cellBitmap = Bitmap.createBitmap(
                    bitmap,
                    col * cellWidth,
                    row * cellHeight,
                    cellWidth,
                    cellHeight
                )
                kakuroBoard[row][col] = processCell(cellBitmap, row, col)
            }
        }

        kakuroBoard.printToConsole()
        return kakuroBoard
    }

    private suspend fun processCell(cellBitmap: Bitmap, row: Int, col: Int): KakuroCell {
        val mat = Mat()
        Utils.bitmapToMat(cellBitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)

        // Use Canny or line detection to check for a slash
        val edges = Mat()
        Imgproc.Canny(mat, edges, 50.0, 150.0)

        val hasSlash = detectSlash(edges)

        val preprocessedBitmap = preprocessForOCR(mat)
        val image = InputImage.fromBitmap(preprocessedBitmap, 0)
        val result = textRecognizer.process(image).await()

        val text = result.text.trim()
        val digits = text.split(Regex("[^\\d]+")).filter { it.isNotBlank() }

        return when {
            hasSlash && digits.isEmpty() -> KakuroCell(row, col, KakuroCellType.BLOCKED)
            hasSlash && digits.size == 1 -> KakuroCell(row, col, KakuroCellType.CLUE_ONE, digits)
            hasSlash && digits.size >= 2 -> KakuroCell(row, col, KakuroCellType.CLUE_TWO, digits)
            else -> KakuroCell(row, col, KakuroCellType.EMPTY)
        }
    }

    private fun detectSlash(edgeMat: Mat): Boolean {
        val lines = Mat()
        Imgproc.HoughLinesP(edgeMat, lines, 1.0, Math.PI / 180, 20, 20.0, 10.0)

        for (i in 0 until lines.rows()) {
            val line = lines.get(i, 0)
            val x1 = line[0]
            val y1 = line[1]
            val x2 = line[2]
            val y2 = line[3]

            val angle = Math.toDegrees(Math.atan2((y2 - y1), (x2 - x1)))
            if (angle in 40.0..50.0 || angle in -50.0..-40.0) {
                return true
            }
        }
        return false
    }

    private fun preprocessForOCR(mat: Mat): Bitmap {
        Imgproc.GaussianBlur(mat, mat, Size(3.0, 3.0), 0.0)
        Imgproc.adaptiveThreshold(mat, mat, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2.0)

        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    override suspend fun process(bitmap: Bitmap): Bitmap {
        // Optionally highlight cells or return preprocessed board
        return bitmap
    }

    override fun showNoGridFoundAlert() {
        Toast.makeText(context, "Unable to process Kakuro grid", Toast.LENGTH_SHORT).show()
    }

}

fun Array<Array<KakuroProcessor.KakuroCell>>.printToConsole() {
    forEach { row ->
        val rowString = row.joinToString(" | ") { cell ->
            val typeChar = cell.type.name.first().toString()
            val cluesStr = if (cell.clues.isNotEmpty()) cell.clues.joinToString(",") else ""
            "$typeChar(${cluesStr})"
        }
        Log.d("KakuroProcessor", rowString)
    }
}


