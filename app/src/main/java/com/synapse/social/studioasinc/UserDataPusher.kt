
package com.synapse.social.studioasinc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Supabase-based UserDataPusher that replaces Firebase implementation.
 */
class UserDataPusher {

    fun pushData(
        username: String,
        nickname: String,
        biography: String,
        thedpurl: String,
        googleLoginAvatarUri: String?,
        email: String,
        uid: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val userData = mapOf(
                    "username" to username,
                    "nickname" to nickname,
                    "biography" to biography,
                    "profile_image_url" to thedpurl,
                    "google_avatar_url" to googleLoginAvatarUri,
                    "email" to email
                )
                
                SupabaseUserDataPusher.updateUserProfile(uid, userData)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }
}
