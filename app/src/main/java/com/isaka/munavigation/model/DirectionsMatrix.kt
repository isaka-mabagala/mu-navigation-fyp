package com.isaka.munavigation.model

data class DirectionsMatrix(
    var code: String,
    var distances: List<List<Double>>,
    var durations: List<List<Double>>,
)
