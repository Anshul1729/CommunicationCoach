package com.communicationcoach.data.api

import com.communicationcoach.data.model.GeminiRequest
import com.communicationcoach.data.model.GeminiResponse
import com.communicationcoach.data.model.SpeechRequest
import com.communicationcoach.data.model.SpeechResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface SpeechToTextApiService {
    @POST("v1p1beta1/speech:recognize")
    suspend fun recognize(
        @Header("Authorization") auth: String,
        @Body request: SpeechRequest
    ): Response<SpeechResponse>
}

interface VertexAiApiService {
    @POST
    suspend fun generate(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
