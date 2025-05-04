package com.example.puzzlesolver

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class NonogramDisplayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_nonogram_display)

        val nonogram = intent.getParcelableExtra<Nonogram>("NONOGRAM")
        val nonogramView = findViewById<NonogramView>(R.id.nonogramView)
        val backButton = findViewById<Button>(R.id.backButton)

        nonogram?.let {
            nonogramView.setNonogram(it)
        }

        backButton.setOnClickListener {
            finish() // Close this activity and return to the previous one
        }
    }
}