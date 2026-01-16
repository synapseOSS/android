package com.synapse.social.studioasinc.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.data.local.database.StorageConfig
import kotlinx.coroutines.delay

// Provider data class for cleaner code
data class ProviderInfo(
    val name: String,
    val icon: ImageVector,
    val description: String,
    val supportsPhoto: Boolean = true,
    val supportsVideo: Boolean = true,
    val supportsOther: Boolean = true
)

// All available providers with their capabilities
private val allProviders = listOf(
    ProviderInfo(
        name = "Default",
        icon = Icons.Default.Security,
        description = "Built-in app credentials (recommended)",
        supportsPhoto = true,
        supportsVideo = true,
        supportsOther = true
    ),
    ProviderInfo(
        name = "ImgBB",
        icon = Icons.Default.Image,
        description = "Free image hosting service",
        supportsPhoto = true,
        supportsVideo = false,
        supportsOther = false
    ),
    ProviderInfo(
        name = "Cloudinary",
        icon = Icons.Default.CloudUpload,
        description = "Cloud-based image & video management",
        supportsPhoto = true,
        supportsVideo = true,
        supportsOther = false
    ),
    ProviderInfo(
        name = "Supabase",
        icon = Icons.Default.Storage,
        description = "Open source Firebase alternative",
        supportsPhoto = true,
        supportsVideo = true,
        supportsOther = true
    ),
    ProviderInfo(
        name = "Cloudflare R2",
        icon = Icons.Default.Cloud,
        description = "S3-compatible object storage",
        supportsPhoto = true,
        supportsVideo = true,
        supportsOther = true
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageProviderScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel
) {
    val storageConfig by viewModel.storageConfig.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // Track which configuration section to expand
    var expandedConfigSection by remember { mutableStateOf<String?>(null) }

    // Handle back press
    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Storage Providers")
                        Text(
                            text = "Select one provider per media type",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(1f - scrollBehavior.state.collapsedFraction)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Status Overview Card
            item {
                StorageStatusCard(storageConfig = storageConfig)
            }

            // Photo Provider Selection
            item {
                AnimatedProviderCard(
                    title = "Photo Storage",
                    description = "For images (JPG, PNG, GIF, WebP, etc.)",
                    icon = Icons.Default.Image,
                    iconTint = MaterialTheme.colorScheme.primary,
                    providers = allProviders.filter { it.supportsPhoto },
                    selectedProvider = storageConfig.photoProvider,
                    storageConfig = storageConfig,
                    onProviderSelected = { viewModel.updatePhotoProvider(it) },
                    index = 0
                )
            }

            // Video Provider Selection
            item {
                AnimatedProviderCard(
                    title = "Video Storage",
                    description = "For videos (MP4, MOV, AVI, etc.)",
                    icon = Icons.Default.Videocam,
                    iconTint = Color(0xFF6366F1),
                    providers = allProviders.filter { it.supportsVideo },
                    selectedProvider = storageConfig.videoProvider,
                    storageConfig = storageConfig,
                    onProviderSelected = { viewModel.updateVideoProvider(it) },
                    index = 1
                )
            }

            // Other Files Provider Selection
            item {
                AnimatedProviderCard(
                    title = "Other Files",
                    description = "For audio, documents, and other files",
                    icon = Icons.Default.Folder,
                    iconTint = Color(0xFF10B981),
                    providers = allProviders.filter { it.supportsOther },
                    selectedProvider = storageConfig.otherProvider,
                    storageConfig = storageConfig,
                    onProviderSelected = { viewModel.updateOtherProvider(it) },
                    index = 2
                )
            }

            // Divider before configuration section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  CONFIGURATION  ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }

            // Configuration Tip
            item {
                ConfigurationTipCard()
            }

            // Provider Configuration Cards - only show for providers that are selected and not "Default"
            val selectedProviders = setOfNotNull(
                storageConfig.photoProvider,
                storageConfig.videoProvider,
                storageConfig.otherProvider
            ).filter { it != "Default" }  // Exclude "Default" from configuration section

            if (selectedProviders.contains("ImgBB")) {
                item {
                    ExpandableConfigCard(
                        title = "ImgBB",
                        icon = Icons.Default.Image,
                        isConfigured = storageConfig.isImgBBConfigured(),
                        isExpanded = expandedConfigSection == "ImgBB",
                        onExpandToggle = {
                            expandedConfigSection = if (expandedConfigSection == "ImgBB") null else "ImgBB"
                        }
                    ) {
                        ImgBBConfigContent(
                            apiKey = storageConfig.imgBBConfig.apiKey,
                            onApiKeyChange = { viewModel.updateImgBBConfig(it) }
                        )
                    }
                }
            }

            if (selectedProviders.contains("Cloudinary")) {
                item {
                    ExpandableConfigCard(
                        title = "Cloudinary",
                        icon = Icons.Default.CloudUpload,
                        isConfigured = storageConfig.isCloudinaryConfigured(),
                        isExpanded = expandedConfigSection == "Cloudinary",
                        onExpandToggle = {
                            expandedConfigSection = if (expandedConfigSection == "Cloudinary") null else "Cloudinary"
                        }
                    ) {
                        CloudinaryConfigContent(
                            cloudName = storageConfig.cloudinaryConfig.cloudName,
                            apiKey = storageConfig.cloudinaryConfig.apiKey,
                            apiSecret = storageConfig.cloudinaryConfig.apiSecret,
                            onConfigChange = { name, key, secret ->
                                viewModel.updateCloudinaryConfig(name, key, secret)
                            }
                        )
                    }
                }
            }

            if (selectedProviders.contains("Supabase")) {
                item {
                    ExpandableConfigCard(
                        title = "Supabase Storage",
                        icon = Icons.Default.Storage,
                        isConfigured = storageConfig.isSupabaseConfigured(),
                        isExpanded = expandedConfigSection == "Supabase",
                        onExpandToggle = {
                            expandedConfigSection = if (expandedConfigSection == "Supabase") null else "Supabase"
                        }
                    ) {
                        SupabaseConfigContent(
                            url = storageConfig.supabaseConfig.url,
                            apiKey = storageConfig.supabaseConfig.apiKey,
                            bucketName = storageConfig.supabaseConfig.bucketName,
                            onConfigChange = { url, key, bucket ->
                                viewModel.updateSupabaseConfig(url, key, bucket)
                            }
                        )
                    }
                }
            }

            if (selectedProviders.contains("Cloudflare R2")) {
                item {
                    ExpandableConfigCard(
                        title = "Cloudflare R2",
                        icon = Icons.Default.Cloud,
                        isConfigured = storageConfig.isR2Configured(),
                        isExpanded = expandedConfigSection == "Cloudflare R2",
                        onExpandToggle = {
                            expandedConfigSection = if (expandedConfigSection == "Cloudflare R2") null else "Cloudflare R2"
                        }
                    ) {
                        R2ConfigContent(
                            accountId = storageConfig.r2Config.accountId,
                            accessKeyId = storageConfig.r2Config.accessKeyId,
                            secretAccessKey = storageConfig.r2Config.secretAccessKey,
                            bucketName = storageConfig.r2Config.bucketName,
                            onConfigChange = { acc, key, secret, bucket ->
                                viewModel.updateR2Config(acc, key, secret, bucket)
                            }
                        )
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StorageStatusCard(storageConfig: StorageConfig) {
    val hasPhotoProvider = storageConfig.photoProvider != null
    val hasVideoProvider = storageConfig.videoProvider != null
    val hasOtherProvider = storageConfig.otherProvider != null
    
    val photoConfigured = storageConfig.photoProvider?.let { storageConfig.isProviderConfigured(it) } ?: false
    val videoConfigured = storageConfig.videoProvider?.let { storageConfig.isProviderConfigured(it) } ?: false
    val otherConfigured = storageConfig.otherProvider?.let { storageConfig.isProviderConfigured(it) } ?: false
    
    val allConfigured = photoConfigured && videoConfigured && otherConfigured
    val configuredCount = listOf(photoConfigured, videoConfigured, otherConfigured).count { it }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                allConfigured -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                configuredCount > 0 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Status Icon
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (!allConfigured) 1.1f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        when {
                            allConfigured -> MaterialTheme.colorScheme.primary
                            configuredCount > 0 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        allConfigured -> Icons.Default.CheckCircle
                        configuredCount > 0 -> Icons.Outlined.Warning
                        else -> Icons.Outlined.Info
                    },
                    contentDescription = null,
                    tint = when {
                        allConfigured -> MaterialTheme.colorScheme.onPrimary
                        configuredCount > 0 -> MaterialTheme.colorScheme.onTertiary
                        else -> MaterialTheme.colorScheme.onError
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        allConfigured -> "All Set!"
                        configuredCount > 0 -> "Partially Configured"
                        else -> "Setup Required"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        allConfigured -> "All storage providers are configured and ready"
                        configuredCount > 0 -> "$configuredCount of 3 media types configured"
                        else -> "Select and configure providers for each media type"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Progress indicator
            if (!allConfigured) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Determinate loading using M3 CircularProgressIndicator
                    CircularProgressIndicator(
                        progress = { configuredCount / 3f },
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "$configuredCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedProviderCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    providers: List<ProviderInfo>,
    selectedProvider: String?,
    storageConfig: StorageConfig,
    onProviderSelected: (String?) -> Unit,
    index: Int
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 100L)
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInHorizontally(
                    animationSpec = tween(400, easing = EaseOutCubic),
                    initialOffsetX = { it / 4 }
                )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Status badge
                    val isConfigured = selectedProvider?.let { storageConfig.isProviderConfigured(it) } ?: false
                    AnimatedVisibility(
                        visible = selectedProvider != null,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        StatusBadge(isConfigured = isConfigured)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Provider Radio Buttons with modern design
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    providers.forEach { provider ->
                        val isSelected = selectedProvider == provider.name
                        val isConfigured = storageConfig.isProviderConfigured(provider.name)
                        
                        ProviderRadioItem(
                            provider = provider,
                            isSelected = isSelected,
                            isConfigured = isConfigured,
                            onSelect = {
                                if (isSelected) {
                                    onProviderSelected(null) // Deselect
                                } else {
                                    onProviderSelected(provider.name)
                                }
                            }
                        )
                    }
                }

                // Warning if no provider selected
                AnimatedVisibility(
                    visible = selectedProvider == null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select a provider to enable uploads",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderRadioItem(
    provider: ProviderInfo,
    isSelected: Boolean,
    isConfigured: Boolean,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isSelected && isConfigured -> MaterialTheme.colorScheme.primary
            isSelected -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(300),
        label = "borderColor"
    )
    val animatedBackgroundColor by animateColorAsState(
        targetValue = when {
            isSelected && isConfigured -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            isSelected -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onSelect() },
        shape = RoundedCornerShape(16.dp),
        color = animatedBackgroundColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = animatedBorderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom radio button with animation
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val scaleAnim by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "radioScale"
                )
                
                // Outer circle
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (isSelected) {
                            if (isConfigured) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary
                        } else MaterialTheme.colorScheme.outline
                    )
                ) {}
                
                // Inner circle (animated)
                Surface(
                    modifier = Modifier
                        .size(12.dp)
                        .scale(scaleAnim),
                    shape = CircleShape,
                    color = if (isConfigured) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary
                ) {}
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // Provider icon
            Icon(
                imageVector = provider.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) {
                    if (isConfigured) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary
                } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Configuration status
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isConfigured) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isConfigured) Icons.Default.Check else Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isConfigured) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isConfigured) "Ready" else "Setup",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isConfigured) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isConfigured: Boolean) {
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val alpha by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = if (!isConfigured) 0.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Surface(
        modifier = Modifier.alpha(alpha),
        shape = RoundedCornerShape(8.dp),
        color = if (isConfigured) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConfigured) Icons.Default.Check else Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isConfigured) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isConfigured) "Ready" else "Setup",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isConfigured) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
        }
    }
}

@Composable
private fun ConfigurationTipCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Tap the arrow on each provider below to configure API credentials. Only configured providers can be used for uploads.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpandableConfigCard(
    title: String,
    icon: ImageVector,
    isConfigured: Boolean,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 4.dp else 1.dp
        )
    ) {
        Column {
            // Header (always visible)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() },
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon with background
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isConfigured) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isConfigured) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isConfigured) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Configured",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isConfigured) "Ready to use" else "Configuration required",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConfigured) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    
                    // Expand/Collapse icon with rotation animation
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300, easing = EaseOutCubic),
                        label = "rotation"
                    )
                    
                    IconButton(onClick = onExpandToggle) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    }
                }
            }
            
            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun ImgBBConfigContent(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StorageSecureTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = "API Key"
        )
        HelpText(text = "Get your free API key from api.imgbb.com")
    }
}

@Composable
private fun CloudinaryConfigContent(
    cloudName: String,
    apiKey: String,
    apiSecret: String,
    onConfigChange: (String, String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = cloudName,
            onValueChange = { newName -> onConfigChange(newName, apiKey, apiSecret) },
            label = { Text("Cloud Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        StorageSecureTextField(
            value = apiKey,
            onValueChange = { newKey -> onConfigChange(cloudName, newKey, apiSecret) },
            label = "API Key"
        )
        StorageSecureTextField(
            value = apiSecret,
            onValueChange = { newSecret -> onConfigChange(cloudName, apiKey, newSecret) },
            label = "API Secret"
        )
        HelpText(text = "Find these in your Cloudinary dashboard under Settings > Access Keys")
    }
}

@Composable
private fun SupabaseConfigContent(
    url: String,
    apiKey: String,
    bucketName: String,
    onConfigChange: (String, String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { newVal -> onConfigChange(newVal, apiKey, bucketName) },
            label = { Text("Project URL") },
            placeholder = { Text("https://your-project.supabase.co") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        StorageSecureTextField(
            value = apiKey,
            onValueChange = { newVal -> onConfigChange(url, newVal, bucketName) },
            label = "Service Role / API Key"
        )
        OutlinedTextField(
            value = bucketName,
            onValueChange = { newVal -> onConfigChange(url, apiKey, newVal) },
            label = { Text("Bucket Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        HelpText(text = "Create a bucket in Supabase Storage and ensure policies allow read/write operations")
    }
}

@Composable
private fun R2ConfigContent(
    accountId: String,
    accessKeyId: String,
    secretAccessKey: String,
    bucketName: String,
    onConfigChange: (String, String, String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = accountId,
            onValueChange = { newVal -> onConfigChange(newVal, accessKeyId, secretAccessKey, bucketName) },
            label = { Text("Account ID") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        StorageSecureTextField(
            value = accessKeyId,
            onValueChange = { newVal -> onConfigChange(accountId, newVal, secretAccessKey, bucketName) },
            label = "Access Key ID"
        )
        StorageSecureTextField(
            value = secretAccessKey,
            onValueChange = { newVal -> onConfigChange(accountId, accessKeyId, newVal, bucketName) },
            label = "Secret Access Key"
        )
        OutlinedTextField(
            value = bucketName,
            onValueChange = { newVal -> onConfigChange(accountId, accessKeyId, secretAccessKey, newVal) },
            label = { Text("Bucket Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        HelpText(text = "Create an R2 bucket in your Cloudflare dashboard and generate API tokens")
    }
}

@Composable
private fun HelpText(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Help,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StorageSecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

// Animation easing
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
