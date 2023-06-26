package com.isaka.munavigation.api

import com.isaka.munavigation.model.DirectionsMatrix
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface MapboxApi {
    @GET("directions-matrix/v1/mapbox/{profile}/{coordinates}")
    fun getDirectionsMatrix(
        @Path("profile") profile: String,
        @Path("coordinates") coordinates: String,
        @QueryMap parameters: HashMap<String, String>
    ): Call<DirectionsMatrix>

    companion object {
        const val MAPBOX_MATRIX_SUCCESS = "Ok"
        const val MAPBOX_MATRIX_NO_ROUTE = "NoRoute"
        const val MAPBOX_MATRIX_WALKING_PROFILE = "walking"
        const val MAPBOX_MATRIX_DRIVING_PROFILE = "driving"
    }
}