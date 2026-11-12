package com.mobile360.healthcare

import org.junit.Assert.assertEquals
import org.junit.Test

// CR-003: the indicator view model must be a Hilt view model fed by ConnectivityObserver
class Cr003NetworkIndicatorTest {
    @Test
    fun cr_003_network_status_view_model_exposes_online_flow() {
        val getter = NetworkStatusViewModel::class.java.methods.map { it.name }
        assertEquals(true, getter.contains("getOnline"))
    }

    @Test
    fun cr_003_view_model_depends_only_on_connectivity_observer() {
        val params = NetworkStatusViewModel::class.java.constructors.single().parameterTypes.map { it.simpleName }
        assertEquals(listOf("ConnectivityObserver"), params)
    }
}
