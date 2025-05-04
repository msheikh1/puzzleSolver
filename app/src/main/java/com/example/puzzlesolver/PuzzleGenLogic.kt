package com.example.puzzlesolver

import kotlin.random.Random

object PuzzleGenLogic {
    enum class Difficulty {
        EASY, MEDIUM, HARD, EXPERT
    }


    // Sudoku Generator
    fun generateSudoku(difficulty: PuzzleGenLogic.Difficulty = Difficulty.MEDIUM): Array<IntArray> {
        val grid = Array(9) { IntArray(9) }
        fillDiagonalBoxes(grid)
        fillRemaining(grid, 0, 3)
        removeDigits(grid, difficulty)
        return grid
    }

    private fun fillDiagonalBoxes(grid: Array<IntArray>) {
        for (i in 0 until 9 step 3) {
            fillBox(grid, i, i)
        }
    }

    private fun fillBox(grid: Array<IntArray>, row: Int, col: Int) {
        val nums = (1..9).shuffled()
        var index = 0
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                grid[row + i][col + j] = nums[index++]
            }
        }
    }

    private fun fillRemaining(grid: Array<IntArray>, i: Int, j: Int): Boolean {
        var i = i
        var j = j
        if (j >= 9 && i < 8) {
            i += 1
            j = 0
        }
        if (i >= 9 && j >= 9) return true

        if (i < 3) {
            if (j < 3) j = 3
        } else if (i < 6) {
            if (j == (i / 3) * 3) j += 3
        } else {
            if (j == 6) {
                i += 1
                j = 0
                if (i >= 9) return true
            }
        }

        for (num in 1..9) {
            if (isSafe(grid, i, j, num)) {
                grid[i][j] = num
                if (fillRemaining(grid, i, j + 1)) return true
                grid[i][j] = 0
            }
        }
        return false
    }

    private fun isSafe(grid: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        return !usedInRow(grid, row, num) &&
                !usedInCol(grid, col, num) &&
                !usedInBox(grid, row - row % 3, col - col % 3, num)
    }

    private fun usedInRow(grid: Array<IntArray>, row: Int, num: Int): Boolean {
        for (col in 0 until 9) {
            if (grid[row][col] == num) return true
        }
        return false
    }

    private fun usedInCol(grid: Array<IntArray>, col: Int, num: Int): Boolean {
        for (row in 0 until 9) {
            if (grid[row][col] == num) return true
        }
        return false
    }

    private fun usedInBox(
        grid: Array<IntArray>,
        boxStartRow: Int,
        boxStartCol: Int,
        num: Int
    ): Boolean {
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                if (grid[row + boxStartRow][col + boxStartCol] == num) return true
            }
        }
        return false
    }

    private fun removeDigits(grid: Array<IntArray>, difficulty: Difficulty) {
        val cellsToRemove = when (difficulty) {
            Difficulty.EASY -> 40..45
            Difficulty.MEDIUM -> 46..50
            Difficulty.HARD -> 51..56
            Difficulty.EXPERT -> 57..64
        }

        val cells = Random.nextInt(cellsToRemove.first, cellsToRemove.last + 1)
        var count = cells
        while (count != 0) {
            val cellId = Random.nextInt(81)
            val row = cellId / 9
            val col = cellId % 9

            if (grid[row][col] != 0) {
                grid[row][col] = 0
                count--
            }
        }
    }

    fun generateKakuro(size: Int = 5): KakuroPuzzle { // Default to 5x5
        val grid = Array(size) { IntArray(size) { -1 } } // -1 = empty, 0 = black/block
        val clues = mutableListOf<KakuroClue>()

        // Create blocks (about 25% of cells for smaller grid)
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (Random.nextFloat() < 0.25) {
                    grid[i][j] = 0
                }
            }
        }

        // Ensure top-left and bottom-right are white cells
        grid[0][0] = -1
        grid[size-1][size-1] = -1

        // Generate clues with smaller sums for 5x5
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (grid[i][j] == 0) {
                    // Horizontal clue
                    if (j < size - 1 && grid[i][j + 1] != 0) {
                        var length = 0
                        var k = j + 1
                        while (k < size && grid[i][k] != 0) {
                            length++
                            k++
                        }
                        if (length > 0) {
                            val minSum = (1..length).sum()
                            val maxSum = (9 downTo 9 - length + 1).sum()
                            // Limit max sum for smaller puzzles
                            val sum = Random.nextInt(minSum, minOf(maxSum, 10) + 1)
                            clues.add(KakuroClue(i, j, true, length, sum))
                        }
                    }
                    // Vertical clue
                    if (i < size - 1 && grid[i + 1][j] != 0) {
                        var length = 0
                        var k = i + 1
                        while (k < size && grid[k][j] != 0) {
                            length++
                            k++
                        }
                        if (length > 0) {
                            val minSum = (1..length).sum()
                            val maxSum = (9 downTo 9 - length + 1).sum()
                            val sum = Random.nextInt(minSum, minOf(maxSum, 10) + 1)
                            clues.add(KakuroClue(i, j, false, length, sum))
                        }
                    }
                }
            }
        }

        return KakuroPuzzle(grid, clues)
    }

    private fun makeConnected(grid: Array<IntArray>) {
        // Simple flood-fill approach to ensure connectivity
        val size = grid.size
        val visited = Array(size) { BooleanArray(size) }

        fun dfs(i: Int, j: Int) {
            if (i !in 0 until size || j !in 0 until size || visited[i][j] || grid[i][j] == 0) return
            visited[i][j] = true
            dfs(i + 1, j)
            dfs(i - 1, j)
            dfs(i, j + 1)
            dfs(i, j - 1)
        }

        // Find first white cell
        outer@ for (i in 0 until size) {
            for (j in 0 until size) {
                if (grid[i][j] != 0) {
                    dfs(i, j)
                    break@outer
                }
            }
        }

        // Connect unvisited cells
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (grid[i][j] != 0 && !visited[i][j]) {
                    // Connect to nearest visited cell
                    grid[i][j] = 0
                }
            }
        }
    }

    data class KakuroPuzzle(val grid: Array<IntArray>, val clues: List<KakuroClue>)
    data class KakuroClue(val row: Int, val col: Int, val isHorizontal: Boolean, val length: Int, val sum: Int)

    // Binary Puzzle Generator
    fun generateBinaryPuzzle(size: Int = 8): Array<IntArray> {
        repeat(10) {
            val grid = Array(size) { IntArray(size) { -1 } }

            // Fill with valid pattern
            for (i in 0 until size) {
                for (j in 0 until size) {
                    // Alternate between 0 and 1 in checkerboard pattern
                    grid[i][j] = if ((i + j) % 2 == 0) 0 else 1
                }
            }

            // Randomize while maintaining basic rules
            for (i in 0 until size) {
                for (j in 0 until size) {
                    if (Random.nextBoolean()) {
                        grid[i][j] = 1 - grid[i][j]
                    }
                }
            }

            enforceBinaryRules(grid)

            // Remove about 50% of numbers
            for (i in 0 until size) {
                for (j in 0 until size) {
                    if (Random.nextBoolean()) {
                        grid[i][j] = -1
                    }
                }
            }

            return grid
        }

        // Fallback if we couldn't generate a valid grid
        return Array(size) { IntArray(size) { -1 } }
    }

    private fun enforceBinaryRules(grid: Array<IntArray>) {
        val size = grid.size
        var changed: Boolean
        var iterations = 0
        val maxIterations = 1000

        do {
            changed = false
            iterations++

            // Fix consecutive triples first
            for (i in 0 until size) {
                for (j in 0 until size - 2) {
                    if (grid[i][j] != -1 && grid[i][j] == grid[i][j+1] && grid[i][j] == grid[i][j+2]) {
                        grid[i][j+2] = 1 - grid[i][j]
                        changed = true
                    }
                }
            }

            for (j in 0 until size) {
                for (i in 0 until size - 2) {
                    if (grid[i][j] != -1 && grid[i][j] == grid[i+1][j] && grid[i][j] == grid[i+2][j]) {
                        grid[i+2][j] = 1 - grid[i][j]
                        changed = true
                    }
                }
            }

            // Only balance counts if no consecutive triples exist
            if (!changed) {
                // Balance row counts
                for (i in 0 until size) {
                    val count0 = grid[i].count { it == 0 }
                    val count1 = grid[i].count { it == 1 }
                    val target = size / 2

                    if (count0 > target || count1 > target) {
                        balanceRow(grid, i, count0, count1, target)
                        changed = true
                    }
                }

                // Balance column counts
                for (j in 0 until size) {
                    var count0 = 0
                    var count1 = 0
                    for (i in 0 until size) {
                        when (grid[i][j]) {
                            0 -> count0++
                            1 -> count1++
                        }
                    }
                    val target = size / 2

                    if (count0 > target || count1 > target) {
                        balanceColumn(grid, j, count0, count1, target)
                        changed = true
                    }
                }
            }
        } while (changed && iterations < maxIterations)

        if (iterations >= maxIterations) {
            generateFreshGrid(grid)
            enforceBinaryRules(grid)  // Try again with fresh grid
        }
    }

    private fun generateFreshGrid(grid: Array<IntArray>) {
        val size = grid.size
        // Completely regenerate the grid
        for (i in 0 until size) {
            for (j in 0 until size) {
                grid[i][j] = Random.nextInt(2)
            }
        }
    }

    private fun balanceRow(grid: Array<IntArray>, row: Int, count0: Int, count1: Int, target: Int) {
        val indices = grid[row].indices.shuffled()
        var c0 = count0
        var c1 = count1

        for (j in indices) {
            when {
                c0 > target && grid[row][j] == 0 -> {
                    grid[row][j] = 1
                    c0--
                    c1++
                }
                c1 > target && grid[row][j] == 1 -> {
                    grid[row][j] = 0
                    c1--
                    c0++
                }
            }
            if (c0 == target && c1 == target) break
        }
    }

    private fun balanceColumn(grid: Array<IntArray>, col: Int, count0: Int, count1: Int, target: Int) {
        val indices = grid.indices.shuffled()
        var c0 = count0
        var c1 = count1

        for (i in indices) {
            when {
                c0 > target && grid[i][col] == 0 -> {
                    grid[i][col] = 1
                    c0--
                    c1++
                }
                c1 > target && grid[i][col] == 1 -> {
                    grid[i][col] = 0
                    c1--
                    c0++
                }
            }
            if (c0 == target && c1 == target) break
        }
    }



    fun solveSudoku(grid: Array<IntArray>): Boolean {
        return solveSudokuHelper(grid)
    }

    private fun solveSudokuHelper(grid: Array<IntArray>): Boolean {
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (grid[i][j] == 0) {
                    for (num in 1..9) {
                        if (isSafe(grid, i, j, num)) {
                            grid[i][j] = num
                            if (solveSudokuHelper(grid)) {
                                return true
                            }
                            grid[i][j] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    fun solveBinaryPuzzle(grid: Array<IntArray>): Boolean {
        // Try to fill in the blanks (-1) with valid 0s and 1s
        return solveBinaryHelper(grid)
    }

    private fun solveBinaryHelper(grid: Array<IntArray>): Boolean {
        val size = grid.size
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (grid[i][j] == -1) {
                    for (num in 0..1) {
                        grid[i][j] = num
                        if (isBinaryValid(grid) && solveBinaryHelper(grid)) {
                            return true
                        }
                        grid[i][j] = -1
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun isBinaryValid(grid: Array<IntArray>): Boolean {
        val size = grid.size

        // Check for consecutive triples in rows
        for (row in grid) {
            for (j in 0 until size - 2) {
                if (row[j] != -1 && row[j+1] != -1 && row[j+2] != -1 &&
                    row[j] == row[j+1] && row[j] == row[j+2]) {
                    return false
                }
            }
        }

        // Check for consecutive triples in columns
        for (j in 0 until size) {
            for (i in 0 until size - 2) {
                if (grid[i][j] != -1 && grid[i+1][j] != -1 && grid[i+2][j] != -1 &&
                    grid[i][j] == grid[i+1][j] && grid[i][j] == grid[i+2][j]) {
                    return false
                }
            }
        }

        // Check row balance (only if all cells are filled)
        for (row in grid) {
            if (row.all { it != -1 }) {
                val count0 = row.count { it == 0 }
                val count1 = row.count { it == 1 }
                if (count0 != count1 && count0 != count1 + 1 && count0 + 1 != count1) {
                    return false
                }
            }
        }

        // Check column balance (only if all cells are filled)
        for (j in 0 until size) {
            if ((0 until size).all { grid[it][j] != -1 }) {
                var count0 = 0
                var count1 = 0
                for (i in 0 until size) {
                    when (grid[i][j]) {
                        0 -> count0++
                        1 -> count1++
                    }
                }
                if (count0 != count1 && count0 != count1 + 1 && count0 + 1 != count1) {
                    return false
                }
            }
        }

        return true
    }

    fun solveKakuro(puzzle: KakuroPuzzle): Boolean {
        // Make a copy of the grid to work with
        val grid = puzzle.grid.map { it.clone() }.toTypedArray()
        val clues = puzzle.clues

        // First fill in obvious single-cell clues
        for (clue in clues) {
            if (clue.length == 1) {
                if (clue.isHorizontal) {
                    grid[clue.row][clue.col + 1] = clue.sum
                } else {
                    grid[clue.row + 1][clue.col] = clue.sum
                }
            }
        }

        // Limit the solver to 5 seconds to prevent freezing
        val startTime = System.currentTimeMillis()
        val solved = solveKakuroHelper(grid, clues, 0, 0, startTime)

        if (solved) {
            // Update the original puzzle with the solution
            puzzle.grid.forEachIndexed { i, row ->
                row.forEachIndexed { j, _ ->
                    row[j] = grid[i][j]
                }
            }
        }
        return solved
    }

    private fun solveKakuroHelper(
        grid: Array<IntArray>,
        clues: List<KakuroClue>,
        row: Int,
        col: Int,
        startTime: Long,
        depth: Int = 0
    ): Boolean {
        // Timeout after 5 seconds to prevent freezing
        if (System.currentTimeMillis() - startTime > 5000) {
            return false
        }

        val size = grid.size
        var currentRow = row
        var currentCol = col

        // Find next empty cell
        while (currentRow < size && grid[currentRow][currentCol] != -1) {
            currentCol++
            if (currentCol >= size) {
                currentCol = 0
                currentRow++
            }
        }

        if (currentRow >= size) return true // Puzzle solved

        // Try numbers 1-9 for this cell, but limit recursion depth for 5x5 puzzles
        if (depth > 1000) return false

        // Get possible numbers for this cell based on constraints
        val possibleNumbers = getPossibleNumbers(grid, clues, currentRow, currentCol)

        for (num in possibleNumbers) {
            grid[currentRow][currentCol] = num

            if (solveKakuroHelper(grid, clues, currentRow, currentCol, startTime, depth + 1)) {
                return true
            }

            grid[currentRow][currentCol] = -1
        }

        return false
    }

    private fun getPossibleNumbers(
        grid: Array<IntArray>,
        clues: List<KakuroClue>,
        row: Int,
        col: Int
    ): List<Int> {
        val possibleNumbers = mutableListOf<Int>()

        // Check all clues that affect this cell
        for (clue in clues) {
            if (clue.isHorizontal && row == clue.row && col >= clue.col + 1 && col < clue.col + 1 + clue.length) {
                // This cell is part of a horizontal clue
                var currentSum = 0
                var emptyCells = 0
                val usedNumbers = mutableSetOf<Int>()

                for (i in 1..clue.length) {
                    val cellVal = grid[row][clue.col + i]
                    if (cellVal == -1 && (clue.col + i) != col) {
                        emptyCells++
                    } else if ((clue.col + i) == col) {
                        // This is the current cell we're trying to fill
                    } else {
                        currentSum += cellVal
                        usedNumbers.add(cellVal)
                    }
                }

                // Calculate possible numbers
                val remainingSum = clue.sum - currentSum
                val remainingCells = emptyCells + 1 // +1 for current cell

                // If this is the last empty cell in the run
                if (emptyCells == 0) {
                    if (remainingSum in 1..9 && !usedNumbers.contains(remainingSum)) {
                        return listOf(remainingSum)
                    } else {
                        return emptyList()
                    }
                }

                // Otherwise, limit possible numbers
                val minPossible = maxOf(1, remainingSum - 9 * (remainingCells - 1))
                val maxPossible = minOf(9, remainingSum - (remainingCells - 1))

                for (num in minPossible..maxPossible) {
                    if (num !in usedNumbers) {
                        possibleNumbers.add(num)
                    }
                }

                return possibleNumbers.distinct()
            }

            if (!clue.isHorizontal && col == clue.col && row >= clue.row + 1 && row < clue.row + 1 + clue.length) {
                // This cell is part of a vertical clue
                var currentSum = 0
                var emptyCells = 0
                val usedNumbers = mutableSetOf<Int>()

                for (i in 1..clue.length) {
                    val cellVal = grid[clue.row + i][col]
                    if (cellVal == -1 && (clue.row + i) != row) {
                        emptyCells++
                    } else if ((clue.row + i) == row) {
                        // This is the current cell we're trying to fill
                    } else {
                        currentSum += cellVal
                        usedNumbers.add(cellVal)
                    }
                }

                // Calculate possible numbers
                val remainingSum = clue.sum - currentSum
                val remainingCells = emptyCells + 1 // +1 for current cell

                // If this is the last empty cell in the run
                if (emptyCells == 0) {
                    if (remainingSum in 1..9 && !usedNumbers.contains(remainingSum)) {
                        return listOf(remainingSum)
                    } else {
                        return emptyList()
                    }
                }

                // Otherwise, limit possible numbers
                val minPossible = maxOf(1, remainingSum - 9 * (remainingCells - 1))
                val maxPossible = minOf(9, remainingSum - (remainingCells - 1))

                for (num in minPossible..maxPossible) {
                    if (num !in usedNumbers) {
                        possibleNumbers.add(num)
                    }
                }

                return possibleNumbers.distinct()
            }
        }

        // If no constraints found (shouldn't happen in valid Kakuro), try all numbers
        return (1..9).toList()
    }

    }



