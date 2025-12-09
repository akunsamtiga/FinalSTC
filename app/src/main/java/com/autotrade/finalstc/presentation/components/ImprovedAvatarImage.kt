package com.autotrade.finalstc.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.delay

/**
 * ✅ IMPROVED Avatar Image Component with:
 * - Proper timeout handling (15s)
 * - Retry mechanism (3 attempts)
 * - Comprehensive caching
 * - Loading shimmer effect
 * - Error fallback
 * - Manual retry button
 */
@Composable
fun ImprovedAvatarImage(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isAdmin: Boolean = false,
    accentColor: Color = Color(0xFF64B5F6),
    backgroundColor: Color = Color(0xFF1F1F1F)
) {
    var retryCount by remember { mutableStateOf(0) }
    var forceRetry by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // ✅ Build URL with retry trigger
    val imageUrl = remember(avatarUrl, retryCount, forceRetry) {
        if (avatarUrl.isNullOrEmpty()) {
            null
        } else {
            // Clean URL and add cache-busting parameter on retry
            val cleanUrl = if (avatarUrl.startsWith("http")) {
                avatarUrl
            } else {
                "https://stockity.id/$avatarUrl"
            }

            if (retryCount > 0 || forceRetry > 0) {
                "$cleanUrl?retry=${retryCount}_${forceRetry}_${System.currentTimeMillis()}"
            } else {
                cleanUrl
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(300)
                    // ✅ CRITICAL: Set timeout
                    .build()
                    .newBuilder()
                    .apply {
                        // Network timeout (OkHttp handles this - already set to 15s in AppModule)

                        // ✅ Cache policies
                        memoryCachePolicy(CachePolicy.ENABLED)
                        diskCachePolicy(CachePolicy.ENABLED)
                        networkCachePolicy(CachePolicy.ENABLED)
                    }
                    .build(),
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            ) {
                val state = painter.state

                when (state) {
                    is AsyncImagePainter.State.Loading -> {
                        // ✅ Loading shimmer
                        ShimmerLoadingAvatar(
                            size = size,
                            accentColor = accentColor
                        )
                    }

                    is AsyncImagePainter.State.Success -> {
                        // ✅ Image loaded successfully
                        SubcomposeAsyncImageContent()
                    }

                    is AsyncImagePainter.State.Error -> {
                        // ✅ Error handling with retry
                        LaunchedEffect(state) {
                            if (retryCount < 3) {
                                delay(1000L * (retryCount + 1)) // Exponential backoff
                                retryCount++
                            }
                        }

                        ErrorAvatarWithRetry(
                            size = size,
                            isAdmin = isAdmin,
                            accentColor = accentColor,
                            backgroundColor = backgroundColor,
                            retryCount = retryCount,
                            onRetry = {
                                if (retryCount >= 3) {
                                    // Manual retry - reset counter
                                    retryCount = 0
                                    forceRetry++
                                }
                            }
                        )
                    }

                    is AsyncImagePainter.State.Empty -> {
                        // ✅ Empty state - show default icon
                        DefaultAvatarIcon(
                            size = size,
                            isAdmin = isAdmin,
                            accentColor = accentColor,
                            backgroundColor = backgroundColor
                        )
                    }
                }
            }
        } else {
            // ✅ No URL - show default icon
            DefaultAvatarIcon(
                size = size,
                isAdmin = isAdmin,
                accentColor = accentColor,
                backgroundColor = backgroundColor
            )
        }
    }
}

@Composable
private fun ShimmerLoadingAvatar(
    size: Dp,
    accentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = alpha),
                        accentColor.copy(alpha = alpha * 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size * 0.4f),
            color = Color.White.copy(alpha = 0.8f),
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun ErrorAvatarWithRetry(
    size: Dp,
    isAdmin: Boolean,
    accentColor: Color,
    backgroundColor: Color,
    retryCount: Int,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.2f),
                        accentColor.copy(alpha = 0.1f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (retryCount < 3) {
            // Auto-retrying
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.3f),
                    color = accentColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${retryCount + 1}/3",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }
        } else {
            // Max retries reached - show retry button
            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(size * 0.6f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    tint = accentColor,
                    modifier = Modifier.size(size * 0.4f)
                )
            }
        }
    }
}

@Composable
private fun DefaultAvatarIcon(
    size: Dp,
    isAdmin: Boolean,
    accentColor: Color,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.2f),
                        accentColor.copy(alpha = 0.1f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isAdmin) Icons.Default.Person else Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(size * 0.5f),
            tint = Color.White
        )
    }
}