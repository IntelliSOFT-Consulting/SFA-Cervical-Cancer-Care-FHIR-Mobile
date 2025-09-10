package com.icl.cervicalcancercare.network

import com.icl.cervicalcancercare.models.DataProcessingResponse
import com.icl.cervicalcancercare.models.ExtractedData
import com.icl.cervicalcancercare.models.FhirBundle
import com.icl.cervicalcancercare.models.Login
import com.icl.cervicalcancercare.models.LoginResponse
import com.icl.cervicalcancercare.models.Payload
import retrofit2.Response

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url


interface Interface {

    @POST("login")
    suspend fun signInUser(@Body login: Login): Response<LoginResponse>

    @GET
    suspend fun fetchBundle(@Url url: String): FhirBundle

    @POST("query_clinical_decision_making")
    suspend fun processData(
        @Body data: ExtractedData,
        @Header("Authorization") token: String?
    ): Response<DataProcessingResponse>

    @POST("query_clinical_decision_making")
    suspend fun processUpdatedData(
        @Body data: Payload,
        @Header("Authorization") token: String?
    ): Response<DataProcessingResponse>

}