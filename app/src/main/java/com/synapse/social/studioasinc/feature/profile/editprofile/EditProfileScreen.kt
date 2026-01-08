package com.synapse.social.studioasinc.presentation.editprofile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.presentation.editprofile.components.GenderSelector
import com.synapse.social.studioasinc.presentation.editprofile.components.ProfileFormFields
import com.synapse.social.studioasinc.presentation.editprofile.components.ProfileImageSection
import com.synapse.social.studioasinc.ui.settings.SettingsCard
import com.synapse.social.studioasinc.ui.settings.SettingsNavigationItem
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing
import com.synapse.social.studioasinc.ui.theme.SynapseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToRegionSelection: (String) -> Unit,
    onNavigateToPhotoHistory: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Image Pickers
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(EditProfileEvent.AvatarSelected(uri))
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(EditProfileEvent.CoverSelected(uri))
        }
    }

    // Navigation Events
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                EditProfileNavigation.NavigateBack -> onNavigateBack()
                EditProfileNavigation.NavigateToRegionSelection -> {
                    onNavigateToRegionSelection(viewModel.uiState.value.selectedRegion ?: "")
                }
                EditProfileNavigation.NavigateToProfileHistory -> {
                    onNavigateToPhotoHistory("PROFILE")
                }
                EditProfileNavigation.NavigateToCoverHistory -> {
                    onNavigateToPhotoHistory("COVER")
                }
            }
        }
    }

    // Error Handling
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    SynapseTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = { Text("Edit Profile") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(EditProfileEvent.BackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (uiState.isSaving) {
                             ExpressiveLoadingIndicator(
                                 modifier = Modifier.padding(end = 16.dp).size(24.dp)
                             )
                        } else {
                            TextButton(
                                onClick = { viewModel.onEvent(EditProfileEvent.SaveClicked) },
                                enabled = uiState.hasChanges &&
                                          uiState.usernameValidation !is UsernameValidation.Error &&
                                          uiState.nicknameError == null &&
                                          uiState.bioError == null,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ExpressiveLoadingIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = SettingsSpacing.screenPadding,
                        end = SettingsSpacing.screenPadding,
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
                ) {
                    // Image Section
                    item {
                        ProfileImageSection(
                            coverUrl = uiState.coverUrl,
                            avatarUrl = uiState.avatarUrl,
                            avatarUploadState = uiState.avatarUploadState,
                            coverUploadState = uiState.coverUploadState,
                            onCoverClick = {
                                coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onAvatarClick = {
                                avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onRetryAvatarUpload = {
                                viewModel.onEvent(EditProfileEvent.RetryAvatarUpload)
                            },
                            onRetryCoverUpload = {
                                viewModel.onEvent(EditProfileEvent.RetryCoverUpload)
                            }
                        )
                    }

                    // Form Fields
                    item {
                        ProfileFormFields(
                            username = uiState.username,
                            onUsernameChange = { viewModel.onEvent(EditProfileEvent.UsernameChanged(it)) },
                            usernameValidation = uiState.usernameValidation,
                            nickname = uiState.nickname,
                            onNicknameChange = { viewModel.onEvent(EditProfileEvent.NicknameChanged(it)) },
                            nicknameError = uiState.nicknameError,
                            bio = uiState.bio,
                            onBiographyChange = { viewModel.onEvent(EditProfileEvent.BiographyChanged(it)) },
                            bioError = uiState.bioError
                        )
                    }

                    // Gender Section
                    item {
                        GenderSelector(
                            selectedGender = uiState.selectedGender,
                            onGenderSelected = { viewModel.onEvent(EditProfileEvent.GenderSelected(it)) }
                        )
                    }

                    // Region Section
                    item {
                        SettingsCard {
                            SettingsNavigationItem(
                                title = "Region",
                                subtitle = uiState.selectedRegion ?: "Not set",
                                icon = R.drawable.ic_location, // Need to verify if this exists or use fallback
                                onClick = {
                                    onNavigateToRegionSelection(uiState.selectedRegion ?: "")
                                }
                            )
                        }
                    }

                    // History Section
                    item {
                        SettingsCard {
                            SettingsNavigationItem(
                                title = "Profile Photo History",
                                subtitle = "View and restore previous photos",
                                icon = null,
                                onClick = { viewModel.onEvent(EditProfileEvent.ProfileHistoryClicked) }
                            )

                            com.synapse.social.studioasinc.ui.settings.SettingsDivider()

                            SettingsNavigationItem(
                                title = "Cover Photo History",
                                subtitle = "View and restore previous covers",
                                icon = null,
                                onClick = { viewModel.onEvent(EditProfileEvent.CoverHistoryClicked) }
                            )
                        }
                    }
                }
            }
        }
    }
}
