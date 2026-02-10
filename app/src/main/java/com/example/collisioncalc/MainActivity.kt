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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { CaseRepository(context, scope) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
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
                onOpenUnitConverter = { screen = Screen.UnitToolQuick },
                onOpenMomentumQuick = { screen = Screen.MomentumQuick },
                onOpenTireCompare = { screen = Screen.TireCompare },
                onOpenCaseWorkbook = { screen = Screen.CaseList },
                onOpenQuickToolsCalcs = { screen = Screen.QuickToolsCalcs }
            )

            // ---------------- Quick Tools ----------------

            Screen.UnitToolQuick -> UnitConverterScreen(
                caseFile = quickToolsCase,
                onBack = { screen = Screen.Home },
                onSaveCalculation = {
                    saveQuickCalc(it)
                    screen = Screen.QuickToolsCalcs
                }
            )

            Screen.MomentumQuick -> MomentumWizardScreen(
                caseFile = quickToolsCase,
                onBack = { screen = Screen.Home },
                onSaveCalculation = {
                    saveQuickCalc(it)
                    screen = Screen.QuickToolsCalcs
                }
            )

            Screen.TireCompare -> TireSizeCompareScreen(
                onBack = { screen = Screen.Home }
            )

            Screen.QuickToolsCalcs -> QuickToolsCalcsScreen(
                caseFile = quickToolsCase,
                onBack = { screen = Screen.Home },
                onOpenCalc = { calcId -> screen = Screen.QuickCalcDetail(calcId) } // ✅ fixed "it"
            )

            is Screen.QuickCalcDetail -> {
                val calc = findQuickCalc(s.calcId)
                if (calc == null) {
                    screen = Screen.QuickToolsCalcs
                } else {
                    CalculationDetailScreen(
                        caseFile = quickToolsCase,
                        calcId = s.calcId,
                        onBack = { screen = Screen.QuickToolsCalcs }
                    )
                }
            }

            // ---------------- Case Workbook ----------------

            Screen.CaseList -> CaseListScreen(
                cases = repo.caseSummaries,
                onBack = { screen = Screen.Home },
                onNewCase = { screen = Screen.NewCase },
                onOpenCase = { caseId -> screen = Screen.CaseDetail(caseId) }
            )

            Screen.NewCase -> NewCaseScreen(
                onBack = { screen = Screen.CaseList },
                onCreate = {
                    val c = repo.createCase(it)
                    screen = Screen.CaseDetail(c.caseId)
                }
            )

            is Screen.CaseDetail -> {
                val caseFile by produceState<CaseFile?>(initialValue = null, s.caseId) {
                    value = repo.loadCase(s.caseId)
                }
                if (caseFile == null) {
                    LoadingScreen(onBack = { screen = Screen.CaseList })
                } else {
                    CaseDetailScreen(
                        caseFile = caseFile!!,
                        onBack = { screen = Screen.CaseList },

                        onUpdateCrashInfo = { repo.updateCrashInfo(s.caseId, it) },

                        onAddVehicleUnit = { repo.addVehicleUnit(s.caseId) },
                        onAddPedestrianUnit = { repo.addPedestrianUnit(s.caseId) },
                        onRenameUnit = { id, label -> repo.renameUnit(s.caseId, id, label) },
                        onRemoveUnit = { unitId -> repo.removeUnit(s.caseId, unitId) },

                        onAddNote = { repo.addNote(s.caseId, it) },
                        onOpenVehicle = { vehicleId -> screen = Screen.VehicleDetail(s.caseId, vehicleId) },
                        onOpenPedestrian = { unitId -> screen = Screen.PedestrianDetail(s.caseId, unitId) },

                        onGoToCombinedSpeed = { screen = Screen.CombinedSpeed(s.caseId) },
                        onGoToMomentum = { screen = Screen.Momentum(s.caseId) },

                        onOpenCalculation = { calcId -> screen = Screen.CalculationDetail(s.caseId, calcId) },

                        onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                    )
                }
            }

            is Screen.VehicleDetail -> {
                val caseFile by produceState<CaseFile?>(initialValue = null, s.caseId) {
                    value = repo.loadCase(s.caseId)
                }
                if (caseFile == null) {
                    LoadingScreen(onBack = { screen = Screen.CaseDetail(s.caseId) })
                } else {
                    VehicleDetailScreen(
                        caseFile = caseFile!!,
                        vehicleId = s.vehicleId,
                        onBack = { screen = Screen.CaseDetail(s.caseId) },
                        onSaveVehicle = { repo.updateVehicle(s.caseId, it) },
                        onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                    )
                }
            }

            is Screen.PedestrianDetail -> {
                val caseFile by produceState<CaseFile?>(initialValue = null, s.caseId) {
                    value = repo.loadCase(s.caseId)
                }
                if (caseFile == null) {
                    LoadingScreen(onBack = { screen = Screen.CaseDetail(s.caseId) })
                } else {
                    PedestrianDetailScreen(
                        caseFile = caseFile!!,
                        unitId = s.unitId,
                        onBack = { screen = Screen.CaseDetail(s.caseId) },
                        onSavePedestrian = { repo.updatePedestrianUnit(s.caseId, it) }
                    )
                }
            }

            is Screen.CombinedSpeed -> {
                val caseFile by produceState<CaseFile?>(initialValue = null, s.caseId) {
                    value = repo.loadCase(s.caseId)
                }
                if (caseFile == null) {
                    LoadingScreen(onBack = { screen = Screen.CaseDetail(s.caseId) })
                } else {
                    CombinedSpeedScreen(
                        caseFile = caseFile!!,
                        onBack = { screen = Screen.CaseDetail(s.caseId) },
                        onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                    )
                }
            }

            is Screen.Momentum -> {
                val caseFile by produceState<CaseFile?>(initialValue = null, s.caseId) {
                    value = repo.loadCase(s.caseId)
                }
                if (caseFile == null) {
                    LoadingScreen(onBack = { screen = Screen.CaseDetail(s.caseId) })
                } else {
                    MomentumWizardScreen(
                        caseFile = caseFile!!,
                        onBack = { screen = Screen.CaseDetail(s.caseId) },
                        onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                    )
                }
            }

            is Screen.TireCompareCase -> {
                val caseFile by produceState<CaseFile?>(initialValue = null, s.caseId) {
                    value = repo.loadCase(s.caseId)
                }
                if (caseFile == null) {
                    LoadingScreen(onBack = { screen = Screen.CaseDetail(s.caseId) })
                } else {
                    TireSizeCompareScreen(
                        caseFile = caseFile!!,
                        defaultVehicleId = caseFile!!.vehicles.firstOrNull()?.vehicleId,
                        onBack = { screen = Screen.CaseDetail(s.caseId) },
                        onSaveCalculation = { repo.saveCalculation(s.caseId, it) }
                    )
                }
            }

            is Screen.CalculationDetail -> {
                val caseFile by produceState<CaseFile?>(initialValue = null, s.caseId) {
                    value = repo.loadCase(s.caseId)
                }
                val exists = caseFile?.calculations?.any { it.calcId == s.calcId } == true

                if (caseFile == null) {
                    LoadingScreen(onBack = { screen = Screen.CaseDetail(s.caseId) })
                } else if (!exists) {
                    screen = Screen.CaseDetail(s.caseId)
                } else {
                    CalculationDetailScreen(
                        caseFile = caseFile!!,
                        calcId = s.calcId,
                        onBack = { screen = Screen.CaseDetail(s.caseId) }
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
