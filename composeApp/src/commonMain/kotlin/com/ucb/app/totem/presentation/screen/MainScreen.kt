package com.ucb.app.totem.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ucb.app.totem.presentation.state.TotemUiState
import com.ucb.app.totem.presentation.theme.TotemColors

@Composable
fun MainScreen(
    uiState: TotemUiState,
    letters: List<String>,
    onLetterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TotemColors.DarkNavy)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header ---
        TotemHeader()

        Spacer(modifier = Modifier.height(20.dp))

        // --- Camera Area ---
        CameraPreviewArea(uiState = uiState)

        Spacer(modifier = Modifier.height(16.dp))

        // --- Instruction Text ---
        Text(
            text = "Haz la inicial de una carrera",
            color = TotemColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "para descubrir más sobre ella",
            color = TotemColors.TextCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Separator ---
        Text(
            text = "~ SIMULAR DETECCIÓN ~",
            color = TotemColors.TextMuted,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Letter Buttons Grid ---
        LetterButtonsGrid(
            letters = letters,
            onLetterClick = onLetterClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Footer ---
        Text(
            text = "Detección automática en señas (próximamente)...",
            color = TotemColors.TextMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

// --- Header Component ---
@Composable
private fun TotemHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // University badge
        Text(
            text = "🎓 UNIVERSIDAD CATÓLICA BOLIVIANA",
            color = TotemColors.CyanNeon,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Event name
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            TotemColors.ButtonBlue,
                            TotemColors.ButtonPurple
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Feria de Carreras 2025",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main title
        Text(
            text = "Interactúa con",
            color = TotemColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Lengua de Señas",
            color = TotemColors.CyanNeon,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// --- Camera Preview Area ---
@Composable
private fun CameraPreviewArea(uiState: TotemUiState) {
    val isDetecting = uiState is TotemUiState.Detecting

    // Pulsing animation for detecting state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val borderColor = if (isDetecting) {
        TotemColors.CyanNeon.copy(alpha = pulseAlpha)
    } else {
        TotemColors.CardBorder
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(TotemColors.CardBackground)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Status indicator
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TotemColors.StatusActive, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CÁMARA ACTIVA",
                    color = TotemColors.StatusActive,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Hand icon placeholder
            Text(
                text = "🤚",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = isDetecting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val detectingLetter = (uiState as? TotemUiState.Detecting)?.letter ?: ""
                Text(
                    text = "Detectando '$detectingLetter'...",
                    color = TotemColors.CyanNeon,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = !isDetecting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Haz la seña de",
                        color = TotemColors.TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "una letra (ej: M, I, D)",
                        color = TotemColors.TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// --- Letter Buttons Grid ---
@Composable
private fun LetterButtonsGrid(
    letters: List<String>,
    onLetterClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(letters) { letter ->
            LetterButton(
                letter = letter,
                onClick = { onLetterClick(letter) }
            )
        }
    }
}

@Composable
private fun LetterButton(
    letter: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TotemColors.ButtonBlue,
                        TotemColors.ButtonBlue.copy(alpha = 0.7f)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
