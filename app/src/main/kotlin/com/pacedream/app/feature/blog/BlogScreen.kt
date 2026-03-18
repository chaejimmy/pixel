package com.pacedream.app.feature.blog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pacedream.common.icon.PaceDreamIcons
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import com.pacedream.app.core.network.ApiError
import com.pacedream.app.core.network.ApiResult
import com.pacedream.common.composables.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ── Data Models (iOS BlogService parity) ────────────────────────

@Serializable
data class BlogAuthor(
    val id: String? = null, val name: String? = null,
    val avatar: String? = null, val bio: String? = null
) { val resolvedName: String get() = name ?: "PaceDream Team" }

@Serializable
data class BlogPost(
    val id: String = "", val title: String = "", val content: String = "",
    val excerpt: String = "", val author: BlogAuthor? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
    val category: String? = null, val tags: List<String> = emptyList(),
    @SerialName("publishedAt") val publishedAt: String = "",
    @SerialName("readTime") val readTime: Int = 0
) {
    val displayDate: String get() = publishedAt.take(10)
    val readTimeLabel: String get() = "$readTime min read"
}

@Serializable
data class BlogsEnvelope(
    val status: Boolean? = null, val success: Boolean? = null,
    val data: List<BlogPost>? = null, val posts: List<BlogPost>? = null,
    val total: Int? = null, val page: Int? = null,
    val categories: List<String>? = null
) { val resolvedPosts: List<BlogPost> get() = data ?: posts ?: emptyList() }

@Serializable
data class BlogPostEnvelope(
    val status: Boolean? = null, val success: Boolean? = null,
    val data: BlogPost? = null, val post: BlogPost? = null
) { val resolvedPost: BlogPost? get() = data ?: post }

// ── Repository ──────────────────────────────────────────────────

@Singleton
class BlogRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val appConfig: AppConfig,
    private val json: Json
) {
    suspend fun getPosts(page: Int = 1, limit: Int = 20): ApiResult<BlogsEnvelope> {
        val url = appConfig.buildApiUrl("blog", "posts") + "?page=$page&limit=$limit"
        return when (val result = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> {
                try {
                    ApiResult.Success(json.decodeFromString(BlogsEnvelope.serializer(), result.data))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse blog posts")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun getPost(postId: String): ApiResult<BlogPostEnvelope> {
        val url = appConfig.buildApiUrl("blog", "posts", postId)
        return when (val result = apiClient.get(url, includeAuth = false)) {
            is ApiResult.Success -> {
                try {
                    ApiResult.Success(json.decodeFromString(BlogPostEnvelope.serializer(), result.data))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse blog post")
                    ApiResult.Failure(ApiError.DecodingError())
                }
            }
            is ApiResult.Failure -> result
        }
    }
}

// ── ViewModel ───────────────────────────────────────────────────

data class BlogUiState(
    val posts: List<BlogPost> = emptyList(), val selectedPost: BlogPost? = null,
    val categories: List<String> = emptyList(), val selectedCategory: String? = null,
    val isLoading: Boolean = false, val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false, val error: String? = null,
    val currentPage: Int = 1, val hasMore: Boolean = true
) {
    val filteredPosts: List<BlogPost>
        get() = if (selectedCategory == null) posts
        else posts.filter { it.category.equals(selectedCategory, ignoreCase = true) }
}

@HiltViewModel
class BlogViewModel @Inject constructor(
    private val repository: BlogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlogUiState(isLoading = true))
    val uiState: StateFlow<BlogUiState> = _uiState.asStateFlow()

    init { loadPosts() }

    fun loadPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, currentPage = 1) }
            when (val result = repository.getPosts(page = 1)) {
                is ApiResult.Success -> {
                    val data = result.data
                    _uiState.update {
                        it.copy(
                            posts = data.resolvedPosts,
                            categories = data.categories ?: extractCategories(data.resolvedPosts),
                            isLoading = false,
                            isRefreshing = false,
                            hasMore = data.resolvedPosts.size >= 20
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "Failed to load posts") }
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadPosts()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        viewModelScope.launch {
            val nextPage = state.currentPage + 1
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = repository.getPosts(page = nextPage)) {
                is ApiResult.Success -> {
                    val newPosts = result.data.resolvedPosts
                    _uiState.update {
                        it.copy(
                            posts = it.posts + newPosts,
                            currentPage = nextPage,
                            isLoadingMore = false,
                            hasMore = newPosts.size >= 20
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    fun selectCategory(category: String?) =
        _uiState.update { it.copy(selectedCategory = if (it.selectedCategory == category) null else category) }

    fun selectPost(post: BlogPost) = _uiState.update { it.copy(selectedPost = post) }
    fun clearSelectedPost() = _uiState.update { it.copy(selectedPost = null) }

    fun loadPostDetail(postId: String) = viewModelScope.launch {
        when (val result = repository.getPost(postId)) {
            is ApiResult.Success -> result.data.resolvedPost?.let { p -> _uiState.update { it.copy(selectedPost = p) } }
            is ApiResult.Failure -> { /* keep existing data */ }
        }
    }

    private fun extractCategories(posts: List<BlogPost>): List<String> =
        posts.mapNotNull { it.category }.distinct().sorted()
}

// ── Blog List Screen ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogListScreen(
    onBackClick: () -> Unit = {},
    onPostClick: (BlogPost) -> Unit = {},
    viewModel: BlogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blog", style = PaceDreamTypography.Title1, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PaceDreamColors.Primary)
                }
            } else if (uiState.error != null && uiState.posts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "Error", color = PaceDreamColors.TextSecondary)
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                        Button(onClick = { viewModel.loadPosts() }) { Text("Retry") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = PaceDreamSpacing.MD)
                ) {
                    // Category filter chips
                    if (uiState.categories.isNotEmpty()) {
                        item {
                            CategoryFilterRow(
                                categories = uiState.categories,
                                selected = uiState.selectedCategory,
                                onSelect = { viewModel.selectCategory(it) }
                            )
                            Spacer(Modifier.height(PaceDreamSpacing.MD))
                        }
                    }

                    // Blog post cards
                    val posts = uiState.filteredPosts
                    if (posts.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No posts found", color = PaceDreamColors.TextSecondary)
                            }
                        }
                    } else {
                        items(posts, key = { it.id }) { post ->
                            BlogPostCard(post = post, onClick = { onPostClick(post) })
                            Spacer(Modifier.height(PaceDreamSpacing.SM))
                        }

                        // Load more trigger
                        if (uiState.hasMore && uiState.selectedCategory == null) {
                            item {
                                LaunchedEffect(Unit) { viewModel.loadMore() }
                                if (uiState.isLoadingMore) {
                                    Box(Modifier.fillMaxWidth().padding(PaceDreamSpacing.MD), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PaceDreamColors.Primary)
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(PaceDreamSpacing.XL)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(categories: List<String>, selected: String?, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)) {
        items(categories) { category ->
            val isSelected = category == selected
            FilterChip(
                selected = isSelected, onClick = { onSelect(category) },
                label = { Text(category, style = PaceDreamTypography.Caption, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PaceDreamColors.Primary, selectedLabelColor = Color.White)
            )
        }
    }
}

@Composable
private fun BlogPostCard(post: BlogPost, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(PaceDreamRadius.LG),
        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.CardBackground)
    ) {
        Column {
            post.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = post.title,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(topStart = PaceDreamRadius.LG, topEnd = PaceDreamRadius.LG)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                post.category?.let { cat ->
                    Text(
                        cat.uppercase(),
                        style = PaceDreamTypography.Caption,
                        fontWeight = FontWeight.SemiBold,
                        color = PaceDreamColors.Primary
                    )
                    Spacer(Modifier.height(PaceDreamSpacing.XS))
                }
                Text(
                    post.title,
                    style = PaceDreamTypography.Title3,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = PaceDreamColors.TextPrimary
                )
                if (post.excerpt.isNotBlank()) {
                    Spacer(Modifier.height(PaceDreamSpacing.XS))
                    Text(
                        post.excerpt,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(PaceDreamSpacing.SM))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    post.author?.avatar?.let { avatarUrl ->
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).clip(CircleShape).background(PaceDreamColors.Divider)
                        )
                        Spacer(Modifier.width(PaceDreamSpacing.XS))
                    }
                    Text(
                        post.author?.resolvedName ?: "PaceDream Team",
                        style = PaceDreamTypography.Caption,
                        fontWeight = FontWeight.Medium,
                        color = PaceDreamColors.TextSecondary
                    )
                    Spacer(Modifier.weight(1f))
                    Text(post.displayDate, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary)
                    Spacer(Modifier.width(PaceDreamSpacing.SM))
                    Icon(PaceDreamIcons.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = PaceDreamColors.TextTertiary)
                    Spacer(Modifier.width(2.dp))
                    Text(post.readTimeLabel, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextTertiary)
                }
            }
        }
    }
}

// ── Blog Detail Screen ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogDetailScreen(
    post: BlogPost,
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PaceDreamIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = PaceDreamSpacing.XL)
        ) {
            // Hero image
            post.imageUrl?.let { url ->
                item {
                    AsyncImage(
                        model = url,
                        contentDescription = post.title,
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(PaceDreamSpacing.MD)) {
                    // Category
                    post.category?.let { cat ->
                        Text(
                            cat.uppercase(),
                            style = PaceDreamTypography.Caption,
                            fontWeight = FontWeight.SemiBold,
                            color = PaceDreamColors.Primary
                        )
                        Spacer(Modifier.height(PaceDreamSpacing.XS))
                    }

                    // Title
                    Text(
                        post.title,
                        style = PaceDreamTypography.LargeTitle,
                        fontWeight = FontWeight.Bold,
                        color = PaceDreamColors.TextPrimary
                    )

                    Spacer(Modifier.height(PaceDreamSpacing.MD))

                    // Author info row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        post.author?.avatar?.let { avatarUrl ->
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(PaceDreamColors.Divider)
                            )
                            Spacer(Modifier.width(PaceDreamSpacing.SM))
                        }
                        Column {
                            Text(
                                post.author?.resolvedName ?: "PaceDream Team",
                                style = PaceDreamTypography.Body,
                                fontWeight = FontWeight.SemiBold,
                                color = PaceDreamColors.TextPrimary
                            )
                            Row {
                                Text(post.displayDate, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                                Text(" \u00B7 ", style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                                Text(post.readTimeLabel, style = PaceDreamTypography.Caption, color = PaceDreamColors.TextSecondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(PaceDreamSpacing.LG))
                    HorizontalDivider(color = PaceDreamColors.Divider)
                    Spacer(Modifier.height(PaceDreamSpacing.LG))

                    // Content
                    Text(
                        post.content,
                        style = PaceDreamTypography.Body,
                        color = PaceDreamColors.TextPrimary,
                        lineHeight = PaceDreamTypography.Body.lineHeight
                    )

                    // Tags
                    if (post.tags.isNotEmpty()) {
                        Spacer(Modifier.height(PaceDreamSpacing.LG))
                        HorizontalDivider(color = PaceDreamColors.Divider)
                        Spacer(Modifier.height(PaceDreamSpacing.MD))
                        Row(horizontalArrangement = Arrangement.spacedBy(PaceDreamSpacing.XS)) {
                            post.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(PaceDreamRadius.Round),
                                    color = PaceDreamColors.Primary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        "#$tag",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = PaceDreamTypography.Caption,
                                        fontWeight = FontWeight.Medium,
                                        color = PaceDreamColors.Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
