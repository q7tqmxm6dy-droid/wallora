package com.wallora.app.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wallora.app.R
import kotlinx.coroutines.launch

private data class OnboardPage(
    val title: String,
    val body: String,
    @get:DrawableRes val image: Int,
)

/** First-run tour, shown once. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        OnboardPage(
            title = stringResource(R.string.onb1_title),
            body = stringResource(R.string.onb1_body),
            image = R.drawable.wp_01_light,
        ),
        OnboardPage(
            title = stringResource(R.string.onb2_title),
            body = stringResource(R.string.onb2_body),
            image = R.drawable.wp_21_light,
        ),
        OnboardPage(
            title = stringResource(R.string.onb3_title),
            body = stringResource(R.string.onb3_body),
            image = R.drawable.posters_p24_light,
        ),
    )

    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    ),
                ),
            ),
    ) {
        TextButton(
            onClick = onDone,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Text(stringResource(R.string.skip), color = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(page.image),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 190.dp, height = 338.dp)
                            .clip(RoundedCornerShape(22.dp)),
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = page.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.45f),
                            ),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (isLast) onDone()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(if (isLast) R.string.get_started else R.string.next))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
