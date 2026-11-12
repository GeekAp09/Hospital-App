package com.mobile360.healthcare

import com.mobile360.healthcare.testutil.MainDispatcherRule
import com.mobile360.healthcare.testutil.StubAccount
import com.mobile360.healthcare.testutil.StubAuth
import com.mobile360.healthcare.ui.screens.ConsentState
import com.mobile360.healthcare.ui.screens.ConsentViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// CR-004: consent dialog state
class Cr004ConsentTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun test_cr_004_consent_is_needed_on_a_fresh_install() {
        val vm = ConsentViewModel(StubAccount(), StubAuth())
        assertEquals(ConsentState.Needed, vm.state.value)
    }

    @Test
    fun test_cr_004_accept_persists_for_a_new_view_model() {
        val account = StubAccount()
        ConsentViewModel(account, StubAuth()).accept()
        assertEquals(ConsentState.Done, ConsentViewModel(account, StubAuth()).state.value)
    }

    @Test
    fun test_cr_004_decline_signs_out_and_stores_nothing() {
        val account = StubAccount()
        val auth = StubAuth()
        val vm = ConsentViewModel(account, auth)
        vm.decline()
        assertEquals(1, auth.signOuts)
        assertEquals(ConsentState.Needed, vm.state.value)
    }
}
