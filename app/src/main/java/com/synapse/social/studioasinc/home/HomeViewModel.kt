package com.synapse.social.studioasinc.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.local.AppDatabase
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.util.ScrollPositionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// TODO: Ensure polls display correctly in Home Feed - verify PagingData flow and PostEventBus sync
@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val postRepository: PostRepository
) : AndroidViewModel(application) {
    private val authRepository: AuthRepository = AuthRepository()

    val posts: Flow<PagingData<Post>> = postRepository.getPostsPaged()
        .cachedIn(viewModelScope)

    private var savedScrollPosition: ScrollPositionState? = null
    
    /**
     * Save scroll position for restoration
     * Called when navigating away from the feed
     * 
     * @param position The scroll position (item index)
     * @param offset The offset within the item
     */
    fun saveScrollPosition(position: Int, offset: Int) {
        savedScrollPosition = ScrollPositionState(position, offset)
    }
    
    /**
     * Restore scroll position if not expired
     * Called when returning to the feed
     * 
     * @return ScrollPositionState if valid and not expired, null otherwise
     */
    fun restoreScrollPosition(): ScrollPositionState? {
        val position = savedScrollPosition
        
        // Check if position exists and is not expired (5 minutes)
        return if (position != null && !position.isExpired()) {
            position
        } else {
            // Clear expired position
            savedScrollPosition = null
            null
        }
    }
}
