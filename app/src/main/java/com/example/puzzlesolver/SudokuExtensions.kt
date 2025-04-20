// SudokuExtensions.kt
package com.example.puzzlesolver

import com.example.puzzlesolver.SudokuProcessor.SudokuCell

fun Array<Array<SudokuCell>>.printToConsole() {
    println("Sudoku Board:")
    println("+-------+-------+-------+")
    for (i in 0..8) {
        print("| ")
        for (j in 0..8) {
            val num = this[i][j].number
            print(if (num == 0) "." else num)
            print(" ")
            if (j == 2 || j == 5) {
                print("| ")
            }
        }
        println("|")
        if (i == 2 || i == 5) {
            println("+-------+-------+-------+")
        }
    }
    println("+-------+-------+-------+")
}