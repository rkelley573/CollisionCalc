package com.collisioncalc.app.ui.caseworkbook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.collisioncalc.app.data.CalcId
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.CollisionInfo
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.data.UnitId
import com.collisioncalc.app.data.VehicleId

enum class CaseTab { CRASH, UNITS, CALCS, NOTES, EXPORT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    caseFile: CaseFile,
    onBack: () -> Unit,

    // tab restore support (UI-only)
    initialTab: CaseTab = CaseTab.CRASH,
    onTabChanged: (CaseTab) -> Unit = {},

    // Phase 1
    onUpdateCrashInfo: (CollisionInfo) -> Unit,

    // Units
    onAddVehicleUnit: () -> Unit,
    onAddPedestrianUnit: () -> Unit,
    onRenameUnit: (UnitId, String) -> Unit,
    onRemoveUnit: (UnitId) -> Unit,

    // Notes / vehicle edit
    onAddNote: (String) -> Unit,
    onOpenVehicle: (vehicleId: VehicleId) -> Unit,
    onOpenPedestrian: (unitId: UnitId) -> Unit,

    // Tools
    onGoToCombinedSpeed: () -> Unit,
    onGoToMomentum: () -> Unit,
    onGoToUnitTools: () -> Unit,

    // Calcs
    onOpenCalculation: (calcId: CalcId) -> Unit,

    // Save calc into this case (used by in-case Unit Tools)
    onSaveCalculation: (SavedCalculation) -> Unit,

    // NEW: flush/persist hook (called right before export)
    onBeforeExport: () -> Unit
) {
    var tab by remember(caseFile.caseId) { mutableStateOf(initialTab) }
    var showUnitTools by remember(caseFile.caseId) { mutableStateOf(false) }

    LaunchedEffect(caseFile.caseId, initialTab) {
        tab = initialTab
        onTabChanged(initialTab)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Case: ${caseFile.serviceNumber}") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->

        if (showUnitTools) {
            CaseUnitToolsBottomSheet(
                caseFile = caseFile,
                onDismiss = { showUnitTools = false },
                onSaveCalculation = onSaveCalculation
            )
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = tab.ordinal) {
                CaseTab.entries.forEach { t ->
                    Tab(
                        selected = tab == t,
                        onClick = {
                            tab = t
                            onTabChanged(t)
                        },
                        text = { Text(t.name) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    CaseTab.CRASH -> CrashTabContent(
                        crashInfo = caseFile.crashInfo,
                        onUpdateCrashInfo = onUpdateCrashInfo
                    )

                    CaseTab.UNITS -> UnitsTabContent(
                        caseFile = caseFile,
                        onOpenVehicle = onOpenVehicle,
                        onOpenPedestrian = onOpenPedestrian,
                        onAddVehicleUnit = onAddVehicleUnit,
                        onAddPedestrianUnit = onAddPedestrianUnit,
                        onRenameUnit = onRenameUnit,
                        onRemoveUnit = onRemoveUnit
                    )

                    CaseTab.CALCS -> CalcsTabContent(
                        caseFile = caseFile,
                        onOpenCalculation = onOpenCalculation,
                        onGoToCombinedSpeed = onGoToCombinedSpeed,
                        onGoToMomentum = onGoToMomentum,
                        onOpenUnitTools = onGoToUnitTools
                    )

                    CaseTab.NOTES -> NotesTabContent(
                        caseFile = caseFile,
                        onAddNote = onAddNote
                    )

                    CaseTab.EXPORT -> ExportTabContent(
                        caseFile = caseFile,
                        onBeforeExport = onBeforeExport
                    )
                }
            }
        }
    }
}
