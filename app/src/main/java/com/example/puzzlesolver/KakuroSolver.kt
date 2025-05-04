// KakuroSolver.kt
package com.example.puzzlesolver

import com.example.puzzlesolver.KakuroProcessor.KakuroCell

data class Run(
    val cells: List<Pair<Int, Int>>,  // (row, col) of each cell in the run
    val targetSum: Int,
    var currentSum: Int = 0,
    val usedNumbers: MutableSet<Int> = mutableSetOf(),
    var remainingCells: Int = cells.size
)

class KakuroSolver {
    companion object {
        fun solve(board: Array<Array<KakuroCell>>): Boolean {
            val runs = mutableListOf<Run>()
            preprocessRuns(board, runs)
            return solveKakuro(board, runs, 0, 0)
        }

        private fun preprocessRuns(
            board: Array<Array<KakuroCell>>,
            runs: MutableList<Run>
        ) {
            for (row in board.indices) {
                for (col in board[row].indices) {
                    val cell = board[row][col]
                    if (cell.type == KakuroProcessor.KakuroCellType.CLUE_ONE || cell.type == KakuroProcessor.KakuroCellType.CLUE_TWO) {
                        // Process down clue
                        if (cell.clues.isNotEmpty()) {
                            val downClue = cell.clues[0].toIntOrNull() ?: 0
                            if (downClue > 0) {
                                val downRun = collectDownRun(board, row, col, downClue)
                                downRun?.let {
                                    runs.add(it)
                                    it.cells.forEach { (r, c) ->
                                        board[r][c].downRuns.add(it)
                                    }
                                }
                            }
                        }
                        // Process across clue
                        val acrossIndex =
                            if (cell.type == KakuroProcessor.KakuroCellType.CLUE_TWO) 1 else 0
                        if (cell.clues.size > acrossIndex) {
                            val acrossClue = cell.clues[acrossIndex].toIntOrNull() ?: 0
                            if (acrossClue > 0) {
                                val acrossRun = collectAcrossRun(board, row, col, acrossClue)
                                acrossRun?.let {
                                    runs.add(it)
                                    it.cells.forEach { (r, c) ->
                                        board[r][c].acrossRuns.add(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun collectDownRun(
            board: Array<Array<KakuroCell>>,
            clueRow: Int,
            clueCol: Int,
            targetSum: Int
        ): Run? {
            val cells = mutableListOf<Pair<Int, Int>>()
            var currentRow = clueRow + 1
            while (currentRow < board.size) {
                val currentCell = board[currentRow][clueCol]
                if (currentCell.type != KakuroProcessor.KakuroCellType.EMPTY) break
                cells.add(currentRow to clueCol)
                currentRow++
            }
            return if (cells.isNotEmpty()) Run(cells, targetSum) else null
        }

        private fun collectAcrossRun(
            board: Array<Array<KakuroCell>>,
            clueRow: Int,
            clueCol: Int,
            targetSum: Int
        ): Run? {
            val cells = mutableListOf<Pair<Int, Int>>()
            var currentCol = clueCol + 1
            while (currentCol < board[clueRow].size) {
                val currentCell = board[clueRow][currentCol]
                if (currentCell.type != KakuroProcessor.KakuroCellType.EMPTY) break
                cells.add(clueRow to currentCol)
                currentCol++
            }
            return if (cells.isNotEmpty()) Run(cells, targetSum) else null
        }


        private fun solveKakuro(
            board: Array<Array<KakuroCell>>,
            runs: List<Run>,
            row: Int,
            col: Int
        ): Boolean {
            var (currentRow, currentCol) = findNextEmptyCell(board, row, col)
            if (currentRow == -1) return runs.all { it.currentSum == it.targetSum }

            val cell = board[currentRow][currentCol]
            for (num in 1..9) {
                if (isValid(cell, num)) {
                    cell.number = num
                    val affectedRuns = cell.downRuns + cell.acrossRuns
                    affectedRuns.forEach { run ->
                        run.usedNumbers.add(num)
                        run.currentSum += num
                        run.remainingCells--
                    }

                    if (solveKakuro(board, runs, currentRow, currentCol)) return true

                    // Backtrack
                    cell.number = 0
                    affectedRuns.forEach { run ->
                        run.usedNumbers.remove(num)
                        run.currentSum -= num
                        run.remainingCells++
                    }
                }
            }
            return false
        }

        private fun findNextEmptyCell(
            board: Array<Array<KakuroCell>>,
            startRow: Int,
            startCol: Int
        ): Pair<Int, Int> {
            for (row in startRow until board.size) {
                for (col in (if (row == startRow) startCol else 0) until board[row].size) {
                    if (board[row][col].type == KakuroProcessor.KakuroCellType.EMPTY && board[row][col].number == 0) {
                        return row to col
                    }
                }
            }
            return -1 to -1
        }

        private fun isValid(cell: KakuroCell, num: Int): Boolean {
            for (run in cell.downRuns + cell.acrossRuns) {
                if (num in run.usedNumbers) return false
                if (run.remainingCells == 1) {
                    if (run.currentSum + num != run.targetSum) return false
                } else {
                    if (run.currentSum + num > run.targetSum) return false
                }
            }
            return true
        }
    }
}
fun printKakuroBoard(board: Array<Array<KakuroCell>>) {
    for (row in board.indices) {
        for (col in board[row].indices) {
            val cell = board[row][col]
            when (cell.type) {
                KakuroProcessor.KakuroCellType.EMPTY -> {
                    if (cell.number != 0) {
                        print(" ${cell.number} ")
                    } else {
                        print(" _ ")
                    }
                }
                KakuroProcessor.KakuroCellType.BLOCKED -> { print("███") }
                KakuroProcessor.KakuroCellType.CLUE_ONE -> {
                    if (cell.clues.isNotEmpty()) {
                        print("\\${cell.clues[0].padStart(2)}")
                    } else {
                        print("\\  ")
                    }
                }
                KakuroProcessor.KakuroCellType.CLUE_TWO -> {
                    if (cell.clues.size >= 2) {
                        print("${cell.clues[0].padStart(2)}\\${cell.clues[1].padStart(2)}")
                    } else if (cell.clues.size == 1) {
                        print("  \\${cell.clues[0].padStart(2)}")
                    } else {
                        print("  \\  ")
                    }
                }
            }
            print(" ") // Add space between cells
        }
        println() // New line after each row
    }
}

// Helper function for padding strings
fun String.padStart(length: Int): String {
    return this.padStart(length, ' ')
}