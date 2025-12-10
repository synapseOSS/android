package com.synapse.social.studioasinc.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.data.repository.*
import com.synapse.social.studioasinc.domain.usecase.profile.*
import com.synapse.social.studioasinc.domain.usecase.post.*

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val profileRepository: ProfileRepository = ProfileRepositoryImpl()
            val postDao = AppDatabase.getDatabase(context).postDao()
            val postRepository = PostRepository(postDao)
            val postInteractionRepository = PostInteractionRepository()
            val profileActionRepository = ProfileActionRepository()
            
            return ProfileViewModel(
                getProfileUseCase = GetProfileUseCase(profileRepository),
                getProfileContentUseCase = GetProfileContentUseCase(profileRepository),
                followUserUseCase = FollowUserUseCase(profileRepository),
                unfollowUserUseCase = UnfollowUserUseCase(profileRepository),
                likePostUseCase = LikePostUseCase(postInteractionRepository),
                unlikePostUseCase = UnlikePostUseCase(postInteractionRepository),
                savePostUseCase = SavePostUseCase(postInteractionRepository),
                unsavePostUseCase = UnsavePostUseCase(postInteractionRepository),
                deletePostUseCase = DeletePostUseCase(postInteractionRepository),
                reportPostUseCase = ReportPostUseCase(postInteractionRepository),
                lockProfileUseCase = LockProfileUseCase(profileActionRepository),
                archiveProfileUseCase = ArchiveProfileUseCase(profileActionRepository),
                blockUserUseCase = BlockUserUseCase(profileActionRepository),
                reportUserUseCase = ReportUserUseCase(profileActionRepository),
                muteUserUseCase = MuteUserUseCase(profileActionRepository)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
