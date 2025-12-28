/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pacedream.common.composables.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.size.Scale
import com.pacedream.common.composables.theme.PaceDreamDesignSystem

/**
 * Enhanced Image Components with Coil Integration
 * Provides consistent image loading with proper error handling and placeholders
 */

@Composable
fun PaceDreamAsyncImage(
    imageUrl: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null,
    loading: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    Box(modifier = modifier) {
        var isLoading by remember { mutableStateOf(true) }
        var hasError by remember { mutableStateOf(false) }
        
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onLoading = { isLoading = true; hasError = false },
            onSuccess = { isLoading = false; hasError = false },
            onError = { isLoading = false; hasError = true }
        )
        
        if (isLoading && loading != null) {
            loading()
        } else if (isLoading && placeholder != null) {
            placeholder()
        }
        
        if (hasError && error != null) {
            error()
        }
    }
}

@Composable
fun PaceDreamPropertyImage(
    imageUrl: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    showFavoriteButton: Boolean = true,
    onFavoriteClick: () -> Unit = {},
    isFavorite: Boolean = false
) {
    Box(modifier = modifier) {
        PaceDreamAsyncImage(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(PaceDreamDesignSystem.PaceDreamRadius.MD)),
            placeholder = {
                PaceDreamImagePlaceholder(
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                PaceDreamImageError(
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
        
        if (showFavoriteButton) {
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(PaceDreamDesignSystem.PaceDreamSpacing.SM)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = Color.White,
                    modifier = Modifier.size(PaceDreamDesignSystem.PaceDreamIconSize.SM)
                )
            }
        }
    }
}

@Composable
fun PaceDreamUserAvatar(
    imageUrl: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Int = PaceDreamDesignSystem.PaceDreamIconSize.MD.value.toInt()
) {
    PaceDreamAsyncImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        placeholder = {
            PaceDreamAvatarPlaceholder(
                modifier = Modifier.fillMaxSize()
            )
        },
        error = {
            PaceDreamAvatarPlaceholder(
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

@Composable
fun PaceDreamImageCarousel(
    images: List<String>,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit = {}
) {
    if (images.isEmpty()) {
        PaceDreamImagePlaceholder(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    } else {
        // This would be implemented with a proper carousel component
        // For now, showing the first image
        PaceDreamAsyncImage(
            imageUrl = images.first(),
            contentDescription = "Property image",
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(PaceDreamDesignSystem.PaceDreamRadius.MD)),
            placeholder = {
                PaceDreamImagePlaceholder(
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                PaceDreamImageError(
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
    }
}

@Composable
fun PaceDreamImagePlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(PaceDreamDesignSystem.PaceDreamColors.SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = "Loading image",
            tint = PaceDreamDesignSystem.PaceDreamColors.OnSurfaceVariant,
            modifier = Modifier.size(PaceDreamDesignSystem.PaceDreamIconSize.LG)
        )
    }
}

@Composable
fun PaceDreamImageError(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(PaceDreamDesignSystem.PaceDreamColors.ErrorContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error loading image",
                tint = PaceDreamDesignSystem.PaceDreamColors.OnErrorContainer,
                modifier = Modifier.size(PaceDreamDesignSystem.PaceDreamIconSize.MD)
            )
            Spacer(modifier = Modifier.height(PaceDreamDesignSystem.PaceDreamSpacing.XS))
            Text(
                text = "Failed to load",
                style = PaceDreamDesignSystem.PaceDreamTypography.Caption,
                color = PaceDreamDesignSystem.PaceDreamColors.OnErrorContainer
            )
        }
    }
}

@Composable
fun PaceDreamAvatarPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(PaceDreamDesignSystem.PaceDreamColors.SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "User avatar",
            tint = PaceDreamDesignSystem.PaceDreamColors.OnSurfaceVariant,
            modifier = Modifier.size(PaceDreamDesignSystem.PaceDreamIconSize.MD)
        )
    }
}

@Composable
fun PaceDreamLoadingImage(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(PaceDreamDesignSystem.PaceDreamColors.SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(PaceDreamDesignSystem.PaceDreamIconSize.MD),
            color = PaceDreamDesignSystem.PaceDreamColors.Primary,
            strokeWidth = 2.dp
        )
    }
}
