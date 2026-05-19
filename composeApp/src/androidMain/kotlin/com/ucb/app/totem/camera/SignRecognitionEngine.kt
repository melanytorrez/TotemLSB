package com.ucb.app.totem.camera

import android.util.Log
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.ArrayDeque

/**
 * The "brain" of the Android app — the Kotlin equivalent of the Python
 * background_thread + process_frame logic from app.py / lsb_mvp_utils.py.
 *
 * Receives MediaPipe results, extracts features, detects motion,
 * and runs TFLite classification to produce letter predictions.
 */
class SignRecognitionEngine(
    private val classifier: SignClassifier,
    private val onLetterDetected: (letter: String, confidence: Float, isMoving: Boolean) -> Unit,
    private val onHandPresenceChanged: (detected: Boolean) -> Unit
) {
    // Feature buffers (mirrors Python logic)
    private var prevFeats: FloatArray? = null
    private val motionHistory = ArrayDeque<Float>(SignClassifier.MOTION_HISTORY_SIZE)
    private val sequenceBuffer = mutableListOf<FloatArray>()

    // Stabilization logic (mirrors app.py)
    private var lastDetectedLetter: String? = null
    private var lastDetectionTimeMs: Long = 0L
    private var handInRoiStartTime: Long? = null
    private var lastHandSeenTime: Long = System.currentTimeMillis()

    companion object {
        private const val TAG = "SignRecognitionEngine"
        private const val RECOGNITION_DELAY_MS = 1000L  // 1 second stabilization
        private const val LETTER_COOLDOWN_MS = 2000L    // 2 seconds between same letter
    }

    /**
     * Called by HandLandmarkerHelper callback with each frame's result.
     */
    fun processResult(result: HandLandmarkerResult, @Suppress("UNUSED_PARAMETER") image: MPImage) {
        val now = System.currentTimeMillis()

        if (result.landmarks().isEmpty()) {
            // No hand detected
            prevFeats = null
            handInRoiStartTime = null
            onHandPresenceChanged(false)

            // Reset last letter so a new gesture of the same letter can be detected
            if (lastDetectedLetter != null) {
                lastDetectedLetter = null
            }
            return
        }

        onHandPresenceChanged(true)
        lastHandSeenTime = now

        // Extract 21 landmarks as [x, y, z]
        val handLandmarks = result.landmarks()[0]
        val landmarkList = handLandmarks.map { lm ->
            floatArrayOf(lm.x(), lm.y(), lm.z())
        }

        val feats = classifier.landmarksToFeatures(landmarkList) ?: return

        // Motion detection
        val score = classifier.motionScore(prevFeats, feats)
        prevFeats = feats.copyOf()

        motionHistory.addLast(score)
        if (motionHistory.size > SignClassifier.MOTION_HISTORY_SIZE) {
            motionHistory.removeFirst()
        }

        val movingCount = motionHistory.count { it > SignClassifier.MOTION_THRESHOLD }
        val isMoving = movingCount >= SignClassifier.MOTION_MOVING_COUNT

        if (isMoving) {
            // Dynamic gesture — accumulate sequence buffer
            sequenceBuffer.add(feats)
            if (sequenceBuffer.size > SignClassifier.SEQ_LEN) {
                sequenceBuffer.removeAt(0)
            }

            if (sequenceBuffer.size == SignClassifier.SEQ_LEN) {
                val sequence = FloatArray(SignClassifier.SEQ_LEN * SignClassifier.FEATURE_DIM)
                for (i in sequenceBuffer.indices) {
                    System.arraycopy(sequenceBuffer[i], 0, sequence, i * SignClassifier.FEATURE_DIM, SignClassifier.FEATURE_DIM)
                }

                val prediction = classifier.classifySequence(sequence)
                if (prediction != null) {
                    handlePrediction(prediction.first, prediction.second, isMoving = true, now)
                }
            }
        } else {
            // Static gesture — classify single frame with stabilization
            sequenceBuffer.clear()

            // Stabilization: require hand to be still for RECOGNITION_DELAY_MS
            if (handInRoiStartTime == null) {
                handInRoiStartTime = now
            }

            val elapsed = now - (handInRoiStartTime ?: now)
            if (elapsed >= RECOGNITION_DELAY_MS) {
                val prediction = classifier.classifyStatic(feats)
                if (prediction != null) {
                    handlePrediction(prediction.first, prediction.second, isMoving = false, now)
                }
                // Reset stabilization timer after recognition
                handInRoiStartTime = null
            }
        }
    }

    private fun handlePrediction(letter: String, confidence: Float, isMoving: Boolean, now: Long) {
        // Cooldown check: don't repeat the same letter too quickly
        val cooldownOk = (now - lastDetectionTimeMs) > LETTER_COOLDOWN_MS
        val differentLetter = letter != lastDetectedLetter

        if (cooldownOk || differentLetter) {
            lastDetectedLetter = letter
            lastDetectionTimeMs = now
            Log.d(TAG, "Detected: $letter (${String.format("%.2f", confidence)}) moving=$isMoving")
            onLetterDetected(letter, confidence, isMoving)
        }
    }

    fun reset() {
        prevFeats = null
        motionHistory.clear()
        sequenceBuffer.clear()
        lastDetectedLetter = null
        lastDetectionTimeMs = 0L
        handInRoiStartTime = null
    }
}
