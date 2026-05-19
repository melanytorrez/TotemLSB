package com.ucb.app.totem.camera

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Kotlin port of lsb_mvp_utils.py landmarks_to_features + TFLite inference.
 *
 * Converts MediaPipe hand landmarks into a normalized 63-feature vector
 * (21 points × 3 coords) and runs the static or sequential TFLite model.
 */
class SignClassifier(private val context: Context) {

    private var staticInterpreter: Interpreter? = null
    private var seqInterpreter: Interpreter? = null
    private var staticLabels: Map<Int, String> = emptyMap()
    private var seqLabels: Map<Int, String> = emptyMap()

    companion object {
        private const val STATIC_MODEL = "models/lsb_alpha.tflite"
        private const val SEQ_MODEL = "models/lsb_seq.tflite"
        private const val STATIC_LABELS = "models/lsb_alpha_labels.json"
        private const val SEQ_LABELS = "models/lsb_seq_labels.json"

        const val SEQ_LEN = 20
        const val FEATURE_DIM = 63 // 21 landmarks × 3 (x, y, z)
        const val MOTION_THRESHOLD = 0.15f
        const val MOTION_HISTORY_SIZE = 15
        const val MOTION_MOVING_COUNT = 8
        const val CONFIDENCE_THRESHOLD = 0.80f
    }

    fun initialize() {
        staticInterpreter = Interpreter(loadModel(STATIC_MODEL))
        seqInterpreter = Interpreter(loadModel(SEQ_MODEL))
        staticLabels = loadLabels(STATIC_LABELS)
        seqLabels = loadLabels(SEQ_LABELS)
    }

    fun close() {
        staticInterpreter?.close()
        seqInterpreter?.close()
    }

    // --- Port of landmarks_to_features ---
    fun landmarksToFeatures(landmarks: List<FloatArray>): FloatArray? {
        if (landmarks.size != 21) return null

        // pts = array of [x, y, z] for each landmark
        val pts = Array(21) { floatArrayOf(landmarks[it][0], landmarks[it][1], landmarks[it][2]) }

        // wrist = pts[0]
        val wrist = pts[0].copyOf()

        // pts_rel = pts - wrist
        val ptsRel = Array(21) { i ->
            floatArrayOf(
                pts[i][0] - wrist[0],
                pts[i][1] - wrist[1],
                pts[i][2] - wrist[2]
            )
        }

        // dists = distances from knuckles [5, 9, 13, 17] to wrist
        val knuckleIndices = intArrayOf(5, 9, 13, 17)
        val dists = knuckleIndices.map { i ->
            val dx = pts[i][0] - wrist[0]
            val dy = pts[i][1] - wrist[1]
            val dz = pts[i][2] - wrist[2]
            sqrt(dx * dx + dy * dy + dz * dz)
        }

        // hand_size = mean(dists) + epsilon
        val handSize = (dists.sum() / dists.size) + 1e-6f

        // Normalize by hand_size
        for (i in ptsRel.indices) {
            ptsRel[i][0] /= handSize
            ptsRel[i][1] /= handSize
            ptsRel[i][2] /= handSize
        }

        // Flatten to 63 features
        return FloatArray(63) { idx ->
            val point = idx / 3
            val coord = idx % 3
            ptsRel[point][coord]
        }
    }

    // --- Port of motion_score ---
    fun motionScore(prevFeats: FloatArray?, currFeats: FloatArray?): Float {
        if (prevFeats == null || currFeats == null) return 0f
        var sum = 0f
        for (i in prevFeats.indices) {
            val diff = currFeats[i] - prevFeats[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    // --- Static classification (single frame) ---
    fun classifyStatic(features: FloatArray): Pair<String, Float>? {
        val interpreter = staticInterpreter ?: return null

        val inputBuffer = ByteBuffer.allocateDirect(FEATURE_DIM * 4)
            .order(ByteOrder.nativeOrder())
        features.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        val numClasses = staticLabels.size
        val output = Array(1) { FloatArray(numClasses) }

        interpreter.run(inputBuffer, output)

        val probabilities = output[0]
        val maxIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: return null
        val confidence = probabilities[maxIdx]
        val label = staticLabels[maxIdx] ?: return null

        return if (confidence >= CONFIDENCE_THRESHOLD) Pair(label, confidence) else null
    }

    // --- Sequential classification (20 frames concatenated) ---
    fun classifySequence(sequence: FloatArray): Pair<String, Float>? {
        val interpreter = seqInterpreter ?: return null

        val inputDim = SEQ_LEN * FEATURE_DIM // 1260
        if (sequence.size != inputDim) return null

        val inputBuffer = ByteBuffer.allocateDirect(inputDim * 4)
            .order(ByteOrder.nativeOrder())
        sequence.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        val numClasses = seqLabels.size
        val output = Array(1) { FloatArray(numClasses) }

        interpreter.run(inputBuffer, output)

        val probabilities = output[0]
        val maxIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: return null
        val confidence = probabilities[maxIdx]
        val label = seqLabels[maxIdx] ?: return null

        return if (confidence >= 0.7f) Pair(label, confidence) else null
    }

    // --- Helpers ---
    private fun loadModel(assetPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(assetPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    private fun loadLabels(assetPath: String): Map<Int, String> {
        val json = context.assets.open(assetPath).bufferedReader().readText()
        val jsonObj = JSONObject(json)
        val map = mutableMapOf<Int, String>()
        jsonObj.keys().forEach { key ->
            map[key.toInt()] = jsonObj.getString(key)
        }
        return map
    }
}
