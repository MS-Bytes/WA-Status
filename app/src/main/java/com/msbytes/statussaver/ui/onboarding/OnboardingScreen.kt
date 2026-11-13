package com.msbytes.statussaver.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.msbytes.statussaver.R
import com.msbytes.statussaver.ui.components.AppLogo
import kotlinx.coroutines.launch

private data class Page(val icon: ImageVector, val title: Int, val body: Int)

private val pages = listOf(
    Page(Icons.Rounded.Download, R.string.onboarding_1_title, R.string.onboarding_1_body),
    Page(Icons.Rounded.AutoFixHigh, R.string.onboarding_2_title, R.string.onboarding_2_body),
    Page(Icons.Rounded.Shield, R.string.onboarding_3_title, R.string.onboarding_3_body),
    Page(Icons.Rounded.FolderOpen, R.string.onboarding_4_title, R.string.onboarding_4_body),
)

/** 4-page intro. No ads are ever shown here. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit, onOpenPrivacyPolicy: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppLogo(40.dp)
            Text(
                stringResource(R.string.app_short_name),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            if (!isLast) TextButton(onClick = onFinish) { Text(stringResource(R.string.action_skip)) }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(200.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                            ),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(page.icon, stringResource(R.string.cd_illustration), Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(32.dp))
                Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 480.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.works_with),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { i ->
                val selected = i == pagerState.currentPage
                val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dotWidth")
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    label = "dotColor",
                )
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(width)
                        .background(color, RoundedCornerShape(4.dp)),
                )
            }
        }

        Button(
            onClick = { if (isLast) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(stringResource(if (isLast) R.string.action_get_started else R.string.action_next))
        }
        TextButton(onClick = onOpenPrivacyPolicy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
                stringResource(R.string.onboarding_privacy_notice),
                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                textAlign = TextAlign.Center,
            )
        }
        Text(
            stringResource(R.string.disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
