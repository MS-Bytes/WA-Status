package com.msbytes.statussaver.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.msbytes.statussaver.R
import com.msbytes.statussaver.ui.theme.BrandIndigo
import com.msbytes.statussaver.ui.theme.BrandIndigoDark

/** The app icon drawn in Compose (adaptive mipmaps can't be loaded by painterResource). */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(BrandIndigo, BrandIndigoDark))),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.cd_app_logo),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
