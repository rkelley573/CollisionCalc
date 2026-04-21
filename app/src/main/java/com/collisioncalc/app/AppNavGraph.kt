package com.collisioncalc.app

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.collisioncalc.app.data.CalcId
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.CaseId
import com.collisioncalc.app.data.UnitId
import com.collisioncalc.app.data.VehicleId
import com.collisioncalc.app.ui.caseworkbook.CaseDetailScreen
import com.collisioncalc.app.ui.caseworkbook.CaseListScreen
import com.collisioncalc.app.ui.caseworkbook.CaseTab
import com.collisioncalc.app.ui.caseworkbook.NewCaseScreen
import com.collisioncalc.app.ui.screens.CalculationDetailScreen
import com.collisioncalc.app.ui.screens.CombinedSpeedScreen
import com.collisioncalc.app.ui.screens.HomeScreen
import com.collisioncalc.app.ui.screens.MomentumWizardScreen
import com.collisioncalc.app.ui.screens.PedestrianDetailScreen
import com.collisioncalc.app.ui.screens.QuickToolsCalcsScreen
import com.collisioncalc.app.ui.screens.TireSizeCompareScreen
import com.collisioncalc.app.ui.screens.UnitConverterScreen
import com.collisioncalc.app.ui.screens.VehicleDetailScreen

// ---- Screen definitions ----

sealed class Screen {
    data object Home : Screen()

    // Quick tools
    data object UnitToolQuick : Screen()
    data object MomentumQuick : Screen()
    data object TireCompare : Screen()
    data object QuickToolsCalcs : Screen()
    data class QuickCalcDetail(val calcId: CalcId) : Screen()

    // Case workflow
    data object CaseList : Screen()
    data object NewCase : Screen()
    data class CaseDetail(val caseId: CaseId) : Screen()
    data class VehicleDetail(val caseId: CaseId, val vehicleId: VehicleId) : Screen()
    data class PedestrianDetail(val caseId: CaseId, val unitId: UnitId) : Screen()
    data class CombinedSpeed(val caseId: CaseId) : Screen()
    data class Momentum(val caseId: CaseId) : Screen()
    data class TireCompareCase(val caseId: CaseId) : Screen()
    data class CalculationDetail(val caseId: CaseId, val calcId: CalcId) : Screen()
    data class UnitToolsCase(val caseId: CaseId) : Screen()
}

// ---- Shared case-loading helper ----

/**
 * Loads a case by ID if not already cached, then calls [content] with it.
 * Shows [LoadingScreen] while loading, eliminating the repeated
 * LaunchedEffect + null-check pattern that was duplicated across 8 screens.
 */
@Composable
fun WithCase(
    caseId: CaseId,
    vm: AppViewModel,
    onBack: () -> Unit,
    content: @Composable (CaseFile) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(caseId) {
        if (vm.repo.cachedCase(caseId) == null) vm.repo.loadCase(caseId)
    }
    val caseFile = vm.repo.cachedCase(caseId)
    if (caseFile == null) {
        LoadingScreen(onBack = onBack)
    } else {
        content(caseFile)
    }
}

// ---- Root nav graph ----

@Composable
fun AppNavGraph(vm: AppViewModel) {
    val repo = vm.repo

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val backStack = remember { mutableStateListOf<Screen>() }

    fun navigate(to: Screen, addToBackStack: Boolean = true) {
        if (addToBackStack) backStack.add(screen)
        screen = to
    }

    fun popBack(fallback: Screen = Screen.Home) {
        screen = if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) else fallback
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        when (val s = screen) {

            Screen.Home -> HomeScreen(
                onOpenUnitConverter = { navigate(Screen.UnitToolQuick) },
                onOpenMomentumQuick = { navigate(Screen.MomentumQuick) },
                onOpenTireCompare = { navigate(Screen.TireCompare) },
                onOpenCaseWorkbook = { navigate(Screen.CaseList) },
                onOpenQuickToolsCalcs = { navigate(Screen.QuickToolsCalcs) }
            )

            Screen.UnitToolQuick -> UnitConverterScreen(
                caseFile = vm.quickToolsCase,
                onBack = { popBack() },
                onSaveCalculation = {
                    vm.saveQuickCalc(it)
                    navigate(Screen.QuickToolsCalcs, addToBackStack = false)
                }
            )

            Screen.MomentumQuick -> MomentumWizardScreen(
                caseFile = vm.quickToolsCase,
                onBack = { popBack() },
                onSaveCalculation = {
                    vm.saveQuickCalc(it)
                    navigate(Screen.QuickToolsCalcs, addToBackStack = false)
                }
            )

            Screen.TireCompare -> TireSizeCompareScreen(onBack = { popBack() })

            Screen.QuickToolsCalcs -> QuickToolsCalcsScreen(
                caseFile = vm.quickToolsCase,
                onBack = { popBack() },
                onOpenCalc = { calcId -> navigate(Screen.QuickCalcDetail(calcId)) }
            )

            is Screen.QuickCalcDetail -> {
                val calc = vm.findQuickCalc(s.calcId)
                if (calc == null) {
                    navigate(Screen.QuickToolsCalcs, addToBackStack = false)
                } else {
                    CalculationDetailScreen(
                        caseFile = vm.quickToolsCase,
                        calcId = s.calcId,
                        onBack = { popBack() }
                    )
                }
            }

            Screen.CaseList -> CaseListScreen(
                cases = repo.caseSummaries,
                onBack = { popBack() },
                onNewCase = { navigate(Screen.NewCase) },
                onOpenCase = { caseId -> navigate(Screen.CaseDetail(caseId)) },
                onDeleteCase = { caseId -> repo.deleteCase(caseId) }
            )

            Screen.NewCase -> NewCaseScreen(
                onBack = { popBack() },
                onCreate = {
                    val c = repo.createCase(it)
                    navigate(Screen.CaseDetail(c.caseId), addToBackStack = false)
                }
            )

            is Screen.CaseDetail -> WithCase(s.caseId, vm, onBack = { popBack(Screen.CaseList) }) { caseFile ->
                CaseDetailScreen(
                    caseFile = caseFile,
                    onBack = { popBack() },
                    initialTab = vm.getLastTab(s.caseId),
                    onTabChanged = { t -> vm.setLastTab(s.caseId, t) },
                    onUpdateCrashInfo = { repo.updateCrashInfo(s.caseId, it) },
                    onAddVehicleUnit = { repo.addVehicleUnit(s.caseId) },
                    onAddPedestrianUnit = { repo.addPedestrianUnit(s.caseId) },
                    onRenameUnit = { id, label -> repo.renameUnit(s.caseId, id, label) },
                    onRemoveUnit = { unitId -> repo.removeUnit(s.caseId, unitId) },
                    onAddNote = { repo.addNote(s.caseId, it) },
                    onOpenVehicle = { vehicleId -> navigate(Screen.VehicleDetail(s.caseId, vehicleId)) },
                    onOpenPedestrian = { unitId -> navigate(Screen.PedestrianDetail(s.caseId, unitId)) },
                    onGoToCombinedSpeed = { navigate(Screen.CombinedSpeed(s.caseId)) },
                    onGoToMomentum = { navigate(Screen.Momentum(s.caseId)) },
                    onOpenCalculation = { calcId -> navigate(Screen.CalculationDetail(s.caseId, calcId)) },
                    onSaveCalculation = { repo.saveCalculation(s.caseId, it) },
                    onGoToUnitTools = { navigate(Screen.UnitToolsCase(s.caseId)) },
                    onBeforeExport = { repo.flushNow(s.caseId) }
                )
            }

            is Screen.VehicleDetail -> WithCase(s.caseId, vm, onBack = { popBack(Screen.CaseDetail(s.caseId)) }) { caseFile ->
                VehicleDetailScreen(
                    caseFile = caseFile,
                    vehicleId = s.vehicleId,
                    onBack = { popBack() },
                    onSaveVehicle = { repo.updateVehicle(s.caseId, it) },
                    onSaveCalculation = { repo.saveCalculation(s.caseId, it) },
                    vinDecoderProvider = vm.vinDecoder
                )
            }

            is Screen.PedestrianDetail -> WithCase(s.caseId, vm, onBack = { popBack(Screen.CaseDetail(s.caseId)) }) { caseFile ->
                PedestrianDetailScreen(
                    caseFile = caseFile,
                    unitId = s.unitId,
                    onBack = { popBack() },
                    onSavePedestrian = { repo.updatePedestrianUnit(s.caseId, it) }
                )
            }

            is Screen.CombinedSpeed -> WithCase(s.caseId, vm, onBack = { popBack(Screen.CaseDetail(s.caseId)) }) { caseFile ->
                CombinedSpeedScreen(
                    caseFile = caseFile,
                    onBack = { popBack() },
                    onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                )
            }

            is Screen.Momentum -> WithCase(s.caseId, vm, onBack = { popBack(Screen.CaseDetail(s.caseId)) }) { caseFile ->
                MomentumWizardScreen(
                    caseFile = caseFile,
                    onBack = { popBack() },
                    onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                )
            }

            is Screen.UnitToolsCase -> WithCase(s.caseId, vm, onBack = { popBack(Screen.CaseDetail(s.caseId)) }) { caseFile ->
                UnitConverterScreen(
                    caseFile = caseFile,
                    onBack = { popBack() },
                    onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                )
            }

            is Screen.TireCompareCase -> WithCase(s.caseId, vm, onBack = { popBack(Screen.CaseDetail(s.caseId)) }) { caseFile ->
                TireSizeCompareScreen(
                    caseFile = caseFile,
                    defaultVehicleId = caseFile.vehicles.firstOrNull()?.vehicleId,
                    onBack = { popBack() },
                    onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                )
            }

            is Screen.CalculationDetail -> WithCase(s.caseId, vm, onBack = { popBack(Screen.CaseDetail(s.caseId)) }) { caseFile ->
                val exists = caseFile.calculations.any { it.calcId == s.calcId }
                if (!exists) {
                    navigate(Screen.CaseDetail(s.caseId), addToBackStack = false)
                } else {
                    CalculationDetailScreen(
                        caseFile = caseFile,
                        calcId = s.calcId,
                        onBack = { popBack() }
                    )
                }
            }
        }
    }
}

// ---- Loading screen ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loading…") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}