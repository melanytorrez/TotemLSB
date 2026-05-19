package com.ucb.app

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ucb.app.totem.presentation.screen.CareerDetailScreen
import com.ucb.app.totem.presentation.screen.MainScreen
import com.ucb.app.totem.presentation.state.TotemUiState
import com.ucb.app.totem.presentation.theme.TotemColors
import com.ucb.app.totem.presentation.viewmodel.TotemViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    cameraContent: (@Composable (Modifier, (String) -> Unit) -> Unit)? = null
) {
    val viewModel: TotemViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    val darkColors = darkColorScheme(
        background = TotemColors.DarkNavy,
        surface = TotemColors.CardBackground,
        primary = TotemColors.CyanNeon,
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(colorScheme = darkColors) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                when (targetState) {
                    is TotemUiState.Success -> {
                        (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 3 })
                            .togetherWith(fadeOut(tween(300)))
                    }
                    else -> {
                        (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { -it / 3 })
                            .togetherWith(fadeOut(tween(300)))
                    }
                }
            },
            label = "screenTransition",
            modifier = Modifier
                .fillMaxSize()
                .background(TotemColors.DarkNavy)
        ) { state ->
            when (state) {
                is TotemUiState.Idle, is TotemUiState.Detecting -> {
                    MainScreen(
                        uiState = state,
                        letters = viewModel.getAllLetters(),
                        onLetterClick = { viewModel.simulateDetection(it) },
                        cameraContent = if (cameraContent != null) {
                            { mod ->
                                cameraContent(mod) { letter ->
                                    viewModel.onRealDetection(letter)
                                }
                            }
                        } else null
                    )
                }
                is TotemUiState.Success -> {
                    CareerDetailScreen(
                        career = state.career,
                        onExploreAnother = { viewModel.resetToMain() },
                        onInteraction = { viewModel.onUserInteraction() }
                    )
                }
            }
        }
    }
}