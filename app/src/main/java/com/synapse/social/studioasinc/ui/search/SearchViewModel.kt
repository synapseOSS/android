package com.synapse.social.studioasinc.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.SearchRepository
import com.synapse.social.studioasinc.data.repository.SearchRepositoryImpl
import com.synapse.social.studioasinc.model.SearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val active: Boolean = false,
    val isLoading: Boolean = false,
    val selectedFilter: SearchFilter = SearchFilter.ALL,
    val results: List<SearchResult> = emptyList(),
    val error: String? = null,
    val searchHistory: List<String> = emptyList() // Could be implemented later
)

enum class SearchFilter {
    ALL, PEOPLE, POSTS, PHOTOS, VIDEOS
}

class SearchViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotEmpty()) {
                delay(300) // Debounce
                performSearch(query)
            } else {
                _uiState.update { it.copy(results = emptyList()) }
            }
        }
    }

    fun onActiveChange(active: Boolean) {
        _uiState.update { it.copy(active = active) }
    }

    fun onFilterSelect(filter: SearchFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        val currentQuery = uiState.value.query
        if (currentQuery.isNotEmpty()) {
            performSearch(currentQuery)
        }
    }

    fun onSearch(query: String) {
         _uiState.update { it.copy(query = query) }
         performSearch(query)
    }

    fun clearSearch() {
        _uiState.update { it.copy(query = "", results = emptyList()) }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val filter = uiState.value.selectedFilter
                val results = mutableListOf<SearchResult>()

                when (filter) {
                    SearchFilter.ALL -> {
                        val usersDeferred = async { searchRepository.searchUsers(query) }
                        val postsDeferred = async { searchRepository.searchPosts(query) }
                        val mediaDeferred = async { searchRepository.searchMedia(query) }

                        val users = usersDeferred.await().getOrThrow()
                        val posts = postsDeferred.await().getOrThrow()
                        val media = mediaDeferred.await().getOrThrow()

                        results.addAll(users)
                        results.addAll(posts)
                        results.addAll(media)
                    }
                    SearchFilter.PEOPLE -> {
                        results.addAll(searchRepository.searchUsers(query).getOrThrow())
                    }
                    SearchFilter.POSTS -> {
                        results.addAll(searchRepository.searchPosts(query).getOrThrow())
                    }
                    SearchFilter.PHOTOS -> {
                        results.addAll(searchRepository.searchMedia(query, mediaType = SearchResult.MediaType.PHOTO).getOrThrow())
                    }
                    SearchFilter.VIDEOS -> {
                        results.addAll(searchRepository.searchMedia(query, mediaType = SearchResult.MediaType.VIDEO).getOrThrow())
                    }
                }

                _uiState.update { it.copy(isLoading = false, results = results) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            searchRepository: SearchRepository = SearchRepositoryImpl()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(searchRepository) as T
            }
        }
    }
}
