// SudokuSolver.kt
package com.example.puzzlesolver

class SudokuSolver {
    companion object {
        fun solve(board: Array<Array<SudokuProcessor.SudokuCell>>): Boolean {
            val grid = convertToGrid(board)
            if (solveSudoku(grid)) {
                updateBoardFromGrid(board, grid)
                return true
            }
            return false
        }

        private fun convertToGrid(board: Array<Array<SudokuProcessor.SudokuCell>>): Array<IntArray> {
            return Array(9) { i ->
                IntArray(9) { j ->
                    board[i][j].number
                }
            }
        }

        private fun updateBoardFromGrid(
            board: Array<Array<SudokuProcessor.SudokuCell>>,
            grid: Array<IntArray>
        ) {
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    if (board[i][j].type == SudokuProcessor.SUDOKU_CELL_TYPE_EMPTY) {
                        board[i][j].number = grid[i][j]
                        board[i][j].type = SudokuProcessor.SUDOKU_CELL_TYPE_GUESS
                    }
                }
            }
        }

        private fun solveSudoku(grid: Array<IntArray>): Boolean {
            for (row in 0 until 9) {
                for (col in 0 until 9) {
                    if (grid[row][col] == 0) {
                        for (num in 1..9) {
                            if (isValid(grid, row, col, num)) {
                                grid[row][col] = num
                                if (solveSudoku(grid)) {
                                    return true
                                }
                                grid[row][col] = 0
                            }
                        }
                        return false
                    }
                }
            }
            return true
        }

        private fun isValid(grid: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
            // Check row
            for (i in 0 until 9) {
                if (grid[row][i] == num) return false
            }

            // Check column
            for (i in 0 until 9) {
                if (grid[i][col] == num) return false
            }

            // Check 3x3 box
            val boxRow = row - row % 3
            val boxCol = col - col % 3
            for (i in 0 until 3) {
                for (j in 0 until 3) {
                    if (grid[boxRow + i][boxCol + j] == num) return false
                }
            }

            return true
        }
    }
}