package com.collisioncalc.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.collisioncalc.app.data.CalcId
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.CaseId
import com.collisioncalc.app.data.CaseRepository
import com.collisioncalc.app.data.CollisionInfo
import com.collisioncalc.app.data.CrashLocation
import com.collisioncalc.app.data.SavedCalculation
import com.collisioncalc.app.data.Vehicle
import com.collisioncalc.app.data.lookups.VinDecoder
import com.collisioncalc.app.ui.caseworkbook.CaseTab
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    val repo: CaseRepository,
    val vinDecoder: VinDecoder
) : ViewModel() {

    var quickToolsCase by mutableStateOf(
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
        private set

    fun saveQuickCalc(calc: SavedCalculation) {
        quickToolsCase = quickToolsCase.copy(
            calculations = quickToolsCase.calculations + calc
        )
    }

    fun findQuickCalc(calcId: CalcId): SavedCalculation? =
        quickToolsCase.calculations.firstOrNull { it.calcId == calcId }

    private val caseLastTab = mutableStateMapOf<CaseId, CaseTab>()

    fun getLastTab(caseId: CaseId): CaseTab = caseLastTab[caseId] ?: CaseTab.CRASH

    fun setLastTab(caseId: CaseId, tab: CaseTab) {
        caseLastTab[caseId] = tab
    }
}