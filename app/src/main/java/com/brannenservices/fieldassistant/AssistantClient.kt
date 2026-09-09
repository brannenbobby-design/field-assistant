package com.brannenservices.fieldassistant

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class AssistantClient {
    private val http = OkHttpClient()

    fun ask(question: String, callback: (Result<String>) -> Unit) {
        val baseUrl = BuildConfig.ASSISTANT_BACKEND_URL.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            callback(Result.failure(IllegalStateException("Backend URL is not configured yet")))
            return
        }

        val body = JSONObject()
            .put("message", question)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/ask")
            .post(body)
            .build()

        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = callback(Result.failure(e))

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val text = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        callback(Result.failure(IOException("Assistant server returned ${it.code}")))
                        return
                    }
                    runCatching {
                        JSONObject(text).optString("reply").ifBlank { error("Empty assistant reply") }
                    }.let(callback)
                }
            }
        })
    }
}
