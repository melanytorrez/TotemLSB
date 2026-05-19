package com.ucb.app.totem.camera

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.ucb.app.totem.presentation.theme.TotemColors
import java.util.concurrent.Executors

/**
 * A Composable that encapsulates the entire camera + ML pipeline:
 * CameraX → MediaPipe HandLandmarker → TFLite Classification → Callback.
 */
@Composable
fun CameraPreview(
    onLetterDetected: (letter: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // State for current detection feedback
    var detectedLetter by remember { mutableStateOf<String?>(null) }
    var detectedConfidence by remember { mutableFloatStateOf(0f) }
    var handDetected by remember { mutableStateOf(false) }
    var handLandmarks by remember { mutableStateOf<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>?>(null) }

    if (hasCameraPermission) {
        Box(modifier = modifier.clip(RoundedCornerShape(16.dp))) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    // Initialize ML pipeline
                    val signClassifier = SignClassifier(ctx)
                    signClassifier.initialize()

                    val recognitionEngine = SignRecognitionEngine(
                        classifier = signClassifier,
                        onLetterDetected = { letter, confidence, _ ->
                            detectedLetter = letter
                            detectedConfidence = confidence
                            onLetterDetected(letter)
                        },
                        onHandPresenceChanged = { detected ->
                            handDetected = detected
                            if (!detected) {
                                detectedLetter = null
                                handLandmarks = null
                            }
                        }
                    )

                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

                    val handLandmarkerHelper = HandLandmarkerHelper(
                        context = ctx,
                        onResults = { result, image ->
                            recognitionEngine.processResult(result, image)
                            mainHandler.post {
                                handLandmarks = if (result.landmarks().isNotEmpty()) {
                                    result.landmarks()[0]
                                } else {
                                    null
                                }
                            }
                        },
                        onError = { e ->
                            Log.e("CameraPreview", "MediaPipe error", e)
                        }
                    )
                    handLandmarkerHelper.initialize()

                    // CameraX setup
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val executor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also { it.surfaceProvider = previewView.surfaceProvider }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(640, 480))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(executor) { imageProxy ->
                                    val bitmap = imageProxy.toBitmap()
                                    val mpImage = BitmapImageBuilder(bitmap).build()

                                    handLandmarkerHelper.detectAsync(
                                        mpImage,
                                        imageProxy.imageInfo.timestamp / 1_000_000
                                    )

                                    imageProxy.close()
                                }
                            }

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("CameraPreview", "CameraX binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Canvas overlay to draw hand landmarks and connections
            handLandmarks?.let { landmarks ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw bones (connections)
                    HAND_CONNECTIONS.forEach { connection ->
                        val startIdx = connection.first
                        val endIdx = connection.second

                        if (startIdx < landmarks.size && endIdx < landmarks.size) {
                            val start = landmarks[startIdx]
                            val end = landmarks[endIdx]

                            val startPt = Offset(
                                x = (1f - start.x()) * size.width,
                                y = start.y() * size.height
                            )
                            val endPt = Offset(
                                x = (1f - end.x()) * size.width,
                                y = end.y() * size.height
                            )

                            drawLine(
                                color = TotemColors.CyanNeon.copy(alpha = 0.8f),
                                start = startPt,
                                end = endPt,
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }

                    // Draw joints (knuckles/fingertips)
                    landmarks.forEach { landmark ->
                        val centerPt = Offset(
                            x = (1f - landmark.x()) * size.width,
                            y = landmark.y() * size.height
                        )

                        // Outer cyan glowing ring
                        drawCircle(
                            color = TotemColors.CyanNeon.copy(alpha = 0.4f),
                            radius = 6.dp.toPx(),
                            center = centerPt
                        )
                        // Inner solid white circle
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = centerPt
                        )
                    }
                }
            }

            // Overlay: Status indicator
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Camera active indicator
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(TotemColors.StatusActive, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "CÁMARA ACTIVA",
                        color = TotemColors.StatusActive,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bottom: Detection result
                AnimatedVisibility(
                    visible = detectedLetter != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                TotemColors.CyanNeon,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Detectado: ${detectedLetter?.uppercase()} (${String.format("%.0f", detectedConfidence * 100)}%)",
                            color = TotemColors.CyanNeon,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Border glow when hand is detected
            val infiniteTransition = rememberInfiniteTransition(label = "glow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )

            if (handDetected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            3.dp,
                            TotemColors.CyanNeon.copy(alpha = glowAlpha),
                            RoundedCornerShape(16.dp)
                        )
                )
            }
        }
    } else {
        // Permission not granted — show placeholder
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(TotemColors.CardBackground)
                .border(2.dp, TotemColors.CardBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📷", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Permiso de cámara requerido",
                    color = TotemColors.TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private val HAND_CONNECTIONS = listOf(
    Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4), // Thumb
    Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8), // Index
    Pair(0, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12), // Middle
    Pair(0, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16), // Ring
    Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20), // Pinky
    Pair(5, 9), Pair(9, 13), Pair(13, 17) // Palm knuckles
)
