package com.ucb.app.totem.presentation.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.ucb.app.totem.domain.model.Career
import com.ucb.app.totem.presentation.theme.TotemColors

@Composable
fun CareerDetailScreen(
    career: Career,
    onExploreAnother: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(career) { visible = true }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TotemColors.DarkNavy)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onInteraction
            )
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Top Badge ---
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }
        ) {
            TopLetterBadge(career = career)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Career Card ---
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, delayMillis = 200)) +
                    slideInVertically(tween(500, delayMillis = 200)) { 60 }
        ) {
            CareerCard(career = career)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- Explore Another Button ---
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400, delayMillis = 500)) +
                    scaleIn(tween(400, delayMillis = 500), initialScale = 0.8f)
        ) {
            ExploreAnotherButton(onClick = onExploreAnother)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- Top Badge with Letter ---
@Composable
private fun TopLetterBadge(career: Career) {
    val gradientStart = Color(career.gradientColors.first)
    val gradientEnd = Color(career.gradientColors.second)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Letter circle
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = career.letter,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TotemColors.StatusActive, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LETRA DETECTADA",
                    color = TotemColors.StatusActive,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = career.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- Career Info Card ---
@Composable
private fun CareerCard(career: Career) {
    val gradientStart = Color(career.gradientColors.first)
    val gradientEnd = Color(career.gradientColors.second)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TotemColors.CardBackground)
            .border(
                width = 1.dp,
                color = TotemColors.CardBorder,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        // Gradient header area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Overlay with faculty name
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = career.faculty.uppercase(),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎓",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = career.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Card content
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Description
            Text(
                text = career.description,
                color = TotemColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tags row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                career.tags.forEach { tag ->
                    TagChip(text = tag, gradientEnd = gradientEnd)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Duration info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "⏱", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Duración: ${career.duration}",
                    color = TotemColors.TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📚", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = career.faculty,
                    color = TotemColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// --- Tag Chip ---
@Composable
private fun TagChip(text: String, gradientEnd: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(gradientEnd.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = gradientEnd.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = gradientEnd,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// --- Explore Another Button ---
@Composable
private fun ExploreAnotherButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        TotemColors.ButtonBlue,
                        TotemColors.ButtonPurple
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
            text = "🔍  Explorar otra carrera",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
