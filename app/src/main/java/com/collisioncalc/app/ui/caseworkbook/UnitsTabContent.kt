package com.collisioncalc.app.ui.caseworkbook

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.collisioncalc.app.data.CaseFile
import com.collisioncalc.app.data.UnitId
import com.collisioncalc.app.data.VehicleId

@Composable
fun UnitsTabContent(
    caseFile: CaseFile,
    onOpenVehicle: (VehicleId) -> Unit,
    onOpenPedestrian: (UnitId) -> Unit,
    onAddVehicleUnit: () -> Unit,
    onAddPedestrianUnit: () -> Unit,
    onRenameUnit: (UnitId, String) -> Unit,
    onRemoveUnit: (UnitId) -> Unit
) {
    UnitsTabScreen(
        caseFile = caseFile,
        onOpenVehicle = onOpenVehicle,
        onOpenPedestrian = onOpenPedestrian,
        onAddVehicleUnit = onAddVehicleUnit,
        onAddPedestrianUnit = onAddPedestrianUnit,
        onRenameUnit = onRenameUnit,
        onRemoveUnit = onRemoveUnit,
        modifier = Modifier
    )
}
