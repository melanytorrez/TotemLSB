package com.ucb.app.totem.presentation.state

import com.ucb.app.totem.domain.model.Career

sealed class TotemUiState {
    data object Idle : TotemUiState()
    data class Detecting(val letter: String) : TotemUiState()
    data class Success(val career: Career) : TotemUiState()
}
