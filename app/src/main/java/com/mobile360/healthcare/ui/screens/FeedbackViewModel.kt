package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.FeedbackRepository
import com.mobile360.healthcare.data.local.FeedbackEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// CR-001: feedback & bug reporter state
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val repo: FeedbackRepository,
) : ViewModel() {
    val reports: StateFlow<List<FeedbackEntity>> =
        repo.reports().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _dialogOpen = MutableStateFlow(false)
    val dialogOpen: StateFlow<Boolean> = _dialogOpen

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun openDialog() {
        _error.value = null
        _dialogOpen.value = true
    }

    fun closeDialog() {
        _error.value = null
        _dialogOpen.value = false
    }

    fun submit(kind: String, text: String) {
        if (text.isBlank()) {
            _error.value = BLANK_MESSAGE
            return
        }
        viewModelScope.launch {
            val outcome = try {
                repo.submit(kind, text)
            } catch (e: Exception) {
                null
            }
            if (outcome != null && outcome.ok) {
                closeDialog()
            } else {
                _error.value = if (outcome == null) SAVE_FAILED else outcome.message
            }
        }
    }

    companion object {
        const val BLANK_MESSAGE = "Please describe your feedback or bug."
        const val SAVE_FAILED = "Could not save your report. Try again."
    }
}
