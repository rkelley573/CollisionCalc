package com.example.collisioncalc.ui.caseworkbook

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.collisioncalc.data.CaseFile
import com.example.collisioncalc.data.UnitId
import com.example.collisioncalc.data.VehicleId

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
