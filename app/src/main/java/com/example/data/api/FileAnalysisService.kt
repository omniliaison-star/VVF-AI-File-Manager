package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object FileAnalysisService {
    private const val TAG = "FileAnalysisService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class AnalysisResult(
        val summary: String,
        val keywords: String
    )

    suspend fun analyzeTextFile(file: File): AnalysisResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.i(TAG, "API Key is missing or default placeholder, skipping online analysis.")
            return@withContext null
        }

        if (!file.exists() || !file.isFile) {
            Log.e(TAG, "File does not exist or is not a file: ${file.absolutePath}")
            return@withContext null
        }

        try {
            // Read first 10KB to avoid excessive token usage
            val maxLength = 10240
            val content = file.readText(Charsets.UTF_8)
            val truncatedContent = if (content.length > maxLength) {
                content.substring(0, maxLength) + "\n[Truncated...]"
            } else {
                content
            }

            val promptText = """
                Analyze the following text file contents to generate:
                1. A brief summary (maximum 2 sentences).
                2. A comma-separated list of key keywords or tags representing the content.
                
                File Contents:
                $truncatedContent
            """.trimIndent()

            val rootJson = JSONObject()
            
            // Contents
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject().apply {
                put("text", promptText)
            }
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // System instructions
            val systemInstructionObj = JSONObject()
            val sysPartsArray = JSONArray()
            val sysPartObj = JSONObject().apply {
                put("text", "You are an AI document analysis system. You must analyze the text contents and return a JSON object with 'summary' (string) and 'keywords' (string, comma-separated values).")
            }
            sysPartsArray.put(sysPartObj)
            systemInstructionObj.put("parts", sysPartsArray)
            rootJson.put("systemInstruction", systemInstructionObj)

            // Generation config
            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                val responseSchema = JSONObject().apply {
                    put("type", "OBJECT")
                    val properties = JSONObject().apply {
                        put("summary", JSONObject().apply { put("type", "STRING") })
                        put("keywords", JSONObject().apply { put("type", "STRING") })
                    }
                    put("properties", properties)
                    val requiredArray = JSONArray().apply {
                        put("summary")
                        put("keywords")
                    }
                    put("required", requiredArray)
                }
                put("responseSchema", responseSchema)
            }
            rootJson.put("generationConfig", generationConfig)

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: Code ${response.code}, Body: $errBody")
                    return@withContext null
                }

                val bodyStr = response.body?.string() ?: return@withContext null
                val resObj = JSONObject(bodyStr)
                val candidates = resObj.getJSONArray("candidates")
                if (candidates.length() == 0) return@withContext null
                
                val text = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val resultObj = JSONObject(text)
                val summary = resultObj.optString("summary", "")
                val keywords = resultObj.optString("keywords", "")
                
                AnalysisResult(summary = summary, keywords = keywords)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception performing content analysis", e)
            null
        }
    }
}
