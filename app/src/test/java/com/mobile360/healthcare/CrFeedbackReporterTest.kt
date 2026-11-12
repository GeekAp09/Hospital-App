package com.mobile360.healthcare

import com.mobile360.healthcare.data.FeedbackRepository
import com.mobile360.healthcare.data.local.FeedbackEntity
import com.mobile360.healthcare.data.model.Outcome
import com.mobile360.healthcare.testutil.BoltMainRule
import com.mobile360.healthcare.testutil.failOutcome
import com.mobile360.healthcare.testutil.okOutcome
import com.mobile360.healthcare.ui.screens.FeedbackViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// CR-001: feedback & bug reporter
private class FakeFeedback : FeedbackRepository {
    val stored = MutableStateFlow<List<FeedbackEntity>>(emptyList())
    var calls = 0
    var next: Outcome? = null
    var throwOnSubmit = false

    override fun reports(): Flow<List<FeedbackEntity>> = stored

    override suspend fun submit(kind: String, text: String): Outcome {
        calls++
        if (throwOnSubmit) error("disk full")
        next?.let { return it }
        stored.value = listOf(FeedbackEntity("f$calls", "u1", kind, text.trim(), calls.toLong())) + stored.value
        return okOutcome("saved")
    }
}

class CrFeedbackReporterTest {
    @get:Rule val main = BoltMainRule()

    @Test
    fun test_cr_001_non_blank_submit_stores_report_and_closes_dialog() {
        val repo = FakeFeedback()
        val vm = FeedbackViewModel(repo)
        vm.openDialog()
        vm.submit("Bug", "  Chat screen freezes  ")
        assertEquals(1, repo.calls)
        assertEquals("Chat screen freezes", repo.stored.value.single().message)
        assertEquals("Bug", repo.stored.value.single().kind)
        assertFalse(vm.dialogOpen.value)
    }

    @Test
    fun test_cr_001_blank_submit_is_rejected_and_nothing_is_stored() {
        val repo = FakeFeedback()
        val vm = FeedbackViewModel(repo)
        vm.openDialog()
        vm.submit("Feedback", "   ")
        assertEquals(FeedbackViewModel.BLANK_MESSAGE, vm.error.value)
        assertEquals(0, repo.calls)
        assertTrue(vm.dialogOpen.value)
    }

    @Test
    fun test_cr_001_failed_save_keeps_dialog_open_with_error() {
        val repo = FakeFeedback().apply { throwOnSubmit = true }
        val vm = FeedbackViewModel(repo)
        vm.openDialog()
        vm.submit("Bug", "text")
        assertEquals(FeedbackViewModel.SAVE_FAILED, vm.error.value)
        assertTrue(vm.dialogOpen.value)
    }

    @Test
    fun test_cr_001_refused_save_reports_repository_message() {
        val repo = FakeFeedback().apply { next = failOutcome("nope") }
        val vm = FeedbackViewModel(repo)
        vm.openDialog()
        vm.submit("Bug", "text")
        assertEquals("nope", vm.error.value)
        assertTrue(vm.dialogOpen.value)
    }

    @Test
    fun test_cr_001_cancel_closes_dialog_without_saving() {
        val repo = FakeFeedback()
        val vm = FeedbackViewModel(repo)
        vm.openDialog()
        vm.closeDialog()
        assertFalse(vm.dialogOpen.value)
        assertEquals(0, repo.calls)
        assertTrue(repo.stored.value.isEmpty())
    }
}
