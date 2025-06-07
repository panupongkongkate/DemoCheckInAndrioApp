package com.example.checkinapp.detection

import android.graphics.RectF

data class DetectionResult(
    val boundingBox: RectF,
    val label: String,
    val confidence: Float
)

data class DetectionResults(
    val detections: List<DetectionResult>,
    val inferenceTime: Long,
    val imageWidth: Int,
    val imageHeight: Int
)