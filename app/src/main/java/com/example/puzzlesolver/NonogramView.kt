package com.example.puzzlesolver

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class NonogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var grid: Array<IntArray> = arrayOf()

    private val cellPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2f
    }

    private val thickLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 4f
    }

    private val blackCellPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private var cellSize = 0f

    fun setNonogram(nonogram: Nonogram) {
        this.grid = nonogram.grid
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (grid.isEmpty()) return

        // Calculate cell size to fit available space
        cellSize = (width.coerceAtMost(height) / grid.size.toFloat())

        // Draw grid
        for (i in grid.indices) {
            for (j in grid[i].indices) {
                val left = j * cellSize
                val top = i * cellSize
                val right = left + cellSize
                val bottom = top + cellSize

                // Draw cell background
                canvas.drawRect(left, top, right, bottom, cellPaint)

                // Draw cell content
                if (grid[i][j] == 1) {
                    canvas.drawRect(left, top, right, bottom, blackCellPaint)
                }

                // Draw cell borders
                val paint = if (i % 5 == 0 || j % 5 == 0) thickLinePaint else linePaint
                canvas.drawRect(left, top, right, bottom, paint)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (grid.isNotEmpty()) {
            cellSize = (w.coerceAtMost(h)) / grid.size.toFloat()
        }
    }
}