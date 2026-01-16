package com.synapse.social.studioasinc.feature.auth.ui.components

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import com.synapse.social.studioasinc.feature.profile.ProfileEditActivity
import com.synapse.social.studioasinc.ui.theme.AuthTheme

class ProfileCompletionDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AuthTheme {
                    ProfileCompletionDialog(
                        onComplete = {
                            updatePrefs()
                            dismiss()
                            val intent = Intent(requireContext(), ProfileEditActivity::class.java)
                            startActivity(intent)
                        },
                        onDismiss = {
                            updatePrefs()
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    private fun updatePrefs() {
        requireContext().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("show_profile_completion_dialog", false)
            .apply()
    }

    companion object {
        const val TAG = "ProfileCompletionDialogFragment"
    }
}
