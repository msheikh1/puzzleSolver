package com.example.puzzlesolver

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        // Set up bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.menu.findItem(R.id.nav_home).isChecked = true // Set default selected item

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Handle home navigation
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
                R.id.nav_nonogram-> {
                    startActivity(Intent(this, NonogramCreator::class.java))
                    true
                }
                else -> false
            }
        }
    }

    fun SudokuInfo(view: View) {
        val dialog = PuzzleInfoDialog(
            title = "Sudoku",
            description = "The classic number placement puzzle loved around the world.",
            rules = listOf(
                "Fill the grid with numbers 1-9",
                "No repeats in any row, column, or 3×3 box"
            ),
            tips = listOf(
                "Start with rows, columns, or boxes that are nearly complete",
                "Look for numbers that can only go in one place",
                "Use pencil marks to keep track of possible values"
            ),
            difficulty = "★★★☆☆",
            videoUrl = "https://www.youtube.com/watch?v=8zRXDsGydeQ&ab_channel=TripleSGames",
            activityClass = null
        )
        dialog.show(supportFragmentManager, "SudokuInfo")
    }

    fun KakuroInfo(view: View) {
        val dialog = PuzzleInfoDialog(
            title = "Kakuro",
            description = "A numerical crossword combining Sudoku and math",
            rules = listOf(
                "Fill white cells with digits 1-9",
                "Clues show the sum of consecutive cells",
                "No digit repeats within a sum",
                "Use only the digits specified (no zero)"
            ),
            tips = listOf(
                "Start with small sums (3 in 2 cells = 1+2)",
                "Look for unique sum combinations",
                "Cross-reference horizontal and vertical sums",
                "Eliminate impossible digits systematically"
            ),
            difficulty = "★★★★☆",
            videoUrl = "https://www.youtube.com/watch?v=BYX93SLkNrQ&ab_channel=StevenScott",
            activityClass = null
        )
        dialog.show(supportFragmentManager, "KakuroInfo")
    }

    fun BinaryPuzzleInfo(view: View) {
        val dialog = PuzzleInfoDialog(
            title = "Binary Puzzle",
            description = "Logical puzzle with simple rules but challenging solutions",
            rules = listOf(
                "Fill grid with 0s and 1s",
                "No more than two identical numbers adjacent",
                "Each row/column must be balanced (equal 0s and 1s)",
                "All rows/columns must be unique"
            ),
            tips = listOf(
                "Mark impossible triplets first",
                "Complete nearly-full rows/columns",
                "Watch for forced moves where only one digit fits",
                "Check for symmetry patterns"
            ),
            difficulty = "★★☆☆☆",
            videoUrl = "https://www.youtube.com/watch?v=yTktPr5lPNM&ab_channel=ForgottenGames",
            activityClass = null
        )
        dialog.show(supportFragmentManager, "BinaryPuzzleInfo")
    }

    fun NonogramInfo(view: View) {
        val dialog = PuzzleInfoDialog(
            title = "Nonogram",
            description = "Picture logic puzzle revealing hidden images",
            rules = listOf(
                "Numbers indicate blocks of consecutive filled squares",
                "Separate blocks by at least one empty square",
                "Follow both row and column clues simultaneously",
                "Complete the puzzle to reveal the hidden picture"
            ),
            tips = listOf(
                "Start with the largest number clues",
                "Mark definite filled squares first",
                "Use edge logic - work from the borders inward",
                "Look for overlapping possibilities"
            ),
            difficulty = "★★★☆☆",
            videoUrl = "https://www.youtube.com/watch?v=EaxT0RGWrjw&t=11s&ab_channel=LokNezMunstr",
            activityClass = null
        )
        dialog.show(supportFragmentManager, "NonogramInfo")
    }
}