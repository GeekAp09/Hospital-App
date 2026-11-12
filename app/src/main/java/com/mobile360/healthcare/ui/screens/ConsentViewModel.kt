package com.mobile360.healthcare.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AccountRepository
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.ui.components.ConsentDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// CR-004: first-visit consent state
enum class ConsentState { Loading, Needed, Done }

@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val account: AccountRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    val state: StateFlow<ConsentState> = account.consentAccepted
        .map { if (it) ConsentState.Done else ConsentState.Needed }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConsentState.Loading)

    fun accept() {
        viewModelScope.launch { account.acceptConsent() }
    }

    fun decline() {
        viewModelScope.launch { auth.signOut() }
    }
}

/** CR-004: blocking consent dialog shown on a home screen until the user decides. */
@Composable
fun ConsentGate(viewModel: ConsentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    if (state == ConsentState.Needed) {
        ConsentDialog(onAccept = viewModel::accept, onDecline = viewModel::decline)
    }
}
