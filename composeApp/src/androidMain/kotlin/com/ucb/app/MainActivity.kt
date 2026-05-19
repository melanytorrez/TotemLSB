package com.ucb.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ucb.app.totem.camera.CameraPreview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                cameraContent = { modifier, onLetterDetected ->
                    CameraPreview(
                        onLetterDetected = onLetterDetected,
                        modifier = modifier
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}