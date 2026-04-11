package com.workouttracker.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Calls the Cloudflare Worker proxy, which holds the Groq API key server-side.
 * Users never need to provide any API key.
 */
object ClaudeApiClient {

    suspend fun sendMessage(
        workerUrl: String,
        appSecret: String,
        systemPrompt: String,
        messages: List<Pair<String, String>> // role → content
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(workerUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-App-Secret", appSecret)
                doOutput = true
                connectTimeout = 30_000
                readTimeout    = 90_000
            }

            // OpenAI-compatible format (Groq)
            val msgArray = JSONArray()
            msgArray.put(JSONObject().put("role", "system").put("content", systemPrompt))
            messages.forEach { (role, content) ->
                msgArray.put(JSONObject().put("role", role).put("content", content))
            }

            val body = JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("max_tokens", 1024)
                put("messages", msgArray)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

            val code = conn.responseCode
            val raw  = if (code == 200) conn.inputStream.bufferedReader().readText()
                       else conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"

            if (code != 200) throw Exception(parseError(raw, code))

            JSONObject(raw)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    private fun parseError(body: String, code: Int): String = try {
        val msg = JSONObject(body).optJSONObject("error")?.optString("message") ?: body
        "Coach error $code: $msg"
    } catch (_: Exception) {
        "Coach error $code"
    }
}
