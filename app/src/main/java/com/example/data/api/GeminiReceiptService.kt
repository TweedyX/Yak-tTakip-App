package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ReceiptScanResult(
    val price: Double?,
    val date: String?
)

class GeminiReceiptService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun scanReceipt(bitmap: Bitmap): ReceiptScanResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiReceiptService", "API Key is missing or default placeholder!")
            return@withContext null
        }

        // Convert Bitmap to Base64
        val base64Image = bitmap.toBase64()

        // Build prompt
        val prompt = "Analyze this fuel receipt ('yakıt fişi') and extract key details. " +
                "Extract the total cost / total price paid ('Toplam Tutar', 'Toplam' or 'Tutar' in Turkish Lira) as a decimal number (double), " +
                "and the purchase date ('Tarih') as a string in 'dd.MM.yyyy' format (or empty string/null if not found). " +
                "Do not make up values, extract them strictly from the receipt image. " +
                "Provide the result strictly as a JSON object with two keys: 'price' (decimal double or null) and 'date' (string or null). " +
                "Example response format: {\"price\": 1250.50, \"date\": \"15.07.2026\"}."

        // JSON payload using raw strings
        val jsonPayload = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": ${escapeJsonString(prompt)}
                    },
                    {
                      "inlineData": {
                        "mimeType": "image/jpeg",
                        "data": "$base64Image"
                      }
                    }
                  ]
                }
              ],
              "generationConfig": {
                "responseMimeType": "application/json"
              }
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toRequestBody(mediaType)

        // Using gemini-3.1-pro-preview as specified
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GeminiReceiptService", "Network call failed: ${response.code} ${response.message}")
                    val errorBody = response.body?.string()
                    Log.e("GeminiReceiptService", "Error body: $errorBody")
                    return@withContext null
                }

                val responseBodyStr = response.body?.string() ?: return@withContext null
                Log.d("GeminiReceiptService", "Raw Response: $responseBodyStr")
                parseGeminiResponse(responseBodyStr)
            }
        } catch (e: Exception) {
            Log.e("GeminiReceiptService", "Error during API call", e)
            null
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to 80% JPEG to fit payload size safely
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun escapeJsonString(s: String): String {
        return Moshi.Builder().build().adapter(String::class.java).toJson(s)
    }

    private fun parseGeminiResponse(jsonString: String): ReceiptScanResult? {
        try {
            val jsonAdapter = moshi.adapter(Map::class.java)
            val parsedMap = jsonAdapter.fromJson(jsonString) as? Map<*, *> ?: return null
            val candidates = parsedMap["candidates"] as? List<*> ?: return null
            val firstCandidate = candidates.firstOrNull() as? Map<*, *> ?: return null
            val content = firstCandidate["content"] as? Map<*, *> ?: return null
            val parts = content["parts"] as? List<*> ?: return null
            val firstPart = parts.firstOrNull() as? Map<*, *> ?: return null
            val text = firstPart["text"] as? String ?: return null

            Log.d("GeminiReceiptService", "Extracted text: $text")

            val innerMap = jsonAdapter.fromJson(text) as? Map<*, *> ?: return null
            val price = when (val p = innerMap["price"]) {
                is Number -> p.toDouble()
                is String -> p.toDoubleOrNull()
                else -> null
            }
            val date = innerMap["date"] as? String

            return ReceiptScanResult(price = price, date = date)
        } catch (e: Exception) {
            Log.e("GeminiReceiptService", "Failed to parse Gemini JSON response", e)
            return null
        }
    }
}
