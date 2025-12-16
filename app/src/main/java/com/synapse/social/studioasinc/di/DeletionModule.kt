package com.synapse.social.studioasinc.di

import android.content.Context
import com.synapse.social.studioasinc.data.repository.deletion.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for chat history deletion dependencies
 * Requirements: 1.4, 1.5, 4.4, 6.5
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DeletionModule {
    
    /**
     * Bind ChatHistoryManager interface to implementation
     */
    @Binds
    @Singleton
    abstract fun bindChatHistoryManager(
        chatHistoryManagerImpl: ChatHistoryManagerImpl
    ): ChatHistoryManager
    
    /**
     * Bind DeletionCoordinator interface to implementation
     */
    @Binds
    @Singleton
    abstract fun bindDeletionCoordinator(
        deletionCoordinatorImpl: DeletionCoordinatorImpl
    ): DeletionCoordinator
    
    /**
     * Bind ErrorRecoveryManager interface to implementation
     */
    @Binds
    @Singleton
    abstract fun bindErrorRecoveryManager(
        errorRecoveryManagerImpl: ErrorRecoveryManagerImpl
    ): ErrorRecoveryManager
    
    /**
     * Bind UserNotificationManager interface to implementation
     */
    @Binds
    @Singleton
    abstract fun bindUserNotificationManager(
        userNotificationManagerImpl: UserNotificationManagerImpl
    ): UserNotificationManager
    
    /**
     * Bind LocalChatRepository interface to implementation
     */
    @Binds
    @Singleton
    abstract fun bindLocalChatRepository(
        localChatRepositoryImpl: LocalChatRepositoryImpl
    ): LocalChatRepository
    
    /**
     * Bind RemoteChatRepository interface to implementation
     */
    @Binds
    @Singleton
    abstract fun bindRemoteChatRepository(
        remoteChatRepositoryImpl: RemoteChatRepositoryImpl
    ): RemoteChatRepository
    
    /**
     * Bind ChatCacheManager interface to implementation
     */
    @Binds
    @Singleton
    abstract fun bindChatCacheManager(
        chatCacheManagerImpl: ChatCacheManagerImpl
    ): ChatCacheManager
    
    /**
     * Bind ProgressTracker interface to implementation
     */
    @Binds
    @Singleton
    abstract fun bindProgressTracker(
        progressTrackerImpl: ProgressTrackerImpl
    ): ProgressTracker
    
    companion object {
        
        /**
         * Provide DeletionStatusPersistence instance
         */
        @Provides
        @Singleton
        fun provideDeletionStatusPersistence(
            @ApplicationContext context: Context
        ): DeletionStatusPersistence {
            return DeletionStatusPersistence(context)
        }
        
        /**
         * Provide RetryQueueManager instance
         */
        @Provides
        @Singleton
        fun provideRetryQueueManager(
            @ApplicationContext context: Context,
            remoteChatRepository: RemoteChatRepository
        ): RetryQueueManager {
            return RetryQueueManager(context, remoteChatRepository)
        }
        
        /**
         * Provide ConnectivityMonitor instance
         */
        @Provides
        @Singleton
        fun provideConnectivityMonitor(
            @ApplicationContext context: Context
        ): ConnectivityMonitor {
            return ConnectivityMonitor(context)
        }
        
        /**
         * Provide BatchDeletionManager instance
         */
        @Provides
        @Singleton
        fun provideBatchDeletionManager(
            localChatRepository: LocalChatRepository,
            remoteChatRepository: RemoteChatRepository,
            chatCacheManager: ChatCacheManager,
            progressTracker: ProgressTracker
        ): BatchDeletionManager {
            return BatchDeletionManager(
                localChatRepository,
                remoteChatRepository,
                chatCacheManager,
                progressTracker
            )
        }
        
        /**
         * Provide MemoryEfficientDeletionProcessor instance
         */
        @Provides
        @Singleton
        fun provideMemoryEfficientDeletionProcessor(
            localChatRepository: LocalChatRepository,
            remoteChatRepository: RemoteChatRepository,
            chatCacheManager: ChatCacheManager
        ): MemoryEfficientDeletionProcessor {
            return MemoryEfficientDeletionProcessor(
                localChatRepository,
                remoteChatRepository,
                chatCacheManager
            )
        }
    }
}