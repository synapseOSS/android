package com.synapse.social.studioasinc

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileEvent
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileScreen
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileViewModel
import com.synapse.social.studioasinc.ui.settings.SelectRegionScreen
import com.synapse.social.studioasinc.ui.theme.SynapseTheme

class ProfileEditActivity : BaseActivity() {

    private val viewModel: EditProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                }
            }
        }
    }
}
