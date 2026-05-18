package com.ucb.app.totem.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucb.app.totem.domain.repository.CareerRepository
import com.ucb.app.totem.presentation.state.TotemUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TotemViewModel(
    private val careerRepository: CareerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TotemUiState>(TotemUiState.Idle)
    val uiState: StateFlow<TotemUiState> = _uiState.asStateFlow()

    private var inactivityJob: Job? = null

    companion object {
        private const val INACTIVITY_TIMEOUT_MS = 30_000L // 30 seconds
        private const val DETECTION_ANIMATION_MS = 1_500L // 1.5 seconds for detecting animation
    }

    fun getAllLetters(): List<String> = careerRepository.getAllLetters()

    /**
     * Simulates a letter detection from the UI buttons.
     * Shows a brief "Detecting" animation before revealing the career.
     */
    fun simulateDetection(letter: String) {
        viewModelScope.launch {
            cancelInactivityTimer()

            _uiState.value = TotemUiState.Detecting(letter)

            delay(DETECTION_ANIMATION_MS)

            val career = careerRepository.getCareerByLetter(letter)
            if (career != null) {
                _uiState.value = TotemUiState.Success(career)
                startInactivityTimer()
            } else {
                _uiState.value = TotemUiState.Idle
            }
        }
    }

    /**
     * Called when a real detection happens from the camera/ML pipeline.
     */
    fun onRealDetection(letter: String) {
        simulateDetection(letter)
    }

    /**
     * Resets back to the main camera screen.
     */
    fun resetToMain() {
        cancelInactivityTimer()
        _uiState.value = TotemUiState.Idle
    }

    /**
     * Resets the inactivity timer (called on user interaction in detail screen).
     */
    fun onUserInteraction() {
        if (_uiState.value is TotemUiState.Success) {
            cancelInactivityTimer()
            startInactivityTimer()
        }
    }

    private fun startInactivityTimer() {
        inactivityJob = viewModelScope.launch {
            delay(INACTIVITY_TIMEOUT_MS)
            _uiState.value = TotemUiState.Idle
        }
    }

    private fun cancelInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = null
    }
}
