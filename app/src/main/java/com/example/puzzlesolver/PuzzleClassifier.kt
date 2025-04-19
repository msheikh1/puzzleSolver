package com.example.puzzlesolver

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class PuzzleClassifier {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val serverUrl = "https://f432-113-23-202-19.ngrok-free.app/classify" // Replace with your server IP

    // Converts Bitmap to base64 string
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream) // JPEG for better size/performance
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // Sends the image to the Flask server and receives the prediction
    fun classify(
        bitmap: Bitmap,
        callback: (prediction: String?, confidence: Float?, error: String?) -> Unit
    ) {
        val base64Image = bitmapToBase64(bitmap)

        val json = JSONObject().apply {
            put("image", base64Image)
        }

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url(serverUrl)
            .post(body)
            .build()

        Log.d("PuzzleClassifier", "Sending image to server...")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PuzzleClassifier", "Network error: ${e.message}")
                callback(null, null, "Network Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    Log.e("PuzzleClassifier", "Server error: ${response.code}")
                    callback(null, null, "Server Error: ${response.code}")
                    return
                }

                Log.d("PuzzleClassifier", "Received response: $responseBody")

                try {
                    val jsonResponse = JSONObject(responseBody)
                    val prediction = jsonResponse.getString("prediction")
                    val confidence = jsonResponse.getDouble("confidence").toFloat()
                    callback(prediction, confidence, null)
                } catch (e: Exception) {
                    Log.e("PuzzleClassifier", "Parsing error: ${e.message}")
                    callback(null, null, "Parsing Error: ${e.message}")
                }
            }
        })
    }
}
