package com.example.collisioncalc.ui.caseworkbook

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.collisioncalc.data.CollisionInfo

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
