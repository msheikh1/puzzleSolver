package com.example.puzzlesolver


data class Clue(
    val row: Int,
    val col: Int,
    val sum: Int,
    val isHorizontal: Boolean
)

data class KakuroPuzzle(
    val grid: Array<IntArray>,
    val clues: List<Clue>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KakuroPuzzle

        if (!grid.contentDeepEquals(other.grid)) return false
        if (clues != other.clues) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + clues.hashCode()
        return result
    }
}
