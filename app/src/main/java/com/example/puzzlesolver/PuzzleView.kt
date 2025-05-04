package com.example.puzzlesolver

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class PuzzleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var puzzle: Any? = null
        set(value) {
            field = value
            invalidate()
        }

    private var cellSize = 0f

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 48f
        textAlign = Paint.Align.CENTER
    }

    private val smallTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val gridPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 3f
    }

    private val thickGridPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 6f
    }

    private val blockPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, width) // Make the view square
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (val p = puzzle) {
            is Array<*> -> {
                if (p.isNotEmpty() && p.all { row -> row is IntArray }) {
                    val grid = p as Array<IntArray>
                    if (grid.size == 9 && grid[0].size == 9) {
                        drawSudoku(canvas, grid)
                    } else {
                        drawBinaryPuzzle(canvas, grid)
                    }
                }
            }
            is KakuroPuzzle -> drawKakuro(canvas, p)
            else -> {}
        }
    }

    private fun drawSudoku(canvas: Canvas, grid: Array<IntArray>) {
        if (grid.size != 9 || grid[0].size != 9) return
        cellSize = width / 9f
        textPaint.textSize = cellSize * 0.4f
        textPaint.color = Color.BLACK

        // Draw grid lines
        for (i in 0..9) {
            val paint = if (i % 3 == 0) thickGridPaint else gridPaint
            canvas.drawLine(i * cellSize, 0f, i * cellSize, height.toFloat(), paint)
            canvas.drawLine(0f, i * cellSize, width.toFloat(), i * cellSize, paint)
        }

        // Draw numbers
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (grid[i][j] != 0) {
                    val x = j * cellSize + cellSize / 2
                    val y = i * cellSize + cellSize / 2 - (textPaint.ascent() + textPaint.descent()) / 2
                    canvas.drawText(grid[i][j].toString(), x, y, textPaint)
                }
            }
        }
    }

    private fun drawKakuro(canvas: Canvas, puzzle: KakuroPuzzle) {
        val size = puzzle.grid.size
        cellSize = width / size.toFloat()
        smallTextPaint.textSize = cellSize * 0.2f
        textPaint.textSize = cellSize * 0.4f
        gridPaint.strokeWidth = 2f
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        // Draw grid and blocks
        for (i in 0..size) {
            canvas.drawLine(i * cellSize, 0f, i * cellSize, height.toFloat(), gridPaint)
            canvas.drawLine(0f, i * cellSize, width.toFloat(), i * cellSize, gridPaint)
        }

        // Draw blocks and clues
        for (i in 0 until size) {
            for (j in 0 until size) {
                val left = j * cellSize
                val top = i * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                if (puzzle.grid[i][j] == 0) { // Block cell
                    canvas.drawRect(left, top, right, bottom, blockPaint)
                    canvas.drawRect(left, top, right, bottom, borderPaint)

                    // Find clues for this block
                    val horizontalClue = puzzle.clues.find { it.row == i && it.col == j && it.isHorizontal }
                    val verticalClue = puzzle.clues.find { it.row == i && it.col == j && !it.isHorizontal }

                    if (horizontalClue != null || verticalClue != null) {
                        canvas.drawLine(left, top, right, bottom, gridPaint)

                        // Draw horizontal clue (top right)
                        horizontalClue?.let { clue ->
                            val x = left + cellSize * 0.75f
                            val y = top + cellSize * 0.3f
                            canvas.drawText(clue.sum.toString(), x, y, smallTextPaint)
                        }

                        // Draw vertical clue (bottom left)
                        verticalClue?.let { clue ->
                            val x = left + cellSize * 0.25f
                            val y = top + cellSize * 0.7f
                            canvas.drawText(clue.sum.toString(), x, y, smallTextPaint)
                        }
                    }
                } else if (puzzle.grid[i][j] > 0) { // Filled cell (solution)
                    // Draw the number
                    val x = left + cellSize / 2
                    val y = top + cellSize / 2 - (textPaint.ascent() + textPaint.descent()) / 2
                    canvas.drawText(puzzle.grid[i][j].toString(), x, y, textPaint)
                }
            }
        }
    }

    private fun drawBinaryPuzzle(canvas: Canvas, grid: Array<IntArray>) {
        val size = grid.size
        cellSize = width / size.toFloat()
        textPaint.textSize = cellSize * 0.4f

        // Draw grid
        for (i in 0..size) {
            canvas.drawLine(i * cellSize, 0f, i * cellSize, height.toFloat(), gridPaint)
            canvas.drawLine(0f, i * cellSize, width.toFloat(), i * cellSize, gridPaint)
        }

        // Draw numbers
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (grid[i][j] != -1) {
                    val x = j * cellSize + cellSize / 2
                    val y = i * cellSize + cellSize / 2 - (textPaint.ascent() + textPaint.descent()) / 2
                    canvas.drawText(grid[i][j].toString(), x, y, textPaint)
                }
            }
        }
    }
}