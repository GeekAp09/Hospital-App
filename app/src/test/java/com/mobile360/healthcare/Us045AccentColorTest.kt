package com.mobile360.healthcare

import com.mobile360.healthcare.data.AccountRepository
import com.mobile360.healthcare.testutil.FakeAuthRepository
import com.mobile360.healthcare.testutil.MainDispatcherRule
import com.mobile360.healthcare.testutil.StubAccount
import com.mobile360.healthcare.testutil.runTestUnconfined
import com.mobile360.healthcare.ui.screens.SettingsViewModel
import com.mobile360.healthcare.ui.theme.AccentChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Tests: CR-007 (accent colour choice). */
class Us045AccentColorTest {
    @get:Rule val main = MainDispatcherRule()

    private class AccentAccount(val store: MutableStateFlow<String> = MutableStateFlow("default")) : AccountRepository by StubAccount() {
        var fail = false
        override val accent: Flow<String> = store
        override suspend fun setAccent(choice: String) {
            if (fail) throw IllegalStateException("disk full")
            store.value = choice
        }
    }

    @Test
    fun test_cr_007_default_on_fresh_install() = runTestUnconfined {
        val vm = SettingsViewModel(FakeAuthRepository(), AccentAccount())
        assertEquals("default", vm.accent.value)
    }

    @Test
    fun test_cr_007_choice_is_saved_and_persists() = runTestUnconfined {
        val account = AccentAccount()
        SettingsViewModel(FakeAuthRepository(), account).setAccent("purple")
        assertEquals("purple", SettingsViewModel(FakeAuthRepository(), account).accent.value)
        assertEquals("Purple", AccentChoice.fromId("purple").label)
    }

    @Test
    fun test_cr_007_failed_save_keeps_choice() = runTestUnconfined {
        val account = AccentAccount().apply { fail = true }
        val vm = SettingsViewModel(FakeAuthRepository(), account)
        vm.setAccent("teal")
        assertEquals("default", vm.accent.value)
    }
}
