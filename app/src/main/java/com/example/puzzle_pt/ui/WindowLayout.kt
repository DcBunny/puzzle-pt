package com.example.puzzle_pt.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun isTabletLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 600 &&
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenWidthDp >= 840
}
