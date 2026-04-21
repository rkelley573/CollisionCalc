package com.collisioncalc.app.ui.caseworkbook

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.collisioncalc.app.data.CollisionInfo

@Composable
fun CrashTabContent(
    crashInfo: CollisionInfo,
    onUpdateCrashInfo: (CollisionInfo) -> Unit
) {
    CrashTabScreen(
        crashInfo = crashInfo,
        onCrashInfoChange = onUpdateCrashInfo,
        modifier = Modifier
    )
}
