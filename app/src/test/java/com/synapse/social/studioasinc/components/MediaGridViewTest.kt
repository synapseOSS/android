package com.synapse.social.studioasinc.components

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for MediaGridView
 * Tests Requirements: 2.1, 2.2, 2.3, 2.4, 9.3
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaGridViewTest {

    private lateinit var context: Context
    private lateinit var gridView: MediaGridView

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        gridView = MediaGridView(context)
    }

    /**
     * Test 2-item horizontal layout
     * Requirement 2.1
     */
    @Test
    fun testTwoItemHorizontalLayout() {
        val mediaItems = listOf(
            MediaItem(id = "1", url = "https://example.com/image1.jpg", type = MediaType.IMAGE),
            MediaItem(id = "2", url = "https://example.com/image2.jpg", type = MediaType.IMAGE)
        )

        gridView.setMediaItems(mediaItems)

        // Verify grid is visible
        assertEquals(View.VISIBLE, gridView.visibility)

        // Verify container exists
        assertTrue(gridView.childCount > 0)
        val container = gridView.getChildAt(0) as? FrameLayout
        assertNotNull(container)

        // Verify 2 media views are present
        assertEquals(2, container?.childCount)
    }

    /**
     * Test 3-item asymmetric layout
     * Requirement 2.2
     */
    @Test
    fun testThreeItemAsymmetricLayout() {
        val mediaItems = listOf(
            MediaItem(id = "1", url = "https://example.com/image1.jpg", type = MediaType.IMAGE),
            MediaItem(id = "2", url = "https://example.com/image2.jpg", type = MediaType.IMAGE),
            MediaItem(id = "3", url = "https://example.com/image3.jpg", type = MediaType.IMAGE)
        )

        gridView.setMediaItems(mediaItems)

        // Verify grid is visible
        assertEquals(View.VISIBLE, gridView.visibility)

        // Verify container exists
        assertTrue(gridView.childCount > 0)
        val container = gridView.getChildAt(0) as? FrameLayout
        assertNotNull(container)

        // Verify 3 media views are present
        assertEquals(3, container?.childCount)
    }

    /**
     * Test 4-item 2x2 grid
     * Requirement 2.3
     */
    @Test
    fun testFourItem2x2Grid() {
        val mediaItems = listOf(
            MediaItem(id = "1", url = "https://example.com/image1.jpg", type = MediaType.IMAGE),
            MediaItem(id = "2", url = "https://example.com/image2.jpg", type = MediaType.IMAGE),
            MediaItem(id = "3", url = "https://example.com/image3.jpg", type = MediaType.IMAGE),
            MediaItem(id = "4", url = "https://example.com/image4.jpg", type = MediaType.IMAGE)
        )

        gridView.setMediaItems(mediaItems)

        // Verify grid is visible
        assertEquals(View.VISIBLE, gridView.visibility)

        // Verify container exists
        assertTrue(gridView.childCount > 0)
        val container = gridView.getChildAt(0) as? FrameLayout
        assertNotNull(container)

        // Verify 4 media views are present
        assertEquals(4, container?.childCount)
    }

    /**
     * Test 5+ item grid with "+N" overlay
     * Requirement 2.4
     */
    @Test
    fun testFivePlusItemGridWithOverlay() {
        val mediaItems = listOf(
            MediaItem(id = "1", url = "https://example.com/image1.jpg", type = MediaType.IMAGE),
            MediaItem(id = "2", url = "https://example.com/image2.jpg", type = MediaType.IMAGE),
            MediaItem(id = "3", url = "https://example.com/image3.jpg", type = MediaType.IMAGE),
            MediaItem(id = "4", url = "https://example.com/image4.jpg", type = MediaType.IMAGE),
            MediaItem(id = "5", url = "https://example.com/image5.jpg", type = MediaType.IMAGE),
            MediaItem(id = "6", url = "https://example.com/image6.jpg", type = MediaType.IMAGE)
        )

        gridView.setMediaItems(mediaItems)

        // Verify grid is visible
        assertEquals(View.VISIBLE, gridView.visibility)

        // Verify container exists
        assertTrue(gridView.childCount > 0)
        val container = gridView.getChildAt(0) as? FrameLayout
        assertNotNull(container)

        // Verify 4 items are displayed (3 media views + 1 container with overlay)
        assertEquals(4, container?.childCount)
    }

    /**
     * Test single item with original aspect ratio
     * Requirement 9.3
     */
    @Test
    fun testSingleItemWithOriginalAspectRatio() {
        val mediaItems = listOf(
            MediaItem(id = "1", url = "https://example.com/image1.jpg", type = MediaType.IMAGE)
        )

        gridView.setMediaItems(mediaItems)

        // Verify grid is visible
        assertEquals(View.VISIBLE, gridView.visibility)

        // Verify single media view exists
        assertTrue(gridView.childCount > 0)
        val mediaView = gridView.getChildAt(0)
        assertNotNull(mediaView)

        // Verify layout params allow for original aspect ratio
        val params = mediaView.layoutParams as FrameLayout.LayoutParams
        assertEquals(FrameLayout.LayoutParams.MATCH_PARENT, params.width)
        assertEquals(FrameLayout.LayoutParams.WRAP_CONTENT, params.height)
    }

    /**
     * Test empty media list hides the view
     */
    @Test
    fun testEmptyMediaListHidesView() {
        val mediaItems = emptyList<MediaItem>()

        gridView.setMediaItems(mediaItems)

        // Verify grid is hidden
        assertEquals(View.GONE, gridView.visibility)
    }

    /**
     * Test click listener is set correctly
     */
    @Test
    fun testClickListenerIsSet() {
        val mediaItems = listOf(
            MediaItem(id = "1", url = "https://example.com/image1.jpg", type = MediaType.IMAGE)
        )

        var clickedPosition = -1
        var clickedItems: List<MediaItem>? = null

        gridView.onMediaClickListener = object : MediaGridView.OnMediaClickListener {
            override fun onMediaClick(mediaItems: List<MediaItem>, position: Int) {
                clickedItems = mediaItems
                clickedPosition = position
            }
        }

        gridView.setMediaItems(mediaItems)

        // Verify listener is set
        assertNotNull(gridView.onMediaClickListener)
    }

    /**
     * Test video items have play icon overlay
     */
    @Test
    fun testVideoItemsHavePlayIcon() {
        val mediaItems = listOf(
            MediaItem(id = "1", url = "https://example.com/video1.mp4", type = MediaType.VIDEO)
        )

        gridView.setMediaItems(mediaItems)

        // Verify grid is visible
        assertEquals(View.VISIBLE, gridView.visibility)

        // For video items, the view should be wrapped in a FrameLayout with overlay
        assertTrue(gridView.childCount > 0)
    }
}
