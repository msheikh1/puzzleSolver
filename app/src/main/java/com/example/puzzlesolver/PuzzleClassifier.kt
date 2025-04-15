package com.example.puzzlesolver

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class PuzzleClassifier {

    private val client = OkHttpClient()
    private val serverUrl = "http://172.18.124.55:5000/classify" // Replace with actual IP/host

    // Converts Bitmap to base64
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // Sends the image to the Flask server
    fun classify(bitmap: Bitmap, callback: (prediction: String?, confidence: Float?, error: String?) -> Unit) {
        val base64Image = bitmapToBase64(bitmap)

        val json = JSONObject()
        json.put("image", base64Image)

        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url(serverUrl)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(null, null, "Server Error: ${response.code}")
                    return
                }

                val responseBody = response.body?.string()
                try {
                    val jsonResponse = JSONObject(responseBody)
                    val prediction = jsonResponse.getString("prediction")
                    val confidence = jsonResponse.getDouble("confidence").toFloat()
                    callback(prediction, confidence, null)
                } catch (e: Exception) {
                    callback(null, null, "Parsing Error: ${e.message}")
                }
            }

        })
    }
}
