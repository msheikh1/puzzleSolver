
package com.example.puzzlesolver

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader

class PuzzleImageProcessorActivity : AppCompatActivity() {
    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
        private const val REQUEST_IMAGE_CAPTURE = 1002
    }

    private lateinit var processedImageView: ImageView
    private lateinit var solveButton: Button
    private lateinit var puzzleView: PuzzleView
    private lateinit var captureButton: Button
    private lateinit var loadingIndicator: ProgressBar
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var sudokuBoard: Array<Array<SudokuProcessor.SudokuCell>>? = null
    private var kakuroBoard: Array<Array<KakuroProcessor.KakuroCell>>? = null
    private var binaryBoard: Array<Array<BinaryProcessor.BinaryCell>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_puzzle_processing)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.menu.findItem(R.id.nav_home).isChecked = true

        val selectImageButton = findViewById<Button>(R.id.selectImageButton)
        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

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
                    dispatchTakePictureIntent()
                    true
                }
                R.id.nav_nonogram -> {
                    startActivity(Intent(this, NonogramCreator::class.java))
                    true
                }
                else -> false
            }
        }

        processedImageView = findViewById(R.id.processedImageView)
        solveButton = findViewById(R.id.solveButton)
        puzzleView = findViewById(R.id.puzzleView)
        captureButton = findViewById(R.id.captureButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed.")
            Toast.makeText(this, "OpenCV initialization failed", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("OpenCV", "OpenCV initialized successfully.")

        captureButton.setOnClickListener {
            dispatchTakePictureIntent()
        }

        solveButton.setOnClickListener {
            when {
                sudokuBoard != null -> {
                    coroutineScope.launch {
                        showLoading(true)
                        val processor = SudokuProcessor(this@PuzzleImageProcessorActivity)
                        val isSolved = processor.solveSudoku(sudokuBoard!!)
                        if (isSolved) {
                            val sudokuGrid = sudokuBoard!!.map { row ->
                                row.map { it.number }.toIntArray()
                            }.toTypedArray()
                            puzzleView.puzzle = sudokuGrid
                            puzzleView.visibility = View.VISIBLE
                            processedImageView.visibility = View.GONE
                            puzzleView.invalidate()
                            Toast.makeText(
                                this@PuzzleImageProcessorActivity,
                                "Sudoku solved!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@PuzzleImageProcessorActivity,
                                "Failed to solve Sudoku",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showLoading(false)
                    }
                }
                binaryBoard != null -> {
                    coroutineScope.launch {
                        showLoading(true)
                        val isSolved = BinarySolver.solve(binaryBoard!!)
                        if (isSolved) {
                            val binaryGrid = binaryBoard!!.map { row ->
                                row.map { cell ->
                                    when (cell.state) {
                                        BinaryProcessor.BinaryCellState.ZERO -> 0
                                        BinaryProcessor.BinaryCellState.ONE -> 1
                                        else -> -1
                                    }
                                }.toIntArray()
                            }.toTypedArray()
                            puzzleView.puzzle = binaryGrid
                            puzzleView.visibility = View.VISIBLE
                            processedImageView.visibility = View.GONE
                            puzzleView.invalidate()
                            Toast.makeText(
                                this@PuzzleImageProcessorActivity,
                                "Binary puzzle solved!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@PuzzleImageProcessorActivity,
                                "Failed to solve Binary puzzle",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showLoading(false)
                    }
                }
                kakuroBoard != null -> {
                    coroutineScope.launch {
                        showLoading(true)
                        val isSolved = KakuroSolver.solve(kakuroBoard!!)
                        if (isSolved) {
                            val grid = Array(kakuroBoard!!.size) { i ->
                                IntArray(kakuroBoard!![i].size) { j ->
                                    kakuroBoard!![i][j].number
                                }
                            }
                            val clues = extractKakuroClues(kakuroBoard!!)
                            puzzleView.puzzle = KakuroPuzzle(grid, clues)
                            puzzleView.visibility = View.VISIBLE
                            processedImageView.visibility = View.GONE
                            puzzleView.invalidate()
                            Toast.makeText(
                                this@PuzzleImageProcessorActivity,
                                "Kakuro solved!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@PuzzleImageProcessorActivity,
                                "Failed to solve Kakuro",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showLoading(false)
                    }
                }
                else -> {
                    Toast.makeText(this, "Please process a puzzle first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun processImage(originalBitmap: Bitmap) {
        coroutineScope.launch {
            showLoading(true)
            try {
                val processors = listOf(
                    Triple(
                        KakuroProcessor(this@PuzzleImageProcessorActivity),
                        "kakuro"
                    ) { processor: PuzzleProcessor, callback: (Pair<Any, Bitmap>) -> Unit ->
                        launch(Dispatchers.Default) {
                            try {
                                val processedBitmap = (processor as KakuroProcessor).process(originalBitmap)
                                val board = processor.extractKakuroCells(processedBitmap)
                                callback(Pair(board, processedBitmap))
                            } catch (e: Exception) {
                                Log.e("KakuroProcessor", "Processing failed", e)
                                throw e
                            }
                        }
                    }, Triple(
                        SudokuProcessor(this@PuzzleImageProcessorActivity),
                        "sudoku"
                    ) { processor: PuzzleProcessor, callback: (Pair<Any, Bitmap>) -> Unit ->
                        launch(Dispatchers.Default) {
                            try {
                                val processedBitmap = (processor as SudokuProcessor).process(originalBitmap)
                                val board = processor.extractSudokuNumbers(processedBitmap)
                                callback(Pair(board, processedBitmap))
                            } catch (e: Exception) {
                                Log.e("SudokuProcessor", "Processing failed", e)
                                throw e
                            }
                        }
                    },                 Triple(
                        BinaryProcessor(this@PuzzleImageProcessorActivity),
                        "binary"
                    ) { processor: PuzzleProcessor, callback: (Pair<Any, Bitmap>) -> Unit ->
                        launch(Dispatchers.Default) {
                            try {
                                val processedBitmap = (processor as BinaryProcessor).process(originalBitmap)
                                val board = processor.extractBinaryPuzzle(processedBitmap)
                                callback(Pair(board, processedBitmap))
                            } catch (e: Exception) {
                                Log.e("BinaryProcessor", "Processing failed", e)
                                throw e
                            }
                        }
                    },
                )

                var detectedType: String? = null
                var processedBitmap: Bitmap? = null

                processorLoop@ for ((processor, puzzleType, extractor) in processors) {
                    try {
                        val (board, bitmap) = suspendCancellableCoroutine { continuation ->
                            extractor(processor) { result ->
                                continuation.resume(result) {
                                    Log.d("PuzzleClassification", "Processing cancelled")
                                }
                            }
                        }

                        when (puzzleType) {
                            "sudoku" -> {
                                if (isValidSudokuBoard(board as Array<Array<SudokuProcessor.SudokuCell>>)) {
                                    sudokuBoard = board
                                    detectedType = puzzleType
                                    processedBitmap = bitmap
                                    break@processorLoop
                                }
                            }
                            "binary" -> {
                                if (isValidBinaryBoard(board as Array<Array<BinaryProcessor.BinaryCell>>)) {
                                    binaryBoard = board
                                    detectedType = puzzleType
                                    processedBitmap = bitmap
                                    break@processorLoop
                                }
                            }
                            "kakuro" -> {
                                if (isValidKakuroBoard(board as Array<Array<KakuroProcessor.KakuroCell>>)) {
                                    kakuroBoard = board
                                    detectedType = puzzleType
                                    processedBitmap = bitmap
                                    break@processorLoop
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PuzzleClassification", "Error with $puzzleType: ${e.message}")
                        continue@processorLoop
                    }
                }

                withContext(Dispatchers.Main) {
                    if (detectedType != null) {
                        processedImageView.setImageBitmap(processedBitmap)
                        solveButton.isEnabled = true
                        Toast.makeText(
                            this@PuzzleImageProcessorActivity,
                            "Detected $detectedType puzzle",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@PuzzleImageProcessorActivity,
                            "Puzzle type not recognized",
                            Toast.LENGTH_SHORT
                        ).show()
                        processedImageView.setImageBitmap(originalBitmap)
                        solveButton.isEnabled = false
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageProcessing", "Error processing image", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PuzzleImageProcessorActivity,
                        "Error processing image",
                        Toast.LENGTH_SHORT
                    ).show()
                    processedImageView.setImageBitmap(originalBitmap)
                    solveButton.isEnabled = false
                }
            } finally {
                showLoading(false)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    try {
                        val inputStream = contentResolver.openInputStream(data?.data!!)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        processedImageView.setImageBitmap(bitmap)
                        puzzleView.visibility = View.GONE
                        processedImageView.visibility = View.VISIBLE
                        processImage(bitmap)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                        Log.e("ImageSelection", "Error loading image", e)
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    processedImageView.setImageBitmap(imageBitmap)
                    puzzleView.visibility = View.GONE
                    processedImageView.visibility = View.VISIBLE
                    processImage(imageBitmap)
                }
            }
        }
    }

    // Validation functions
    private fun isValidSudokuBoard(board: Array<Array<SudokuProcessor.SudokuCell>>): Boolean {
        if (board.size != 9 || board.any { it.size != 9 }) return false
        val numbers = board.flatMap { row -> row.map { it.number } }
        if (numbers.all { it == 0 }) return false

        // Should contain numbers greater than 1 (to distinguish from Binary puzzles)
        if (numbers.none { it > 1 }) return false

        // Should have a reasonable distribution of numbers (not all the same)
        val uniqueNumbers = numbers.filter { it != 0 }.toSet()
        if (uniqueNumbers.size < 3) return false

        return true
    }

    private fun isValidBinaryBoard(board: Array<Array<BinaryProcessor.BinaryCell>>): Boolean {
        val validSizes = setOf(10)
        return board.size in validSizes &&
                board.all { it.size == board.size } &&
                board.any { row -> row.any { it.state != BinaryProcessor.BinaryCellState.EMPTY } }
    }

    private fun isValidKakuroBoard(board: Array<Array<KakuroProcessor.KakuroCell>>): Boolean {
        // Must have at least some clue cells
        val hasClueCells = board.any { row ->
            row.any { cell ->
                cell.type == KakuroProcessor.KakuroCellType.CLUE_ONE ||
                        cell.type == KakuroProcessor.KakuroCellType.CLUE_TWO
            }
        }
        if (!hasClueCells) return false

        // Must have slashes in clue cells
        val hasSlashCells = board.any { row ->
            row.any { cell ->
                cell.type == KakuroProcessor.KakuroCellType.CLUE_ONE ||
                        cell.type == KakuroProcessor.KakuroCellType.CLUE_TWO
            }
        }
        if (!hasSlashCells) return false

        return true
    }


    private fun extractKakuroClues(board: Array<Array<KakuroProcessor.KakuroCell>>): List<Clue> {
        val clues = mutableListOf<Clue>()

        for (i in board.indices) {
            for (j in board[i].indices) {
                val cell = board[i][j]

                when (cell.type) {
                    KakuroProcessor.KakuroCellType.CLUE_ONE,
                    KakuroProcessor.KakuroCellType.CLUE_TWO -> {
                        // Check for horizontal (across) clues
                        if (j == 0 || board[i][j - 1].type == KakuroProcessor.KakuroCellType.BLOCKED) {
                            val clueIndex =
                                if (cell.type == KakuroProcessor.KakuroCellType.CLUE_TWO) 1 else 0
                            if (cell.clues.size > clueIndex) {
                                cell.clues[clueIndex].toIntOrNull()?.let { sum ->
                                    clues.add(Clue(i, j, sum, true))
                                }
                            }
                        }

                        // Check for vertical (down) clues
                        if (i == 0 || board[i - 1][j].type == KakuroProcessor.KakuroCellType.BLOCKED) {
                            cell.clues.firstOrNull()?.toIntOrNull()?.let { sum ->
                                clues.add(Clue(i, j, sum, false))
                            }
                        }
                    }

                    else -> {} // Skip other cell types
                }
            }
        }

        return clues
    }
    fun showLoading(isLoading: Boolean) {
        loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        solveButton.isEnabled = !isLoading
        captureButton.isEnabled = !isLoading

    }
}



//val imageUri = Uri.parse(imageUriString)
//val bitmap = uriToBitmap(imageUri) ?: run {
//    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
//    finish()
//    return
//}
//PuzzleClassifier.initialize(this)
//
//// Get image URI from intent
//val imageUriString = intent.getStringExtra("imageUri") ?: run {
//    Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
//    finish()
//    return
//}
//val (prediction, confidence) = PuzzleClassifier.classify(bitmap)
//val confidencePercent = (confidence * 100).toInt()
//
//runOnUiThread {
//    Toast.makeText(
//        this,
//        "Prediction: $prediction\nConfidence: $confidencePercent%",
//        Toast.LENGTH_LONG
//    ).show()
//
//    val processedBitmap = when {
//        prediction.contains("sudoku", ignoreCase = true) -> SudokuProcessor(this).process(bitmap)
//        prediction.contains("kakuro", ignoreCase = true) -> KakuroProcessor(this).process(bitmap)
//        prediction.contains("nonogram", ignoreCase = true) -> NonogramProcessor(this).process(bitmap)
//        prediction == "unknown" -> {
//            Toast.makeText(
//                this,
//                "Unrecognized puzzle type. Try another image.",
//                Toast.LENGTH_SHORT
//            ).show()
//            bitmap
//        }
//        else -> {
//            Toast.makeText(this, "Unexpected label: $prediction", Toast.LENGTH_SHORT)
//                .show()
//            bitmap
//        }
//    }