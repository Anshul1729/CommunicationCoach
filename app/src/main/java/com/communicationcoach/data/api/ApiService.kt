package com.communicationcoach.data.api

import com.communicationcoach.data.model.GeminiRequest
import com.communicationcoach.data.model.GeminiResponse
import com.communicationcoach.data.model.SpeechRequest
import com.communicationcoach.data.model.SpeechResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SpeechToTextApiService {
    @POST("v1p1beta1/speech:recognize")
    suspend fun recognize(
        @Header("Authorization") auth: String,
        @Body request: SpeechRequest
    ): Response<SpeechResponse>
}

interface VertexAiApiService {
    @POST("v1/projects/op-d2r/locations/us-central1/publishers/google/models/gemini-2.5-flash:generateContent")
    suspend fun generate(
        @Header("Authorization") auth: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
