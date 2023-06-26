package com.isaka.munavigation.api

import com.isaka.munavigation.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitBuilder {
    private const val URL = Constants.MAPBOX_URL
    private val logger = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS)
    private val okHttp = OkHttpClient.Builder().addInterceptor(logger)
    private val builder =
        Retrofit.Builder().baseUrl(URL).addConverterFactory(GsonConverterFactory.create())
            .client(okHttp.build())
    private val retrofit = builder.build()

    fun <T> buildApi(apiType: Class<T>): T {
        return retrofit.create(apiType)
    }
}