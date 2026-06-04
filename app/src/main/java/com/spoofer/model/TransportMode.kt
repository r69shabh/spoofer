package com.spoofer.model

enum class TransportMode(val label: String, val osrmProfile: String, val defaultSpeedKmh: Float) {
    WALK("Walking", "foot", 5f),
    BIKE("Cycling", "cycling", 15f),
    CAR("Driving", "driving", 60f),
}

enum class SpeedMode { MANUAL, CURRENT }
