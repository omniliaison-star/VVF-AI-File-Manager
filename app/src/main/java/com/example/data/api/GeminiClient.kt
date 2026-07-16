package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.IndexedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class SemanticSearchResponse(
        val matchedIds: List<Int>,
        val explanation: String
    )

    suspend fun performSemanticSearch(
        query: String,
        files: List<IndexedFile>
    ): SemanticSearchResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.i(TAG, "API Key is missing, falling back to offline metadata search.")
            return@withContext performOfflineMetadataSearch(query, files)
        }

        // Format files into a compact JSON string for prompt efficiency
        val filesArray = JSONArray()
        files.forEach { file ->
            val obj = JSONObject().apply {
                put("id", file.id)
                put("name", file.name)
                put("mimeType", file.mimeType)
                put("size", formatSize(file.size))
                put("classification", file.classification)
            }
            filesArray.put(obj)
        }

        val promptText = """
            User Search Query: "$query"
            
            Indexed Files:
            ${filesArray.toString(2)}
            
            Instructions:
            Find files that are semantically relevant to the user query. Include matches based on name, extensions, content type, categories, and potential intent (e.g. searching 'bills' matches PDF documents, 'pics' matches images, etc.).
        """.trimIndent()

        try {
            // Build direct JSON payload
            val rootJson = JSONObject()
            
            // Contents array
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
                put("text", "You are a professional offline-first file manager AI assistant. Match semantically relevant file IDs. If the query or language is Hindi, explain your matching choices politely in Hindi or Hinglish.")
            }
            sysPartsArray.put(sysPartObj)
            systemInstructionObj.put("parts", sysPartsArray)
            rootJson.put("systemInstruction", systemInstructionObj)

            // Generation Config with schema
            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                
                val responseSchema = JSONObject().apply {
                    put("type", "OBJECT")
                    val properties = JSONObject().apply {
                        put("matchedIds", JSONObject().apply {
                            put("type", "ARRAY")
                            put("items", JSONObject().apply { put("type", "INTEGER") })
                        })
                        put("explanation", JSONObject().apply {
                            put("type", "STRING")
                        })
                    }
                    put("properties", properties)
                    
                    val requiredArray = JSONArray().apply {
                        put("matchedIds")
                        put("explanation")
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
                    return@withContext SemanticSearchResponse(
                        matchedIds = emptyList(),
                        explanation = "API Error (Code ${response.code}). Please check your API key and connection."
                    )
                }

                val responseBodyStr = response.body?.string()
                if (responseBodyStr.isNullOrEmpty()) {
                    return@withContext SemanticSearchResponse(
                        matchedIds = emptyList(),
                        explanation = "Empty response received from Gemini API."
                    )
                }

                // Parse the response
                val resObj = JSONObject(responseBodyStr)
                val candidates = resObj.getJSONArray("candidates")
                if (candidates.length() == 0) {
                    return@withContext SemanticSearchResponse(
                        matchedIds = emptyList(),
                        explanation = "No matching candidates returned from Gemini API."
                    )
                }

                val candidate = candidates.getJSONObject(0)
                val content = candidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() == 0) {
                    return@withContext SemanticSearchResponse(
                        matchedIds = emptyList(),
                        explanation = "No response text in candidate."
                    )
                }

                val text = parts.getJSONObject(0).getString("text")
                val resultObj = JSONObject(text)
                
                val matchedIdsArray = resultObj.getJSONArray("matchedIds")
                val matchedIds = mutableListOf<Int>()
                for (i in 0 until matchedIdsArray.length()) {
                    matchedIds.add(matchedIdsArray.getInt(i))
                }
                
                val explanation = resultObj.optString("explanation", "Matches found.")
                
                SemanticSearchResponse(matchedIds = matchedIds, explanation = explanation)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception performing search: ", e)
            SemanticSearchResponse(
                matchedIds = emptyList(),
                explanation = "Error: ${e.localizedMessage ?: "Connection error. Please try again."}"
            )
        }
    }

    private fun performOfflineMetadataSearch(
        query: String,
        files: List<IndexedFile>
    ): SemanticSearchResponse {
        val lowercaseQuery = query.lowercase().trim()
        val matchedIds = mutableListOf<Int>()
        
        // Define some semantic mapping for common categories/intents
        val isImageSearch = lowercaseQuery.contains("pic") || lowercaseQuery.contains("photo") || 
                lowercaseQuery.contains("image") || lowercaseQuery.contains("jpeg") || 
                lowercaseQuery.contains("jpg") || lowercaseQuery.contains("png") || 
                lowercaseQuery.contains("तस्वीर") || lowercaseQuery.contains("फोटो")
                
        val isVideoSearch = lowercaseQuery.contains("video") || lowercaseQuery.contains("movie") || 
                lowercaseQuery.contains("vlog") || lowercaseQuery.contains("mp4") || 
                lowercaseQuery.contains("वीडियो")
                
        val isAudioSearch = lowercaseQuery.contains("audio") || lowercaseQuery.contains("song") || 
                lowercaseQuery.contains("music") || lowercaseQuery.contains("podcast") || 
                lowercaseQuery.contains("mp3") || lowercaseQuery.contains("wav") || 
                lowercaseQuery.contains("आवाज") || lowercaseQuery.contains("गाना")
                
        val isDocSearch = lowercaseQuery.contains("doc") || lowercaseQuery.contains("pdf") || 
                lowercaseQuery.contains("text") || lowercaseQuery.contains("txt") || 
                lowercaseQuery.contains("bill") || lowercaseQuery.contains("tax") || 
                lowercaseQuery.contains("दस्तावेज") || lowercaseQuery.contains("फ़ाइल")

        files.forEach { file ->
            val fileNameLower = file.name.lowercase()
            val fileClassLower = file.classification.lowercase()
            val mimeLower = file.mimeType.lowercase()

            var matches = false
            
            // 1. Direct name/path/mime contains query
            if (fileNameLower.contains(lowercaseQuery) || 
                file.path.lowercase().contains(lowercaseQuery) || 
                mimeLower.contains(lowercaseQuery)) {
                matches = true
            }
            // 2. Semantic-ish category matching
            else if (isImageSearch && fileClassLower == "images") {
                matches = true
            } else if (isVideoSearch && fileClassLower == "videos") {
                matches = true
            } else if (isAudioSearch && fileClassLower == "audios") {
                matches = true
            } else if (isDocSearch && fileClassLower == "documents") {
                matches = true
            }
            
            if (matches) {
                matchedIds.add(file.id)
            }
        }

        val explanation = if (matchedIds.isEmpty()) {
            "ऑफ़लाइन खोज: '$query' के लिए कोई प्रासंगिक फ़ाइल नहीं मिली।"
        } else {
            "ऑफ़लाइन खोज (Offline fallback): हमने आपके लिए '${matchedIds.size}' प्रासंगिक फ़ाइलें ढूंढी हैं जो आपके प्रश्न '$query' से मेल खाती हैं।"
        }

        return SemanticSearchResponse(matchedIds = matchedIds, explanation = explanation)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val ch = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), ch)
    }
}
