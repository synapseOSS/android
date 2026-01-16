package com.synapse.social.studioasinc.core.media.processing

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Test with a newer SDK
class ImageCompressorTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var contentResolver: ContentResolver

    private lateinit var imageCompressor: ImageCompressor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(context.cacheDir).thenReturn(File("."))
        imageCompressor = ImageCompressor(context)
    }

    @Test
    fun `calculateInSampleSize calculates correct sample size for various dimensions`() {
        // Mock BitmapFactory.Options
        val options = BitmapFactory.Options()

        // 1. Small image (smaller than target 1920x1080)
        options.outWidth = 1000
        options.outHeight = 500
        assertEquals(1, imageCompressor.calculateInSampleSize(options, 1920, 1080))

        // 2. Exact match
        options.outWidth = 1920
        options.outHeight = 1080
        assertEquals(1, imageCompressor.calculateInSampleSize(options, 1920, 1080))

        // 3. 2x larger
        options.outWidth = 3840
        options.outHeight = 2160
        assertEquals(2, imageCompressor.calculateInSampleSize(options, 1920, 1080))

        // 4. 4x larger (4k)
        // 3840 * 2 = 7680 (8K width).
        // 2160 * 2 = 4320 (8K height).
        // reqWidth = 1920, reqHeight = 1080
        // halfWidth = 3840, halfHeight = 2160

        // Iteration 1: sampleSize = 1.
        // 3840/1 = 3840 >= 1920 (True). 2160/1 = 2160 >= 1080 (True).
        // sampleSize becomes 2.

        // Iteration 2: sampleSize = 2.
        // 3840/2 = 1920 >= 1920 (True). 2160/2 = 1080 >= 1080 (True).
        // sampleSize becomes 4.

        // Iteration 3: sampleSize = 4.
        // 3840/4 = 960 >= 1920 (False).
        // Loop terminates.

        // Result from loop: 4.

        // Check "Very large image" logic.
        // effectiveWidth = 7680 / 4 = 1920.
        // effectiveHeight = 4320 / 4 = 1080.
        // 1920 > 4096 (False).

        // So final result is 4.
        options.outWidth = 3840 * 2
        options.outHeight = 2160 * 2
        assertEquals(4, imageCompressor.calculateInSampleSize(options, 1920, 1080))

        // 5. Very large image (>4096) - should trigger the extra check
        // Let's try something that requires bumping sample size.
        // reqWidth = 4000, reqHeight = 4000.
        // input = 10000 x 10000.
        // half = 5000.

        // Loop:
        // 5000 / 1 >= 4000 -> sampleSize = 2.
        // 5000 / 2 = 2500 < 4000 -> Loop terminates.

        // Result from loop: 2.

        // Check "Very large image" logic.
        // effectiveWidth = 10000 / 2 = 5000.
        // 5000 > 4096 -> sampleSize becomes 4.
        // effectiveWidth = 10000 / 4 = 2500.
        // 2500 > 4096 (False).

        // Final result: 4.
        options.outWidth = 10000
        options.outHeight = 10000
        assertEquals(4, imageCompressor.calculateInSampleSize(options, 4000, 4000))
    }
}
