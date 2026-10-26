package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiModelInfo
import com.example.data.model.AiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchModels(provider: AiProvider, apiKey: String): List<AiModelInfo> = withContext(Dispatchers.IO) {
        val effectiveGeminiKey = if (apiKey.isNotBlank()) apiKey else BuildConfig.GEMINI_API_KEY
        try {
            when (provider) {
                AiProvider.GEMINI -> {
                    if (effectiveGeminiKey.isBlank() || effectiveGeminiKey == "MY_GEMINI_API_KEY") {
                        return@withContext defaultGeminiModels()
                    }
                    val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$effectiveGeminiKey"
                    val request = Request.Builder().url(url).get().build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()
                    if (response.isSuccessful && !body.isNullOrEmpty()) {
                        val json = JSONObject(body)
                        val modelsArr = json.optJSONArray("models") ?: JSONArray()
                        val list = mutableListOf<AiModelInfo>()
                        for (i in 0 until modelsArr.length()) {
                            val m = modelsArr.getJSONObject(i)
                            val name = m.optString("name").removePrefix("models/")
                            val displayName = m.optString("displayName", name)
                            val desc = m.optString("description", "")
                            val supportedMethods = m.optJSONArray("supportedGenerationMethods")
                            var canGenerate = false
                            if (supportedMethods != null) {
                                for (j in 0 until supportedMethods.length()) {
                                    if (supportedMethods.getString(j) == "generateContent") {
                                        canGenerate = true
                                        break
                                    }
                                }
                            }
                            if (canGenerate) {
                                val badge = when {
                                    name == "gemini-2.5-flash" -> "Default • Latest Flash"
                                    name.contains("2.5") -> "Updated • Ultra Fast"
                                    name.contains("2.0") -> "Gemini 2.0 Series"
                                    name.contains("1.5-pro") || name.contains("pro") -> "Advanced Reasoning"
                                    name.contains("flash") -> "Fast & Efficient"
                                    else -> "Live & Active"
                                }
                                list.add(
                                    AiModelInfo(
                                        id = name,
                                        name = displayName,
                                        provider = AiProvider.GEMINI,
                                        badge = badge,
                                        description = desc
                                    )
                                )
                            }
                        }
                        // Sort so latest default model is on top
                        list.sortWith(compareByDescending {
                            when {
                                it.id == "gemini-2.5-flash" -> 1000
                                it.id.contains("2.5") -> 900
                                it.id.contains("2.0-flash") -> 850
                                it.id.contains("1.5-flash") -> 800
                                it.id.contains("pro") -> 700
                                else -> 100
                            }
                        })
                        if (list.isNotEmpty()) return@withContext list
                    }
                    defaultGeminiModels()
                }

                AiProvider.OPEN_ROUTER -> {
                    val url = "https://openrouter.ai/api/v1/models"
                    val reqBuilder = Request.Builder().url(url).get()
                    if (apiKey.isNotBlank()) {
                        reqBuilder.addHeader("Authorization", "Bearer $apiKey")
                    }
                    val response = client.newCall(reqBuilder.build()).execute()
                    val body = response.body?.string()
                    if (response.isSuccessful && !body.isNullOrEmpty()) {
                        val json = JSONObject(body)
                        val dataArr = json.optJSONArray("data") ?: JSONArray()
                        val list = mutableListOf<AiModelInfo>()
                        for (i in 0 until minOf(dataArr.length(), 80)) {
                            val m = dataArr.getJSONObject(i)
                            val id = m.optString("id")
                            val name = m.optString("name", id)
                            val desc = m.optString("description", "")
                            val badge = when {
                                id.contains("gemini-2.5") -> "Default • Google Gemini"
                                id.contains("gemini") -> "Google Gemini"
                                id.contains("gpt-4o") -> "OpenAI Flagship"
                                id.contains("claude-3.5") -> "Anthropic Claude"
                                id.contains("deepseek") -> "DeepSeek AI"
                                else -> "Active Live Model"
                            }
                            list.add(
                                AiModelInfo(
                                    id = id,
                                    name = name,
                                    provider = AiProvider.OPEN_ROUTER,
                                    badge = badge,
                                    description = desc
                                )
                            )
                        }
                        if (list.isNotEmpty()) return@withContext list
                    }
                    defaultOpenRouterModels()
                }
            }
        } catch (e: Exception) {
            Log.e("AiApiService", "Error fetching models for $provider: ${e.message}")
            if (provider == AiProvider.GEMINI) defaultGeminiModels() else defaultOpenRouterModels()
        }
    }

    suspend fun generateResponse(
        provider: AiProvider,
        apiKey: String,
        model: String,
        systemInstruction: String,
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val effectiveGeminiKey = if (apiKey.isNotBlank()) apiKey else BuildConfig.GEMINI_API_KEY
        try {
            when (provider) {
                AiProvider.GEMINI -> {
                    if (effectiveGeminiKey.isBlank() || effectiveGeminiKey == "MY_GEMINI_API_KEY") {
                        return@withContext Result.failure(
                            Exception("Gemini API key is not configured. Please add your API key in AI Settings.")
                        )
                    }
                    val targetModel = if (model.isNotBlank()) model else "gemini-2.5-flash"
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$effectiveGeminiKey"

                    val jsonReq = JSONObject().apply {
                        val contentsArr = JSONArray()
                        val contentObj = JSONObject().apply {
                            val partsArr = JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            }
                            put("parts", partsArr)
                        }
                        contentsArr.put(contentObj)
                        put("contents", contentsArr)

                        if (systemInstruction.isNotBlank()) {
                            put(
                                "systemInstruction",
                                JSONObject().put(
                                    "parts",
                                    JSONArray().put(JSONObject().put("text", systemInstruction))
                                )
                            )
                        }
                        put(
                            "generationConfig",
                            JSONObject().apply {
                                put("temperature", 0.7)
                                put("maxOutputTokens", 800)
                            }
                        )
                    }

                    val mediaType = "application/json".toMediaType()
                    val reqBody = jsonReq.toString().toRequestBody(mediaType)
                    val request = Request.Builder().url(url).post(reqBody).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()

                    if (!response.isSuccessful || body.isNullOrEmpty()) {
                        val errMsg = parseError(body) ?: "HTTP ${response.code}: ${response.message}"
                        return@withContext Result.failure(Exception(errMsg))
                    }

                    val resJson = JSONObject(body)
                    val candidates = resJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text")
                        if (!text.isNullOrBlank()) {
                            return@withContext Result.success(text.trim())
                        }
                    }
                    Result.failure(Exception("No response generated by model."))
                }

                AiProvider.OPEN_ROUTER -> {
                    if (apiKey.isBlank()) {
                        return@withContext Result.failure(
                            Exception("OpenRouter API key is not configured. Please add your key in AI Settings.")
                        )
                    }
                    val targetModel = if (model.isNotBlank()) model else "google/gemini-2.5-flash"
                    val url = "https://openrouter.ai/api/v1/chat/completions"

                    val jsonReq = JSONObject().apply {
                        put("model", targetModel)
                        val messagesArr = JSONArray()
                        if (systemInstruction.isNotBlank()) {
                            messagesArr.put(
                                JSONObject().apply {
                                    put("role", "system")
                                    put("content", systemInstruction)
                                }
                            )
                        }
                        messagesArr.put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            }
                        )
                        put("messages", messagesArr)
                        put("max_tokens", 800)
                    }

                    val mediaType = "application/json".toMediaType()
                    val reqBody = jsonReq.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("HTTP-Referer", "https://mr-techlabstudio.vercel.app")
                        .addHeader("X-Title", "MR AI Assistant")
                        .post(reqBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body?.string()

                    if (!response.isSuccessful || body.isNullOrEmpty()) {
                        val errMsg = parseError(body) ?: "HTTP ${response.code}: ${response.message}"
                        return@withContext Result.failure(Exception(errMsg))
                    }

                    val resJson = JSONObject(body)
                    val choices = resJson.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val msg = choices.getJSONObject(0).optJSONObject("message")
                        val text = msg?.optString("content")
                        if (!text.isNullOrBlank()) {
                            return@withContext Result.success(text.trim())
                        }
                    }
                    Result.failure(Exception("No response choices returned by OpenRouter."))
                }
            }
        } catch (e: Exception) {
            Log.e("AiApiService", "Error calling $provider: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun generateMultimodalResponse(
        provider: AiProvider,
        apiKey: String,
        model: String,
        systemInstruction: String,
        prompt: String,
        imageBase64Jpeg: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val effectiveGeminiKey = if (apiKey.isNotBlank()) apiKey else BuildConfig.GEMINI_API_KEY
        try {
            when (provider) {
                AiProvider.GEMINI -> {
                    if (effectiveGeminiKey.isBlank() || effectiveGeminiKey == "MY_GEMINI_API_KEY") {
                        return@withContext Result.failure(
                            Exception("Gemini API key is not configured. Please add your API key in AI Settings.")
                        )
                    }
                    val targetModel = if (model.isNotBlank() && !model.contains("audio")) model else "gemini-2.5-flash"
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$effectiveGeminiKey"

                    val jsonReq = JSONObject().apply {
                        val contentsArr = JSONArray()
                        val contentObj = JSONObject().apply {
                            val partsArr = JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                                put(
                                    JSONObject().put(
                                        "inlineData",
                                        JSONObject().apply {
                                            put("mimeType", "image/jpeg")
                                            put("data", imageBase64Jpeg)
                                        }
                                    )
                                )
                            }
                            put("parts", partsArr)
                        }
                        contentsArr.put(contentObj)
                        put("contents", contentsArr)

                        if (systemInstruction.isNotBlank()) {
                            put(
                                "systemInstruction",
                                JSONObject().put(
                                    "parts",
                                    JSONArray().put(JSONObject().put("text", systemInstruction))
                                )
                            )
                        }
                        put(
                            "generationConfig",
                            JSONObject().apply {
                                put("temperature", 0.4)
                                put("maxOutputTokens", 600)
                            }
                        )
                    }

                    val mediaType = "application/json".toMediaType()
                    val reqBody = jsonReq.toString().toRequestBody(mediaType)
                    val request = Request.Builder().url(url).post(reqBody).build()
                    val response = client.newCall(request).execute()
                    val body = response.body?.string()

                    if (!response.isSuccessful || body.isNullOrEmpty()) {
                        val errMsg = parseError(body) ?: "HTTP ${response.code}: ${response.message}"
                        return@withContext Result.failure(Exception(errMsg))
                    }

                    val resJson = JSONObject(body)
                    val candidates = resJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text")
                        if (!text.isNullOrBlank()) {
                            return@withContext Result.success(text.trim())
                        }
                    }
                    Result.failure(Exception("No vision response returned by model."))
                }

                AiProvider.OPEN_ROUTER -> {
                    if (apiKey.isBlank()) {
                        return@withContext Result.failure(
                            Exception("OpenRouter API key is not configured. Please add your key in AI Settings.")
                        )
                    }
                    val targetModel = if (model.isNotBlank()) model else "google/gemini-2.5-flash"
                    val url = "https://openrouter.ai/api/v1/chat/completions"

                    val jsonReq = JSONObject().apply {
                        put("model", targetModel)
                        val messagesArr = JSONArray()
                        if (systemInstruction.isNotBlank()) {
                            messagesArr.put(
                                JSONObject().apply {
                                    put("role", "system")
                                    put("content", systemInstruction)
                                }
                            )
                        }
                        val userContentArr = JSONArray().apply {
                            put(JSONObject().put("type", "text").put("text", prompt))
                            put(
                                JSONObject().put(
                                    "type",
                                    "image_url"
                                ).put(
                                    "image_url",
                                    JSONObject().put("url", "data:image/jpeg;base64,$imageBase64Jpeg")
                                )
                            )
                        }
                        messagesArr.put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", userContentArr)
                            }
                        )
                        put("messages", messagesArr)
                        put("max_tokens", 600)
                    }

                    val mediaType = "application/json".toMediaType()
                    val reqBody = jsonReq.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("HTTP-Referer", "https://mr-techlabstudio.vercel.app")
                        .addHeader("X-Title", "MR AI Assistant")
                        .post(reqBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body?.string()

                    if (!response.isSuccessful || body.isNullOrEmpty()) {
                        val errMsg = parseError(body) ?: "HTTP ${response.code}: ${response.message}"
                        return@withContext Result.failure(Exception(errMsg))
                    }

                    val resJson = JSONObject(body)
                    val choices = resJson.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val msg = choices.getJSONObject(0).optJSONObject("message")
                        val text = msg?.optString("content")
                        if (!text.isNullOrBlank()) {
                            return@withContext Result.success(text.trim())
                        }
                    }
                    Result.failure(Exception("No vision response choices returned by OpenRouter."))
                }
            }
        } catch (e: Exception) {
            Log.e("AiApiService", "Error calling vision on $provider: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseError(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val json = JSONObject(body)
            if (json.has("error")) {
                val errObj = json.optJSONObject("error")
                errObj?.optString("message") ?: json.optString("error")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun defaultGeminiModels(): List<AiModelInfo> = listOf(
        AiModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash", AiProvider.GEMINI, "Fast & efficient", "General purpose high speed model"),
        AiModelInfo("gemini-3.5-flash", "Gemini 3.5 Flash", AiProvider.GEMINI, "Best for general use", "Latest fast reasoning model"),
        AiModelInfo("gemini-3.1-pro-preview", "Gemini 3.1 Pro Preview", AiProvider.GEMINI, "Advanced reasoning", "Complex queries and deep knowledge"),
        AiModelInfo("gemini-2.5-flash-native-audio-preview-12-2025", "Gemini Flash Audio", AiProvider.GEMINI, "Voice & Audio", "Real-time audio processing", supportsAudio = true)
    )

    fun defaultOpenRouterModels(): List<AiModelInfo> = listOf(
        AiModelInfo("google/gemini-2.5-flash", "Google: Gemini 2.5 Flash", AiProvider.OPEN_ROUTER, "Fast & efficient", "High performance multimodal"),
        AiModelInfo("openai/gpt-4o-mini", "OpenAI: GPT-4o Mini", AiProvider.OPEN_ROUTER, "Fast & efficient", "Compact high intelligence"),
        AiModelInfo("anthropic/claude-3.5-haiku", "Anthropic: Claude 3.5 Haiku", AiProvider.OPEN_ROUTER, "Fast & nuanced", "Quick conversational responses"),
        AiModelInfo("deepseek/deepseek-chat", "DeepSeek: Chat V3", AiProvider.OPEN_ROUTER, "Strong reasoning", "Economical high capability")
    )
}
