package dev.devfest.agent.data.remote

import dev.devfest.agent.data.remote.dto.GenerateContentRequest
import dev.devfest.agent.data.remote.dto.GenerateContentResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Base URL: https://generativelanguage.googleapis.com/
 * La API key se envía en el header `x-goog-api-key` (interceptor en LlmModule).
 *
 * Para streaming real token a token usa `:streamGenerateContent?alt=sse`
 * con OkHttp SSE y conviértelo en Flow con callbackFlow.
 */
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body body: GenerateContentRequest,
    ): GenerateContentResponse
}
