package com.example.puzzlesolver

class BinarySolver {
    companion object {
        fun solve(board: Array<Array<BinaryProcessor.BinaryCell>>): Boolean {
            val grid = convertToGrid(board)
            if (solveBinaryPuzzle(grid)) {
                updateBoardFromGrid(board, grid)
                return true
            }
            return false
        }

        private fun convertToGrid(board: Array<Array<BinaryProcessor.BinaryCell>>): Array<IntArray> {
            return Array(10) { i ->
                IntArray(10) { j ->
                    board[i][j].value
                }
            }
        }

        private fun updateBoardFromGrid(
            board: Array<Array<BinaryProcessor.BinaryCell>>,
            grid: Array<IntArray>
        ) {
            for (i in 0 until 10) {
                for (j in 0 until 10) {
                    if (!board[i][j].isFixed) {
                        board[i][j].value = grid[i][j]
                    }
                }
            }
        }

        private fun solveBinaryPuzzle(grid: Array<IntArray>): Boolean {
            // First apply logical rules to fill in obvious cells
            if (applyLogicalRules(grid)) {
                return true
            }

            // If logical rules don't solve it completely, use backtracking
            return backtrackSolve(grid)
        }

        private fun applyLogicalRules(grid: Array<IntArray>): Boolean {
            var changed: Boolean
            do {
                changed = false

                // Rule 1: No three consecutive 0's or 1's in rows or columns
                if (applyNoThreeConsecutiveRule(grid)) changed = true

                // Rule 2: Each row and column must have equal number of 0's and 1's
                if (applyEqualCountRule(grid)) changed = true

                // Rule 3: No identical rows or columns
                if (applyUniqueRowsColumnsRule(grid)) changed = true

            } while (changed && !isSolved(grid))

            return isSolved(grid)
        }

        private fun applyNoThreeConsecutiveRule(grid: Array<IntArray>): Boolean {
            var changed = false

            // Check rows
            for (i in 0 until 10) {
                for (j in 0 until 8) {
                    if (grid[i][j] != -1 && grid[i][j] == grid[i][j+1]) {
                        val value = grid[i][j]
                        if (j > 0 && grid[i][j-1] == -1) {
                            grid[i][j-1] = 1 - value
                            changed = true
                        }
                        if (j < 7 && grid[i][j+2] == -1) {
                            grid[i][j+2] = 1 - value
                            changed = true
                        }
                    }
                }
            }

            // Check columns
            for (j in 0 until 10) {
                for (i in 0 until 8) {
                    if (grid[i][j] != -1 && grid[i][j] == grid[i+1][j]) {
                        val value = grid[i][j]
                        if (i > 0 && grid[i-1][j] == -1) {
                            grid[i-1][j] = 1 - value
                            changed = true
                        }
                        if (i < 7 && grid[i+2][j] == -1) {
                            grid[i+2][j] = 1 - value
                            changed = true
                        }
                    }
                }
            }

            return changed
        }

        private fun applyEqualCountRule(grid: Array<IntArray>): Boolean {
            var changed = false

            // Check rows
            for (i in 0 until 10) {
                val zeros = grid[i].count { it == 0 }
                val ones = grid[i].count { it == 1 }

                if (zeros == 5) {
                    for (j in 0 until 10) {
                        if (grid[i][j] == -1) {
                            grid[i][j] = 1
                            changed = true
                        }
                    }
                } else if (ones == 5) {
                    for (j in 0 until 10) {
                        if (grid[i][j] == -1) {
                            grid[i][j] = 0
                            changed = true
                        }
                    }
                }
            }

            // Check columns
            for (j in 0 until 10) {
                var zeros = 0
                var ones = 0
                for (i in 0 until 10) {
                    when (grid[i][j]) {
                        0 -> zeros++
                        1 -> ones++
                    }
                }

                if (zeros == 5) {
                    for (i in 0 until 10) {
                        if (grid[i][j] == -1) {
                            grid[i][j] = 1
                            changed = true
                        }
                    }
                } else if (ones == 5) {
                    for (i in 0 until 10) {
                        if (grid[i][j] == -1) {
                            grid[i][j] = 0
                            changed = true
                        }
                    }
                }
            }

            return changed
        }

        private fun applyUniqueRowsColumnsRule(grid: Array<IntArray>): Boolean {
            var changed = false

            // Check for unique rows
            val completedRows = mutableListOf<IntArray>()
            val incompleteRows = mutableListOf<Pair<Int, IntArray>>()

            for (i in 0 until 10) {
                if (grid[i].none { it == -1 }) {
                    completedRows.add(grid[i].copyOf())
                } else {
                    incompleteRows.add(Pair(i, grid[i].copyOf()))
                }
            }

            for ((i, row) in incompleteRows) {
                // Find possible values for empty cells
                val emptyIndices = row.indices.filter { row[it] == -1 }
                val possibleValues = Array(emptyIndices.size) { mutableSetOf(0, 1) }

                // For each empty cell, try both values and check if it would make the row identical to any completed row
                for ((idx, j) in emptyIndices.withIndex()) {
                    for (value in 0..1) {
                        val tempRow = row.copyOf()
                        tempRow[j] = value

                        if (completedRows.any { completedRow ->
                                tempRow.indices.all { k ->
                                    tempRow[k] != -1 && tempRow[k] == completedRow[k]
                                }
                            }) {
                            possibleValues[idx].remove(value)
                        }
                    }

                    if (possibleValues[idx].size == 1) {
                        grid[i][emptyIndices[idx]] = possibleValues[idx].first()
                        changed = true
                    }
                }
            }

            // Similar check for columns (omitted for brevity but similar logic)

            return changed
        }

        private fun backtrackSolve(grid: Array<IntArray>): Boolean {
            for (i in 0 until 10) {
                for (j in 0 until 10) {
                    if (grid[i][j] == -1) {
                        for (num in 0..1) {
                            if (isValid(grid, i, j, num)) {
                                grid[i][j] = num
                                if (solveBinaryPuzzle(grid)) {
                                    return true
                                }
                                grid[i][j] = -1
                            }
                        }
                        return false
                    }
                }
            }
            return true
        }

        private fun isValid(grid: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
            // Check for three consecutive in row
            if (col >= 2 && grid[row][col-1] == num && grid[row][col-2] == num) return false
            if (col <= 7 && grid[row][col+1] == num && grid[row][col+2] == num) return false
            if (col >= 1 && col <= 8 && grid[row][col-1] == num && grid[row][col+1] == num) return false

            // Check for three consecutive in column
            if (row >= 2 && grid[row-1][col] == num && grid[row-2][col] == num) return false
            if (row <= 7 && grid[row+1][col] == num && grid[row+2][col] == num) return false
            if (row >= 1 && row <= 8 && grid[row-1][col] == num && grid[row+1][col] == num) return false

            // Check row count
            val rowZeros = grid[row].count { it == 0 } + if (num == 0) 1 else 0
            val rowOnes = grid[row].count { it == 1 } + if (num == 1) 1 else 0
            if (rowZeros > 5 || rowOnes > 5) return false

            // Check column count
            var colZeros = 0
            var colOnes = 0
            for (i in 0 until 10) {
                when (grid[i][col]) {
                    0 -> colZeros++
                    1 -> colOnes++
                }
            }
            colZeros += if (num == 0) 1 else 0
            colOnes += if (num == 1) 1 else 0
            if (colZeros > 5 || colOnes > 5) return false

            return true
        }

        private fun isSolved(grid: Array<IntArray>): Boolean {
            // Check all cells are filled
            if (grid.any { row -> row.any { it == -1 } }) return false

            // Check no three consecutive in rows or columns
            for (i in 0 until 10) {
                for (j in 0 until 8) {
                    if (grid[i][j] == grid[i][j+1] && grid[i][j+1] == grid[i][j+2]) return false
                }
            }

            for (j in 0 until 10) {
                for (i in 0 until 8) {
                    if (grid[i][j] == grid[i+1][j] && grid[i+1][j] == grid[i+2][j]) return false
                }
            }

            // Check equal count of 0's and 1's in each row and column
            for (i in 0 until 10) {
                if (grid[i].count { it == 0 } != 5) return false
            }

            for (j in 0 until 10) {
                var zeros = 0
                for (i in 0 until 10) {
                    if (grid[i][j] == 0) zeros++
                }
                if (zeros != 5) return false
            }

            // Check all rows and columns are unique
            val rows = grid.map { it.joinToString("") }
            if (rows.distinct().size != 10) return false

            val columns = (0 until 10).map { j ->
                (0 until 10).map { i -> grid[i][j] }.joinToString("")
            }
            if (columns.distinct().size != 10) return false

            return true
        }
    }
}