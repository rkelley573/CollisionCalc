package com.example.collisioncalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import com.example.collisioncalc.data.*
import com.example.collisioncalc.ui.caseworkbook.CaseDetailScreen
import com.example.collisioncalc.ui.caseworkbook.CaseListScreen
import com.example.collisioncalc.ui.caseworkbook.CaseTab
import com.example.collisioncalc.ui.caseworkbook.NewCaseScreen
import com.example.collisioncalc.ui.screens.*
import com.example.collisioncalc.ui.theme.CollisionCalcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CollisionCalcTheme { AppRoot() } }
    }
}

private sealed class Screen {
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

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { CaseRepository(context, scope) }

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    // manual back stack
    val backStack = remember { mutableStateListOf<Screen>() }

    // remember last selected tab per case
    val caseLastTab = remember { mutableStateMapOf<CaseId, CaseTab>() }

    fun navigate(to: Screen, addToBackStack: Boolean = true) {
        if (addToBackStack) backStack.add(screen)
        screen = to
    }

    fun popBack(fallback: Screen = Screen.Home) {
        if (backStack.isNotEmpty()) {
            screen = backStack.removeAt(backStack.lastIndex)
        } else {
            screen = fallback
        }
    }

    val focusManager = LocalFocusManager.current

    // In-memory “case” for Quick Tools
    var quickToolsCase by remember {
        mutableStateOf(
            CaseFile(
                serviceNumber = "QUICK TOOLS",
                crashInfo = CollisionInfo(location = CrashLocation.NonIntersection()),
                vehicles = listOf(
                    Vehicle(label = "Vehicle 1"),
                    Vehicle(label = "Vehicle 2")
                ),
                units = emptyList(),
                calculations = emptyList()
            )
        )
    }

    fun saveQuickCalc(calc: SavedCalculation) {
        quickToolsCase = quickToolsCase.copy(
            calculations = quickToolsCase.calculations + calc
        )
    }

    fun findQuickCalc(calcId: CalcId): SavedCalculation? =
        quickToolsCase.calculations.firstOrNull { it.calcId == calcId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        when (val s = screen) {

            // ---------------- Home ----------------

            Screen.Home -> HomeScreen(
                onOpenUnitConverter = { navigate(Screen.UnitToolQuick) },
                onOpenMomentumQuick = { navigate(Screen.MomentumQuick) },
                onOpenTireCompare = { navigate(Screen.TireCompare) },
                onOpenCaseWorkbook = { navigate(Screen.CaseList) },
                onOpenQuickToolsCalcs = { navigate(Screen.QuickToolsCalcs) }
            )

            // ---------------- Quick Tools ----------------

            Screen.UnitToolQuick -> UnitConverterScreen(
                caseFile = quickToolsCase,
                onBack = { popBack() },
                onSaveCalculation = {
                    saveQuickCalc(it)
                    navigate(Screen.QuickToolsCalcs, addToBackStack = false)
                }
            )

            Screen.MomentumQuick -> MomentumWizardScreen(
                caseFile = quickToolsCase,
                onBack = { popBack() },
                onSaveCalculation = {
                    saveQuickCalc(it)
                    navigate(Screen.QuickToolsCalcs, addToBackStack = false)
                }
            )

            Screen.TireCompare -> TireSizeCompareScreen(
                onBack = { popBack() }
            )

            Screen.QuickToolsCalcs -> QuickToolsCalcsScreen(
                caseFile = quickToolsCase,
                onBack = { popBack() },
                onOpenCalc = { calcId -> navigate(Screen.QuickCalcDetail(calcId)) }
            )

            is Screen.QuickCalcDetail -> {
                val calc = findQuickCalc(s.calcId)
                if (calc == null) {
                    navigate(Screen.QuickToolsCalcs, addToBackStack = false)
                } else {
                    CalculationDetailScreen(
                        caseFile = quickToolsCase,
                        calcId = s.calcId,
                        onBack = { popBack() }
                    )
                }
            }

            // ---------------- Case Workbook ----------------

            Screen.CaseList -> CaseListScreen(
                cases = repo.caseSummaries,
                onBack = { popBack() },
                onNewCase = { navigate(Screen.NewCase) },
                onOpenCase = { caseId -> navigate(Screen.CaseDetail(caseId)) }
            )

            Screen.NewCase -> NewCaseScreen(
                onBack = { popBack() },
                onCreate = {
                    val c = repo.createCase(it)
                    navigate(Screen.CaseDetail(c.caseId), addToBackStack = false)
                }
            )

            is Screen.CaseDetail -> {
                // Load if missing; UI reads from Compose-observable cache
                LaunchedEffect(s.caseId) {
                    if (repo.cachedCase(s.caseId) == null) repo.loadCase(s.caseId)
                }
                val caseFile = repo.cachedCase(s.caseId)

                if (caseFile == null) {
                    LoadingScreen(onBack = { popBack(Screen.CaseList) })
                } else {
                    val rememberedTab = caseLastTab[s.caseId] ?: CaseTab.CRASH

                    CaseDetailScreen(
                        caseFile = caseFile,
                        onBack = { popBack() },

                        initialTab = rememberedTab,
                        onTabChanged = { t -> caseLastTab[s.caseId] = t },

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
                        onGoToUnitTools = { navigate(Screen.UnitToolsCase(s.caseId)) }
                    )
                }
            }

            is Screen.VehicleDetail -> {
                LaunchedEffect(s.caseId) {
                    if (repo.cachedCase(s.caseId) == null) repo.loadCase(s.caseId)
                }
                val caseFile = repo.cachedCase(s.caseId)

                if (caseFile == null) {
                    LoadingScreen(onBack = { popBack(Screen.CaseDetail(s.caseId)) })
                } else {
                    VehicleDetailScreen(
                        caseFile = caseFile,
                        vehicleId = s.vehicleId,
                        onBack = { popBack() },
                        onSaveVehicle = { repo.updateVehicle(s.caseId, it) },
                        onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                    )
                }
            }

            is Screen.PedestrianDetail -> {
                LaunchedEffect(s.caseId) {
                    if (repo.cachedCase(s.caseId) == null) repo.loadCase(s.caseId)
                }
                val caseFile = repo.cachedCase(s.caseId)

                if (caseFile == null) {
                    LoadingScreen(onBack = { popBack(Screen.CaseDetail(s.caseId)) })
                } else {
                    PedestrianDetailScreen(
                        caseFile = caseFile,
                        unitId = s.unitId,
                        onBack = { popBack() },
                        onSavePedestrian = { repo.updatePedestrianUnit(s.caseId, it) }
                    )
                }
            }

            is Screen.CombinedSpeed -> {
                LaunchedEffect(s.caseId) {
                    if (repo.cachedCase(s.caseId) == null) repo.loadCase(s.caseId)
                }
                val caseFile = repo.cachedCase(s.caseId)

                if (caseFile == null) {
                    LoadingScreen(onBack = { popBack(Screen.CaseDetail(s.caseId)) })
                } else {
                    CombinedSpeedScreen(
                        caseFile = caseFile,
                        onBack = { popBack() },
                        onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                    )
                }
            }

            is Screen.Momentum -> {
                LaunchedEffect(s.caseId) {
                    if (repo.cachedCase(s.caseId) == null) repo.loadCase(s.caseId)
                }
                val caseFile = repo.cachedCase(s.caseId)

                if (caseFile == null) {
                    LoadingScreen(onBack = { popBack(Screen.CaseDetail(s.caseId)) })
                } else {
                    MomentumWizardScreen(
                        caseFile = caseFile,
                        onBack = { popBack() },
                        onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                    )
                }
            }

            is Screen.UnitToolsCase -> {
                LaunchedEffect(s.caseId) {
                    if (repo.cachedCase(s.caseId) == null) repo.loadCase(s.caseId)
                }
                val caseFile = repo.cachedCase(s.caseId)

                if (caseFile == null) {
                    LoadingScreen(onBack = { popBack(Screen.CaseDetail(s.caseId)) })
                } else {
                    UnitConverterScreen(
                        caseFile = caseFile,
                        onBack = { popBack() },
                        onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                    )
                }
            }

            is Screen.TireCompareCase -> {
                LaunchedEffect(s.caseId) {
                    if (repo.cachedCase(s.caseId) == null) repo.loadCase(s.caseId)
                }
                val caseFile = repo.cachedCase(s.caseId)

                if (caseFile == null) {
                    LoadingScreen(onBack = { popBack(Screen.CaseDetail(s.caseId)) })
                } else {
                    TireSizeCompareScreen(
                        caseFile = caseFile,
                        defaultVehicleId = caseFile.vehicles.firstOrNull()?.vehicleId,
                        onBack = { popBack() },
                        onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                    )
                }
            }

            is Screen.CalculationDetail -> {
                LaunchedEffect(s.caseId) {
                    if (repo.cachedCase(s.caseId) == null) repo.loadCase(s.caseId)
                }
                val caseFile = repo.cachedCase(s.caseId)
                val exists = caseFile?.calculations?.any { it.calcId == s.calcId } == true

                if (caseFile == null) {
                    LoadingScreen(onBack = { popBack(Screen.CaseDetail(s.caseId)) })
                } else if (!exists) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingScreen(onBack: () -> Unit) {
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
