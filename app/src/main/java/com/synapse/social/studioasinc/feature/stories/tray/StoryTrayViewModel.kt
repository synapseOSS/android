package com.synapse.social.studioasinc.feature.stories.tray

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.network.SupabaseClient
import com.synapse.social.studioasinc.data.repository.StoryRepository
import com.synapse.social.studioasinc.domain.model.StoryTrayState
import com.synapse.social.studioasinc.domain.model.StoryWithUser
import com.synapse.social.studioasinc.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

@HiltViewModel
class StoryTrayViewModel @Inject constructor(
    private val storyRepository: StoryRepository
) : ViewModel() {
    
    private val _storyTrayState = MutableStateFlow(StoryTrayState())
    val storyTrayState: StateFlow<StoryTrayState> = _storyTrayState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val currentUserId: String?
        get() = SupabaseClient.client.auth.currentUserOrNull()?.id
    
    init {
        loadCurrentUser()
        loadStories()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            try {
                val result = SupabaseClient.client.from("users")
                    .select(columns = Columns.raw("*")) {
                        filter {
                            eq("uid", userId)
                        }
                    }
                    .decodeSingleOrNull<JsonObject>()
                
                result?.let { json ->
                    _currentUser.value = User(
                        id = json["id"]?.jsonPrimitive?.content,
                        uid = json["uid"]?.jsonPrimitive?.content ?: userId,
                        username = json["username"]?.jsonPrimitive?.content,
                        displayName = json["display_name"]?.jsonPrimitive?.content,
                        avatar = json["avatar"]?.jsonPrimitive?.content,
                        verify = json["verify"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun loadStories() {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            _storyTrayState.update { it.copy(isLoading = true, error = null) }
            
            storyRepository.getActiveStories(userId)
                .catch { e ->
                    _storyTrayState.update { 
                        it.copy(isLoading = false, error = e.message) 
                    }
                }
                .collect { stories ->
                    val myStory = stories.find { it.user.uid == userId }
                    val friendStories = stories.filter { it.user.uid != userId }
                    
                    _storyTrayState.update {
                        StoryTrayState(
                            myStory = myStory,
                            friendStories = friendStories,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }
    
    fun refresh() {
        loadStories()
    }
    
    /**
     * Mark a story as seen when the user views it
     */
    fun markStoryAsSeen(storyId: String) {
        val viewerId = currentUserId ?: return
        
        viewModelScope.launch {
            storyRepository.markAsSeen(storyId, viewerId)
        }
    }
    
    /**
     * Update the seen status of a story collection after viewing
     */
    fun markStoriesAsSeen(storyWithUser: StoryWithUser) {
        _storyTrayState.update { state ->
            val updatedFriends = state.friendStories.map { story ->
                if (story.user.uid == storyWithUser.user.uid) {
                    story.copy(hasUnseenStories = false)
                } else {
                    story
                }
            }
            state.copy(friendStories = updatedFriends)
        }
    }
}
