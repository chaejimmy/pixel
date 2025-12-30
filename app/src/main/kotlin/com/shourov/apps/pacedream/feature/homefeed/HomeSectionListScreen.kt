package com.shourov.apps.pacedream.feature.homefeed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacedream.common.composables.theme.PaceDreamColors
import com.pacedream.common.composables.theme.PaceDreamSpacing
import com.pacedream.common.composables.theme.PaceDreamTypography
import com.shourov.apps.pacedream.core.network.api.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class HomeSectionListViewModel @Inject constructor(
    private val repo: HomeFeedRepository
) : ViewModel() {
    var isRefreshing by mutableStateOf(false)
        private set

    var items by mutableStateOf<List<HomeCard>>(emptyList())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var page1 by mutableStateOf(1)
        private set

    var hasMore by mutableStateOf(true)
        private set

    fun refresh(section: HomeSectionKey) {
        viewModelScope.launch {
            isRefreshing = true
            page1 = 1
            load(section, reset = true)
            isRefreshing = false
        }
    }

    fun loadMore(section: HomeSectionKey) {
        if (!hasMore) return
        viewModelScope.launch {
            load(section, reset = false)
        }
    }

    private suspend fun load(section: HomeSectionKey, reset: Boolean) {
        errorMessage = null
        val shareType = section.shareType ?: return
        val nextPage = if (reset) 1 else (page1 + 1)
        val res = repo.getListingsShareTypePage(shareType = shareType, page1 = nextPage, limit = 24)
        when (res) {
            is ApiResult.Success -> {
                page1 = nextPage
                items = if (reset) res.data else (items + res.data)
                hasMore = res.data.size >= 24
            }
            is ApiResult.Failure -> {
                hasMore = false
                errorMessage = res.error.message ?: "Failed to load"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSectionListScreen(
    section: HomeSectionKey,
    onBack: () -> Unit,
    onListingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeSectionListViewModel = hiltViewModel()
) {
    LaunchedEffect(section) {
        viewModel.refresh(section)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(section.displayTitle, style = PaceDreamTypography.Title2, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = com.pacedream.common.icon.PaceDreamIcons.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaceDreamColors.Background)
            )
        },
        containerColor = PaceDreamColors.Background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.refresh(section) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PaceDreamColors.Background),
                contentPadding = PaddingValues(horizontal = PaceDreamSpacing.LG, vertical = PaceDreamSpacing.SM),
                verticalArrangement = Arrangement.spacedBy(PaceDreamSpacing.SM)
            ) {
                items(viewModel.items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onListingClick(item.id) },
                        colors = CardDefaults.cardColors(containerColor = PaceDreamColors.Card)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaceDreamSpacing.MD),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = PaceDreamTypography.Body,
                                color = PaceDreamColors.TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            item.priceText?.let {
                                Text(
                                    text = it,
                                    style = PaceDreamTypography.Caption,
                                    color = PaceDreamColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                viewModel.errorMessage?.let { msg ->
                    item {
                        Text(msg, style = PaceDreamTypography.Body, color = PaceDreamColors.Error)
                    }
                }

                if (viewModel.hasMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaceDreamSpacing.MD),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = PaceDreamColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        LaunchedEffect(Unit) { viewModel.loadMore(section) }
                    }
                } else {
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

