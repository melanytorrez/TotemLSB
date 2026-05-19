package com.ucb.app.totem.camera

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Wraps MediaPipe HandLandmarker for real-time hand detection.
 * Uses LIVE_STREAM mode for non-blocking, callback-driven processing.
 */
class HandLandmarkerHelper(
    private val context: Context,
    private val onResults: (HandLandmarkerResult, MPImage) -> Unit,
    private val onError: (Exception) -> Unit
) {
    private var handLandmarker: HandLandmarker? = null

    companion object {
        private const val TAG = "HandLandmarkerHelper"
        private const val MODEL_ASSET = "hand_landmarker.task"
        private const val MIN_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_TRACKING_CONFIDENCE = 0.5f
        private const val MIN_PRESENCE_CONFIDENCE = 0.5f
        private const val MAX_NUM_HANDS = 1
    }

    fun initialize() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_ASSET)
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(MAX_NUM_HANDS)
                .setMinHandDetectionConfidence(MIN_DETECTION_CONFIDENCE)
                .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
                .setMinHandPresenceConfidence(MIN_PRESENCE_CONFIDENCE)
                .setResultListener { result, input ->
                    onResults(result, input)
                }
                .setErrorListener { e ->
                    onError(RuntimeException(e.message))
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Log.i(TAG, "HandLandmarker initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing HandLandmarker", e)
            onError(e)
        }
    }

    fun detectAsync(mpImage: MPImage, timestampMs: Long) {
        handLandmarker?.detectAsync(mpImage, timestampMs)
    }

    fun close() {
        handLandmarker?.close()
        handLandmarker = null
    }
}
