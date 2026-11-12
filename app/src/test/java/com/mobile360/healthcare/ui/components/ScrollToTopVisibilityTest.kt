package com.mobile360.healthcare.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// CR-005: the button shows only once the list is scrolled past its first item
class ScrollToTopVisibilityTest {
    @Test
    fun hiddenAtTop() = assertFalse(showScrollToTop(0))

    @Test
    fun shownWhenScrolled() = assertTrue(showScrollToTop(3))
}
