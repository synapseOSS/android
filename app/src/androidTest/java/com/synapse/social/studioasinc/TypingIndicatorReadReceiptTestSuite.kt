package com.synapse.social.studioasinc

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for Typing Indicators and Read Receipts integration tests
 * 
 * This suite runs all integration and end-to-end tests for the typing indicators
 * and read receipts feature. It covers:
 * 
 * 1. Typing indicator functionality across devices
 * 2. Read receipt state transitions and real-time updates
 * 3. Error scenarios and graceful degradation
 * 
 * Requirements covered: 1.1-1.5, 2.1-2.5, 3.1-3.5, 4.1-4.5, 5.1-5.5, 6.1-6.5
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    TypingIndicatorIntegrationTest::class,
    ReadReceiptIntegrationTest::class,
    ErrorScenarioIntegrationTest::class
)
class TypingIndicatorReadReceiptTestSuite
