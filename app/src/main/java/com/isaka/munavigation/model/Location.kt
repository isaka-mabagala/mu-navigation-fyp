package com.isaka.munavigation.model

data class Location(
    var id: String,
    var title: String,
    var description: String,
    var category: String,
    var campus: String,
    var longitude: Double,
    var latitude: Double,
    var distance: Double? = null,
    var duration: Double? = null,
    var featureIdentifier: String? = null,
)
