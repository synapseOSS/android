package com.synapse.social.studioasinc

import android.content.Context
import com.synapse.social.studioasinc.data.local.AppSettingsManager
import org.junit.Test
import org.mockito.Mockito

class MediaStorageServiceCompilationTest {

    @Test
    fun `test class instantiation`() {
        val context = Mockito.mock(Context::class.java)
        val appSettingsManager = Mockito.mock(AppSettingsManager::class.java)
        val service = MediaStorageService(context, appSettingsManager)

        // Just verify the object is created, which implies the file compiled successfully.
        assert(service != null)
    }
}
