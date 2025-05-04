package com.example.puzzlesolver

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class PuzzleGeneratorActivity : AppCompatActivity() {
    private lateinit var puzzleTypeSpinner: Spinner
    private lateinit var generateButton: Button
    private lateinit var boardContainer: FrameLayout
    private lateinit var solveButton: Button
    private var currentBoardView: PuzzleGeneratorDialog.BoardView? = null
    private val puzzleGenerator = PuzzleGenerator.instance
    private var currentPuzzle: Any? = null
    private var currentPuzzleType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_puzzle_generator)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.menu.findItem(R.id.nav_gen_puzzle).isChecked = true // Set default selected item

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }

                R.id.nav_gen_puzzle -> {

                    true
                }

                R.id.nav_camera -> {
                    startActivity(Intent(this, PuzzleImageProcessorActivity::class.java))
                    true
                }
                R.id.nav_nonogram-> {
                    startActivity(Intent(this, NonogramCreator::class.java))
                    true
                }
                else -> false
            }
        }


        puzzleTypeSpinner = findViewById(R.id.puzzle_type_spinner)
        generateButton = findViewById(R.id.generate_button)
        boardContainer = findViewById(R.id.board_container)
        solveButton = findViewById(R.id.solve_button)

        val puzzleTypes = listOf("Sudoku", "Kakuro", "Binary Puzzle")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, puzzleTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        puzzleTypeSpinner.adapter = adapter

        generateButton.setOnClickListener {
            val selected = puzzleTypeSpinner.selectedItem.toString()
            currentPuzzleType = selected
            val boardView = when (selected) {
                "Sudoku" -> {
                    val puzzle = puzzleGenerator.generateSudoku()
                    currentPuzzle = puzzle
                    PuzzleGeneratorDialog.BoardView(this).apply { setPuzzle(puzzle) }
                }
                "Kakuro" -> {
                    val puzzle = puzzleGenerator.generateKakuro()
                    currentPuzzle = puzzle
                    PuzzleGeneratorDialog.BoardView(this).apply { setPuzzle(puzzle) }
                }
                "Binary Puzzle" -> {
                    val puzzle = puzzleGenerator.generateBinaryPuzzle()
                    currentPuzzle = puzzle
                    PuzzleGeneratorDialog.BoardView(this).apply { setPuzzle(puzzle) }
                }
                else -> null
            }

            boardView?.let {
                currentBoardView = it
                boardContainer.removeAllViews()
                boardContainer.addView(it)
            }
        }

        solveButton.setOnClickListener {
            currentBoardView?.let { boardView ->
                when (currentPuzzleType) {
                    "Sudoku" -> {
                        val grid = currentPuzzle as Array<IntArray>
                        if (PuzzleGenLogic.solveSudoku(grid)) {
                            boardView.setPuzzle(grid)
                            boardView.invalidate()
                        } else {
                            Toast.makeText(this, "Could not solve this puzzle", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Kakuro" -> {
                        val puzzle = currentPuzzle as PuzzleGenLogic.KakuroPuzzle
                        if (PuzzleGenLogic.solveKakuro(puzzle)) {
                            boardView.setPuzzle(puzzle)
                            boardView.invalidate()
                        } else {
                            Toast.makeText(this, "Could not solve this puzzle", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    "Binary Puzzle" -> {
                        val grid = currentPuzzle as Array<IntArray>
                        if (PuzzleGenLogic.solveBinaryPuzzle(grid)) {
                            boardView.setPuzzle(grid)
                            boardView.invalidate()
                        } else {
                            Toast.makeText(this, "Could not solve this puzzle", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> Toast.makeText(this, "Solving not implemented for this puzzle type", Toast.LENGTH_SHORT).show()
                }
            } ?: Toast.makeText(this, "Please generate a puzzle first.", Toast.LENGTH_SHORT).show()
        }
    }
}
