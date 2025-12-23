package com.synapse.social.studioasinc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseClientTest {

    @Test
    fun `ConfigurationException is defined correctly`() {
        val message = "Test exception"
        val cause = RuntimeException("Cause")
        val exception = ConfigurationException(message, cause)

        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is RuntimeException)
    }
}
