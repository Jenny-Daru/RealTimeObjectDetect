package com.android.jenny.realtimeobjectdetect

import android.graphics.RectF

data class DetectionObject(
    val score: Float,
    val label: String,
    val boundingBox: RectF
)
