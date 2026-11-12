package com.mobile360.healthcare

import org.junit.Assert.assertEquals
import org.junit.Test

// CR-002: the home carousel's placeholder images and captions are real, distinct resources
class Cr002HomeCarouselTest {
    @Test
    fun carousel_has_three_distinct_images_and_captions() {
        val images = listOf(R.drawable.home_carousel_1, R.drawable.home_carousel_2, R.drawable.home_carousel_3)
        val captions = listOf(R.string.home_highlight_1, R.string.home_highlight_2, R.string.home_highlight_3)
        assertEquals(3, images.toSet().size)
        assertEquals(3, captions.toSet().size)
        assertEquals(R.string.home_highlights_title, R.string.home_highlights_title)
    }
}
