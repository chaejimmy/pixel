package com.shourov.apps.pacedream.signin

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.shourov.apps.pacedream.core.ui.OnBoardingScreenItems.onBoardingScreenItems
import com.shourov.apps.pacedream.feature.signin.R
import com.pacedream.common.composables.theme.PaceDreamTheme
import com.pacedream.common.composables.theme.slightlyDeemphasizedAlpha
import com.pacedream.common.composables.theme.stronglyDeemphasizedAlpha
import kotlin.math.absoluteValue

@Composable
fun OnBoardingScreen(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
) {
    val pagerState = rememberPagerState(
        pageCount = {
            onBoardingScreenItems.size
        },
        // take middle item as the initial page
        initialPage = onBoardingScreenItems.size / 2,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
    ) {
        HorizontalPager(
            pageSize = PageSize.Fill,
            state = pagerState,
        ) { index ->
            val item = onBoardingScreenItems[index]
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            SwipeAbleOnBoardingItemImage(
                image = item.image,
                imageHeight = screenHeight * 0.45f,
                modifier = Modifier,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SwipeAbleOnBoardingItemDescription(
                title = onBoardingScreenItems[pagerState.currentPage].title,
                description = onBoardingScreenItems[pagerState.currentPage].description,
            )
            Spacer(modifier = Modifier.height(20.dp))
            CustomDotIndicator(
                currentPage = pagerState.currentPage,
                numberOfPages = onBoardingScreenItems.size,
                pagerState = pagerState,
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 30.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = onCreateAccount,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(id = R.string.feature_signin_onboarding_create_account),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(id = R.string.feature_signin_onboarding_sign_in).uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
fun CustomDotIndicator(currentPage: Int, numberOfPages: Int, pagerState: PagerState) {
    val dotWidth = 8.dp
    val expandedDotWidth = 16.dp

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Page ${currentPage + 1} of $numberOfPages" },
    ) {
        repeat(numberOfPages) { index ->
            val width by animateDpAsState(
                when (index) {
                    currentPage -> lerp(
                        dotWidth,
                        expandedDotWidth,
                        1f - pagerState.currentPageOffsetFraction.absoluteValue,
                    )

                    (currentPage + 1) % numberOfPages -> lerp(
                        dotWidth,
                        expandedDotWidth,
                        pagerState.currentPageOffsetFraction.absoluteValue,
                    )

                    else -> dotWidth
                },
                label = "onBoarding_dot_width_animation",
            )
            val color by animateColorAsState(
                targetValue = if (index == currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                    stronglyDeemphasizedAlpha,
                ),
                label = "onBoarding_dot_color_animation",
            )
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(width = width, height = dotWidth)
                    .background(
                        color = color,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
fun SwipeAbleOnBoardingItemImage(
    @DrawableRes image: Int,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 400.dp,
) {
    Column(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight),
        ) {
            Image(
                painter = painterResource(id = image),
                contentDescription = "Onboarding illustration",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun SwipeAbleOnBoardingItemDescription(
    @StringRes title: Int,
    @StringRes description: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(id = title),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(id = description),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(
                    slightlyDeemphasizedAlpha,
                ),
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
fun PreviewSwipeAbleOnBoardingItem() {
    val item = onBoardingScreenItems[0]

    PaceDreamTheme {
        Column {
            SwipeAbleOnBoardingItemImage(
                image = item.image,
            )
            SwipeAbleOnBoardingItemDescription(
                title = item.title,
                description = item.description,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewOnBoardingScreen() {
    PaceDreamTheme {
        OnBoardingScreen(
            onCreateAccount = { },
            onSignIn = { },
        )
    }
}