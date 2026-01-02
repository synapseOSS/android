package com.synapse.social.studioasinc.di

import android.content.Context
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.local.ChatDao
import com.synapse.social.studioasinc.data.local.PostDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    fun providePostDao(database: AppDatabase): PostDao {
        return database.postDao()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): com.synapse.social.studioasinc.data.local.UserDao {
        return database.userDao()
    }
}
