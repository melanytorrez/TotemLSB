package com.ucb.app.totem.domain.model

data class Career(
    val letter: String,
    val name: String,
    val faculty: String,
    val description: String,
    val tags: List<String>,
    val duration: String,
    val gradientColors: Pair<Long, Long> // Start and end colors for the card gradient
)
