package com.synapse.social.studioasinc.feature.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileEvent
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileScreen
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileViewModel
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.PhotoHistoryScreen
import com.synapse.social.studioasinc.presentation.editprofile.photohistory.PhotoType
import com.synapse.social.studioasinc.ui.settings.SelectRegionScreen
import com.synapse.social.studioasinc.ui.theme.SynapseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileEditActivity : ComponentActivity() {

    private val viewModel: EditProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("ProfileEditActivity", "onCreate called")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        try {
            android.util.Log.d("ProfileEditActivity", "Setting up content")
            setContent {
                SynapseTheme {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "edit_profile") {
                        composable("edit_profile") {
                            EditProfileScreen(
                                viewModel = viewModel,
                                onNavigateBack = { finish() },
                                onNavigateToRegionSelection = { currentRegion ->
                                    val encodedRegion = java.net.URLEncoder.encode(currentRegion, "UTF-8")
                                    navController.navigate("select_region?currentRegion=$encodedRegion")
                                },
                                onNavigateToPhotoHistory = { type ->
                                    navController.navigate("photo_history/$type")
                                }
                            )
                        }

                        composable(
                            route = "select_region?currentRegion={currentRegion}",
                            arguments = listOf(navArgument("currentRegion") {
                                type = NavType.StringType
                                defaultValue = ""
                            })
                        ) { backStackEntry ->
                            val currentRegion = backStackEntry.arguments?.getString("currentRegion") ?: ""
                            SelectRegionScreen(
                                currentRegion = currentRegion,
                                onRegionSelected = { region ->
                                    viewModel.onEvent(EditProfileEvent.RegionSelected(region))
                                    navController.popBackStack()
                                },
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = "photo_history/{type}",
                            arguments = listOf(navArgument("type") {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val typeStr = backStackEntry.arguments?.getString("type") ?: "PROFILE"
                            val photoType = try {
                                 PhotoType.valueOf(typeStr)
                            } catch (e: Exception) {
                                 PhotoType.PROFILE
                            }

                            PhotoHistoryScreen(
                                type = photoType,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
            android.util.Log.d("ProfileEditActivity", "Content setup completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("ProfileEditActivity", "Error in onCreate", e)
            throw e
        }
    }
}
