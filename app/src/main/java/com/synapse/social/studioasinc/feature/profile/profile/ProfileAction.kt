package com.synapse.social.studioasinc.ui.profile

sealed class ProfileAction {
    data class LoadProfile(val userId: String) : ProfileAction()
    object RefreshProfile : ProfileAction()
    data class FollowUser(val targetUserId: String) : ProfileAction()
    data class UnfollowUser(val targetUserId: String) : ProfileAction()
    data class SwitchContentFilter(val filter: ProfileContentFilter) : ProfileAction()
    data class LoadMoreContent(val filter: ProfileContentFilter) : ProfileAction()
    object NavigateToEditProfile : ProfileAction()
    object NavigateToAddStory : ProfileAction()
    object OpenMoreMenu : ProfileAction()
    object CloseMoreMenu : ProfileAction()
}
