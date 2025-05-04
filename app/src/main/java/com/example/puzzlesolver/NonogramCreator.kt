package com.example.puzzlesolver

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlin.math.pow
import kotlin.math.sqrt

class NonogramCreator : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var captureButton: Button
    private lateinit var convertButton: Button
    private var currentPhotoUri: Uri? = null
    private var nonogramSize = 15 // Default nonogram size (15x15)

    // Register the activity result launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                imageView.setImageURI(uri)
            }
        } else {
            Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_nonogram_creator)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.menu.findItem(R.id.nav_nonogram).isChecked = true // Set default selected item

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }

                R.id.nav_gen_puzzle -> {
                    startActivity(Intent(this, PuzzleGeneratorActivity::class.java))
                    true
                }

                R.id.nav_camera -> {
                    startActivity(Intent(this, PuzzleImageProcessorActivity::class.java))
                    true
                }

                R.id.nav_nonogram -> {
                    true
                }

                else -> false
            }
        }

        imageView = findViewById(R.id.imageView)
        captureButton = findViewById(R.id.captureButton)
        convertButton = findViewById(R.id.convertButton)

        captureButton.setOnClickListener {
            dispatchTakePictureIntent()
        }

        convertButton.setOnClickListener {
            currentPhotoUri?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val nonogram = createNonogramFromBitmap(bitmap)
                    // Start activity to show the generated nonogram
                    val intent = Intent(this, NonogramDisplayActivity::class.java).apply {
                        putExtra("NONOGRAM", nonogram)
                    }
                    startActivity(intent)
                } catch (e: IOException) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "Please capture an image first", Toast.LENGTH_SHORT).show()
            }
        }

        val sizeSelector = findViewById<RadioGroup>(R.id.sizeSelector)
        sizeSelector.setOnCheckedChangeListener { _, checkedId ->
            nonogramSize = when (checkedId) {
                R.id.size15 -> 15
                R.id.size20 -> 20
                R.id.size25 -> 25
                R.id.size30 -> 30
                else -> 15
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val photoFile = try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }

        photoFile?.also {
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                it
            )
            currentPhotoUri = photoURI
            takePictureLauncher.launch(photoURI)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Get the directory for the app's private pictures directory
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_",
            ".jpg",
            storageDir
        )
    }

    private fun createNonogramFromBitmap(bitmap: Bitmap): Nonogram {
        val contrasted = enhanceContrast(bitmap)
        val scaledBitmap = scaleBitmap(contrasted)
        val croppedBitmap = smartCrop(scaledBitmap)
        val processedBitmap = improvedDithering(croppedBitmap)

        val grid = Array(nonogramSize) { IntArray(nonogramSize) }
        for (y in 0 until nonogramSize) {
            for (x in 0 until nonogramSize) {
                grid[y][x] = if (Color.red(processedBitmap.getPixel(x, y)) < 128) 1 else 0
            }
        }
        return Nonogram(rotateGrid90Clockwise(grid))
    }

    private fun rotateGrid90Clockwise(grid: Array<IntArray>): Array<IntArray> {
        val size = grid.size
        val rotated = Array(size) { IntArray(size) }

        for (y in 0 until size) {
            for (x in 0 until size) {
                rotated[x][size - 1 - y] = grid[y][x]
            }
        }

        return rotated
    }

    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val contrastFactor = 1.5f // Adjust this value (1.0 = no change)
        val matrix = floatArrayOf(
            contrastFactor, 0f, 0f, 0f, -128f * (contrastFactor - 1),
            0f, contrastFactor, 0f, 0f, -128f * (contrastFactor - 1),
            0f, 0f, contrastFactor, 0f, -128f * (contrastFactor - 1),
            0f, 0f, 0f, 1f, 0f
        )
        val config = bitmap.config ?: Bitmap.Config.ARGB_8888
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, config)
        val canvas = Canvas(output)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix))
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun scaleBitmap(src: Bitmap): Bitmap {
        val aspectRatio = src.width.toFloat() / src.height.toFloat()
        val (targetWidth, targetHeight) = if (aspectRatio > 1) {
            Pair((nonogramSize * aspectRatio).toInt(), nonogramSize)
        } else {
            Pair(nonogramSize, (nonogramSize / aspectRatio).toInt())
        }

        val matrix = Matrix().apply {
            postScale(targetWidth.toFloat() / src.width, targetHeight.toFloat() / src.height)
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun smartCrop(src: Bitmap): Bitmap {
        val grayBitmap = convertToGrayscale(src)
        val edgeMap = detectEdges(grayBitmap)
        val (x, y) = findBestCropRegion(edgeMap, src.width, src.height)
        return Bitmap.createBitmap(src, x, y, nonogramSize, nonogramSize)
    }

    private fun detectEdges(bitmap: Bitmap): Array<BooleanArray> {
        val width = bitmap.width
        val height = bitmap.height
        val magnitudes = Array(height) { IntArray(width) }

        // Sobel operators
        val sobelX = arrayOf(intArrayOf(-1, 0, 1), intArrayOf(-2, 0, 2), intArrayOf(-1, 0, 1))
        val sobelY = arrayOf(intArrayOf(-1, -2, -1), intArrayOf(0, 0, 0), intArrayOf(1, 2, 1))

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var gx = 0
                var gy = 0
                for (i in -1..1) {
                    for (j in -1..1) {
                        val luminance = Color.red(bitmap.getPixel(x + j, y + i))
                        gx += sobelX[i + 1][j + 1] * luminance
                        gy += sobelY[i + 1][j + 1] * luminance
                    }
                }
                magnitudes[y][x] = sqrt(gx.toDouble() * gx + gy * gy).toInt()
            }
        }

        val threshold = calculateOtsuThreshold(magnitudes)
        return Array(height) { y -> BooleanArray(width) { x -> magnitudes[y][x] > threshold } }
    }

    private fun calculateOtsuThreshold(magnitudes: Array<IntArray>): Int {
        val histogram = IntArray(1024) // Adjust based on max magnitude
        magnitudes.forEach { row -> row.forEach { mag -> if (mag < histogram.size) histogram[mag]++ } }

        var total = magnitudes.size * magnitudes[0].size
        var sum = histogram.indices.sumOf { it * histogram[it] }.toDouble()
        var sumB = 0.0
        var wB = 0
        var threshold = 0
        var maxVariance = 0.0

        for (i in histogram.indices) {
            wB += histogram[i]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break

            sumB += i * histogram[i]
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val variance = wB * wF * (mB - mF).pow(2)

            if (variance > maxVariance) {
                maxVariance = variance
                threshold = i
            }
        }
        return threshold
    }

    private fun findBestCropRegion(
        edgeMap: Array<BooleanArray>,
        srcWidth: Int,
        srcHeight: Int
    ): Pair<Int, Int> {
        var maxSum = 0
        var bestX = (srcWidth - nonogramSize) / 2
        var bestY = (srcHeight - nonogramSize) / 2

        for (y in 0..(srcHeight - nonogramSize)) {
            for (x in 0..(srcWidth - nonogramSize)) {
                var sum = 0
                for (i in 0 until nonogramSize) {
                    for (j in 0 until nonogramSize) {
                        if (edgeMap.getOrNull(y + i)?.getOrNull(x + j) == true) sum++
                    }
                }
                if (sum > maxSum || (sum == maxSum && isCloserToCenter(
                        x,
                        y,
                        srcWidth,
                        srcHeight,
                        bestX,
                        bestY
                    ))
                ) {
                    maxSum = sum
                    bestX = x
                    bestY = y
                }
            }
        }
        return Pair(bestX, bestY)
    }

    private fun isCloserToCenter(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        bestX: Int,
        bestY: Int
    ): Boolean {
        val centerX = (width - nonogramSize) / 2
        val centerY = (height - nonogramSize) / 2
        val currentDist = (x - centerX).toDouble().pow(2) + (y - centerY).toDouble().pow(2)
        val bestDist = (bestX - centerX).toDouble().pow(2) + (bestY - centerY).toDouble().pow(2)
        return currentDist < bestDist
    }

    private fun improvedDithering(bitmap: Bitmap): Bitmap {
        val grayBitmap = convertToGrayscale(bitmap)
        val width = grayBitmap.width
        val height = grayBitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        grayBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Calculate adaptive threshold using Otsu's method
        val threshold = calculateOtsuThreshold(pixels)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val oldPixel = Color.red(pixels[y * width + x])
                val newPixel = if (oldPixel > threshold) 255 else 0
                val quantError = oldPixel - newPixel

                pixels[y * width + x] = Color.rgb(newPixel, newPixel, newPixel)

                // Atkinson dithering (better for line art)
                if (x + 1 < width) pixels[y * width + x + 1] =
                    applyError(pixels[y * width + x + 1], quantError * 1 / 8)
                if (x + 2 < width) pixels[y * width + x + 2] =
                    applyError(pixels[y * width + x + 2], quantError * 1 / 8)
                if (y + 1 < height) {
                    if (x > 0) pixels[(y + 1) * width + x - 1] =
                        applyError(pixels[(y + 1) * width + x - 1], quantError * 1 / 8)
                    pixels[(y + 1) * width + x] =
                        applyError(pixels[(y + 1) * width + x], quantError * 1 / 8)
                    if (x + 1 < width) pixels[(y + 1) * width + x + 1] =
                        applyError(pixels[(y + 1) * width + x + 1], quantError * 1 / 8)
                }
                if (y + 2 < height) {
                    pixels[(y + 2) * width + x] =
                        applyError(pixels[(y + 2) * width + x], quantError * 1 / 8)
                }
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyError(pixel: Int, error: Int): Int {
        val newVal = Color.red(pixel) + error
        return Color.rgb(newVal.coerceIn(0, 255), newVal.coerceIn(0, 255), newVal.coerceIn(0, 255))
    }

    private fun calculateOtsuThreshold(pixels: IntArray): Int {
        val histogram = IntArray(256)

        // Build histogram
        for (pixel in pixels) {
            val luminance = Color.red(pixel)
            histogram[luminance]++
        }

        // Calculate Otsu's threshold
        var sum: Int = pixels.size
        var sumB = 0
        var wB = 0
        var maximum = 0.0
        var threshold = 0

        for (i in 0..255) {
            wB += histogram[i]
            if (wB == 0) continue
            val wF = sum - wB
            if (wF == 0) break

            var sumBF = 0
            for (j in 0..i) sumBF += j * histogram[j]

            val mB = sumBF.toDouble() / wB
            val mF = (sumBF.toDouble() / sum - mB * wB) / wF
            val between = wB * wF * (mB - mF) * (mB - mF)

            if (between >= maximum) {
                threshold = i
                maximum = between
            }
        }

        return threshold
    }

    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                grayBitmap.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }

        return grayBitmap
    }
}








