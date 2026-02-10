package com.example.collisioncalc.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.collisioncalc.data.CaseFile
import com.example.collisioncalc.data.Vehicle

/**
 * Quick tool wrapper: lets you use the Momentum Wizard without creating a case.
 * Uses a temporary CaseFile with Vehicle 1 & Vehicle 2. Does NOT save.
 */
@Composable
fun MomentumQuickScreen(
    onBack: () -> Unit
) {
    val tempCase = remember {
        CaseFile(
            serviceNumber = "Quick Momentum",
            vehicles = listOf(
                Vehicle(label = "Vehicle 1"),
                Vehicle(label = "Vehicle 2")
            )
        )
    }

    MomentumWizardScreen(
        caseFile = tempCase,
        onBack = onBack,
        onSaveCalculation = { /* no-op in quick mode */ }
    )
}
