package com.example.puzzlesolver

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class PuzzleInfoDialog(
    private val title: String,
    private val description: String,
    private val rules: List<String>,
    private val tips: List<String>,
    private val difficulty: String,
    private val videoUrl: String?,
    private val activityClass: Class<*>?
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_puzzle_info, null)

        val titleView = view.findViewById<TextView>(R.id.puzzleTitle)
        val descView = view.findViewById<TextView>(R.id.puzzleDescription)
        val difficultyView = view.findViewById<TextView>(R.id.puzzleDifficulty)
        val rulesView = view.findViewById<TextView>(R.id.puzzleRules)
        val tipsView = view.findViewById<TextView>(R.id.puzzleTips)

        val videoPlaceholder = view.findViewById<ImageView>(R.id.fallbackPlaceholder)

        val startButton = view.findViewById<Button>(R.id.launchPuzzleButton)
        val dismissButton = view.findViewById<Button>(R.id.dismissButton)

        val backButton = view.findViewById<View>(R.id.backButton)
        backButton.setOnClickListener {
            dismiss()
        }


        titleView.text = title
        descView.text = description
        difficultyView.text = difficulty
        rulesView.text = rules.joinToString(separator = "\n• ", prefix = "• ")
        tipsView.text = tips.joinToString(separator = "\n• ", prefix = "• ")

        // Handle video
        val webView = view.findViewById<WebView>(R.id.youtubeWebView)

        if (videoUrl != null && hasInternet()) {
            WebView.setWebContentsDebuggingEnabled(true)

            webView.visibility = View.VISIBLE
            videoPlaceholder.visibility = View.GONE
            webView.settings.javaScriptEnabled = true
            val embedLink = toEmbedUrl(videoUrl)
            val embeddedHtml = """
                <html>
                <body style="margin:0;padding:0;">
                    <iframe width="100%" height="100%" src="$embedLink" frameborder="0" allowfullscreen></iframe>
                </body>
                </html>
            """.trimIndent()

            WebView.setWebContentsDebuggingEnabled(true)
            webView.loadData(embeddedHtml, "text/html", "utf-8")
        } else {
            webView.visibility = View.GONE
            videoPlaceholder.visibility = View.VISIBLE
        }


        // Handle Start Button
        if (activityClass != null) {
            startButton.setOnClickListener {
                startActivity(Intent(requireContext(), activityClass))
                dismiss()
            }
        } else {
            startButton.visibility = View.GONE
        }

        dismissButton.setOnClickListener {
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    private fun hasInternet(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun toEmbedUrl(youtubeUrl: String): String {
        val regex = Regex("""(?:youtube\.com/watch\?v=|youtu\.be/)([a-zA-Z0-9_-]{11})""")
        val match = regex.find(youtubeUrl)
        val videoId = match?.groupValues?.get(1)
        return if (videoId != null) {
            "https://www.youtube.com/embed/$videoId"
        } else {
            youtubeUrl
        }
    }

}
