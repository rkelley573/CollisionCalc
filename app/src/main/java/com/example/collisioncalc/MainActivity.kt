package com.example.collisioncalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.collisioncalc.data.*
import com.example.collisioncalc.ui.caseworkbook.CaseDetailScreen
import com.example.collisioncalc.ui.caseworkbook.CaseListScreen
import com.example.collisioncalc.ui.caseworkbook.NewCaseScreen
import com.example.collisioncalc.ui.screens.*
import com.example.collisioncalc.ui.theme.CollisionCalcTheme
import kotlin.math.abs

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
    data class PedestrianDetail(val caseId: CaseId, val unitId: UnitId) : Screen() // NEW
    data class CombinedSpeed(val caseId: CaseId) : Screen()
    data class Momentum(val caseId: CaseId) : Screen()
    data class TireCompareCase(val caseId: CaseId) : Screen()
    data class CalculationDetail(val caseId: CaseId, val calcId: CalcId) : Screen()
}

@Composable
private fun AppRoot() {
    val repo = remember { CaseRepository() }
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
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
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
                onOpenCalc = { screen = Screen.QuickCalcDetail(it) }
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
                cases = repo.cases,
                onBack = { screen = Screen.Home },
                onNewCase = { screen = Screen.NewCase },
                onOpenCase = { screen = Screen.CaseDetail(it) }
            )

            Screen.NewCase -> NewCaseScreen(
                onBack = { screen = Screen.CaseList },
                onCreate = {
                    val c = repo.createCase(it)
                    screen = Screen.CaseDetail(c.caseId)
                }
            )

            is Screen.CaseDetail -> {
                val caseFile = repo.getCase(s.caseId)
                if (caseFile == null) {
                    screen = Screen.CaseList
                } else {
                    CaseDetailScreen(
                        caseFile = caseFile,
                        onBack = { screen = Screen.CaseList },

                        // Crash tab
                        onUpdateCrashInfo = { repo.updateCrashInfo(caseFile.caseId, it) },

                        // Units tab
                        onAddVehicleUnit = { repo.addVehicleUnit(caseFile.caseId) },
                        onAddPedestrianUnit = { repo.addPedestrianUnit(caseFile.caseId) },
                        onRenameUnit = { id, label -> repo.renameUnit(caseFile.caseId, id, label) },
                        onRemoveUnit = { unitId -> repo.removeUnit(caseFile.caseId, unitId) },

                        // Notes / vehicle edit
                        onAddNote = { repo.addNote(caseFile.caseId, it) },
                        onOpenVehicle = { screen = Screen.VehicleDetail(caseFile.caseId, it) },
                        onOpenPedestrian = { unitId -> screen = Screen.PedestrianDetail(caseFile.caseId, unitId) }, // NEW

                        // Tools (these remain their own screens)
                        onGoToCombinedSpeed = { screen = Screen.CombinedSpeed(caseFile.caseId) },
                        onGoToMomentum = { screen = Screen.Momentum(caseFile.caseId) },

                        // Calcs
                        onOpenCalculation = { screen = Screen.CalculationDetail(caseFile.caseId, it) },

                        // Used by in-case Unit Tools sheet
                        onSaveCalculation = { repo.saveCalculation(caseFile.caseId, it) }
                    )
                }
            }

            is Screen.VehicleDetail -> {
                val caseFile = repo.getCase(s.caseId)
                if (caseFile == null) {
                    screen = Screen.CaseList
                } else {
                    VehicleDetailScreen(
                        caseFile = caseFile,
                        vehicleId = s.vehicleId,
                        onBack = { screen = Screen.CaseDetail(caseFile.caseId) },
                        onSaveVehicle = { repo.updateVehicle(caseFile.caseId, it) },
                        onSaveCalculation = { repo.saveCalculation(caseFile.caseId, it) }
                    )
                }
            }

            is Screen.PedestrianDetail -> {
                val caseFile = repo.getCase(s.caseId)
                if (caseFile == null) {
                    screen = Screen.CaseList
                } else {
                    PedestrianDetailScreen(
                        caseFile = caseFile,
                        unitId = s.unitId,
                        onBack = { screen = Screen.CaseDetail(caseFile.caseId) },
                        onSavePedestrian = { repo.updatePedestrianUnit(caseFile.caseId, it) }
                    )
                }
            }

            is Screen.CombinedSpeed -> {
                val caseFile = repo.getCase(s.caseId)
                if (caseFile == null) {
                    screen = Screen.CaseList
                } else {
                    CombinedSpeedScreen(
                        caseFile = caseFile,
                        onBack = { screen = Screen.CaseDetail(caseFile.caseId) },
                        onSaveCalculation = { repo.saveCalculation(caseFile.caseId, it) }
                    )
                }
            }

            is Screen.Momentum -> {
                val caseFile = repo.getCase(s.caseId)
                if (caseFile == null) {
                    screen = Screen.CaseList
                } else {
                    MomentumWizardScreen(
                        caseFile = caseFile,
                        onBack = { screen = Screen.CaseDetail(caseFile.caseId) },
                        onSaveCalculation = { repo.saveCalculation(caseFile.caseId, it) }
                    )
                }
            }

            is Screen.TireCompareCase -> {
                val caseFile = repo.getCase(s.caseId)
                if (caseFile == null) {
                    screen = Screen.CaseList
                } else {
                    TireSizeCompareScreen(
                        caseFile = caseFile,
                        defaultVehicleId = caseFile.vehicles.firstOrNull()?.vehicleId,
                        onBack = { screen = Screen.CaseDetail(caseFile.caseId) },
                        onSaveCalculation = { repo.saveCalculation(caseFile.caseId, it) }
                    )
                }
            }

            is Screen.CalculationDetail -> {
                val caseFile = repo.getCase(s.caseId)
                val exists = caseFile?.calculations?.any { it.calcId == s.calcId } == true

                if (caseFile == null || !exists) {
                    screen = Screen.CaseDetail(s.caseId)
                } else {
                    CalculationDetailScreen(
                        caseFile = caseFile,
                        calcId = s.calcId,
                        onBack = { screen = Screen.CaseDetail(caseFile.caseId) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickToolsCalcsScreen(
    caseFile: CaseFile,
    onBack: () -> Unit,
    onOpenCalc: (CalcId) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Tools — Saved Calcs") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "Quick Tools mode: saved calculations are not tied to a case or unit.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (caseFile.calculations.isEmpty()) {
                Text("No saved calculations yet.")
            } else {
                caseFile.calculations
                    .sortedByDescending { it.createdAtEpochMs }
                    .forEach { c ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpenCalc(c.calcId) }
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(c.title, style = MaterialTheme.typography.titleSmall)
                                c.outputs.firstOrNull()?.let {
                                    Text("${it.name} = ${format3(it.value)} ${it.unit}".trim())
                                }
                            }
                        }
                    }
            }
        }
    }
}

private fun format3(x: Double): String =
    "%.3f".format(abs(x)).trimEnd('0').trimEnd('.')
