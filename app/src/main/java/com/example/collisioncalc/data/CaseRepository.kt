package com.example.collisioncalc.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.example.collisioncalc.data.db.CollisionCalcDatabase
import com.example.collisioncalc.data.db.DbMappers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.UUID

class CaseRepository(
    context: Context,
    private val scope: CoroutineScope
) {
    private val db = CollisionCalcDatabase.get(context)
    private val dao = db.caseDao()

    // Debounce window (tune anytime)
    private val AUTOSAVE_DEBOUNCE_MS = 400L

    private val _caseSummaries = mutableStateListOf<CaseSummary>()
    val caseSummaries: List<CaseSummary> get() = _caseSummaries

    // Cache full cases once opened/loaded
    private val caseCache = mutableMapOf<CaseId, CaseFile>()

    // Debounced autosave jobs per-case
    private val saveJobs = mutableMapOf<CaseId, Job>()

    init {
        // Live case list (counts)
        scope.launch(Dispatchers.IO) {
            dao.observeCases().collectLatest { rows ->
                val summaries = rows.map {
                    CaseSummary(
                        caseId = it.caseId,
                        serviceNumber = it.serviceNumber,
                        createdAtEpochMs = it.createdAtEpochMs,
                        vehiclesCount = it.vehiclesCount,
                        unitsCount = it.unitsCount,
                        notesCount = it.notesCount,
                        calculationsCount = it.calculationsCount
                    )
                }
                withContext(Dispatchers.Main) {
                    _caseSummaries.clear()
                    _caseSummaries.addAll(summaries)
                }
            }
        }
    }

    /**
     * Loads the full case from Room (and caches it). Safe to call repeatedly.
     */
    suspend fun loadCase(caseId: CaseId): CaseFile? {
        caseCache[caseId]?.let { return it }

        return withContext(Dispatchers.IO) {
            val row = dao.getCase(caseId) ?: return@withContext null

            val vehicles = dao.getVehicles(caseId)
            val occupants = if (vehicles.isEmpty()) emptyList() else dao.getOccupants(vehicles.map { it.vehicleId })
            val units = dao.getUnits(caseId)
            val notes = dao.getNotes(caseId)

            val calcs = dao.getCalcs(caseId)
            val calcIds = calcs.map { it.calcId }
            val values = if (calcIds.isEmpty()) emptyList() else dao.getCalcValues(calcIds)
            val steps = if (calcIds.isEmpty()) emptyList() else dao.getCalcSteps(calcIds)
            val attribUnits = if (calcIds.isEmpty()) emptyList() else dao.getCalcAttribUnits(calcIds)
            val attribVehicles = if (calcIds.isEmpty()) emptyList() else dao.getCalcAttribVehicles(calcIds)

            DbMappers.rowsToCase(
                caseRow = row,
                vehicles = vehicles,
                occupants = occupants,
                units = units,
                notes = notes,
                calcs = calcs,
                values = values,
                steps = steps,
                attribUnits = attribUnits,
                attribVehicles = attribVehicles
            ).also { loaded ->
                caseCache[caseId] = loaded
            }
        }
    }

    fun createCase(serviceNumber: String, location: String = "", caseNotes: String = ""): CaseFile {
        val v1 = Vehicle(label = "Vehicle 1")
        val v2 = Vehicle(label = "Vehicle 2")

        val u1 = VehicleUnit(vehicleId = v1.vehicleId, label = "Unit 1")
        val u2 = VehicleUnit(vehicleId = v2.vehicleId, label = "Unit 2")

        val c = CaseFile(
            serviceNumber = serviceNumber.trim(),
            location = location,
            caseNotes = caseNotes,
            crashInfo = CollisionInfo(location = CrashLocation.NonIntersection()),
            vehicles = listOf(v1, v2),
            units = listOf(u1, u2)
        )

        caseCache[c.caseId] = c

        // Save immediately so it appears in list right away.
        persistNow(c)
        return c
    }

    // ---------------- Crash ----------------

    fun updateCrashInfo(caseId: CaseId, crashInfo: CollisionInfo) {
        updateCase(caseId) { it.copy(crashInfo = crashInfo) }
    }

    // ---------------- Notes ----------------

    fun addNote(caseId: CaseId, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        updateCase(caseId) { it.copy(notes = it.notes + CaseNote(text = trimmed)) }
    }

    // ---------------- Vehicles ----------------

    fun updateVehicle(caseId: CaseId, updated: Vehicle) {
        updateCase(caseId) { case ->
            case.copy(vehicles = case.vehicles.map { if (it.vehicleId == updated.vehicleId) updated else it })
        }
    }

    // ---------------- Calculations ----------------

    fun saveCalculation(caseId: CaseId, calc: SavedCalculation) {
        val now = System.currentTimeMillis()
        val normalized = calc.copy(
            createdAtEpochMs = if (calc.createdAtEpochMs <= 0L) now else calc.createdAtEpochMs,
            calcId = if (calc.calcId.isBlank()) UUID.randomUUID().toString() else calc.calcId
        )
        updateCase(caseId) { case ->
            case.copy(calculations = case.calculations + normalized)
        }
    }

    // ---------------- Units ----------------

    fun addVehicleUnit(caseId: CaseId) {
        val case = caseCache[caseId] ?: return
        val next = (case.units.count { it is VehicleUnit } + 1).coerceAtLeast(1)

        val v = Vehicle(label = "Vehicle $next")
        val unit = VehicleUnit(vehicleId = v.vehicleId, label = "Unit $next")

        updateCase(caseId) {
            it.copy(
                vehicles = it.vehicles + v,
                units = it.units + unit
            )
        }
    }

    fun addPedestrianUnit(caseId: CaseId) {
        val case = caseCache[caseId] ?: return
        val next = (case.units.count { it is PedestrianUnit } + 1).coerceAtLeast(1)
        val unit = PedestrianUnit(label = "Pedestrian $next")
        updateCase(caseId) { it.copy(units = it.units + unit) }
    }

    fun updatePedestrianUnit(caseId: CaseId, updated: PedestrianUnit) {
        updateCase(caseId) { case ->
            case.copy(
                units = case.units.map { u -> if (u.unitId == updated.unitId) updated else u }
            )
        }
    }

    fun renameUnit(caseId: CaseId, unitId: UnitId, newLabel: String) {
        val trimmed = newLabel.trim()
        if (trimmed.isEmpty()) return

        updateCase(caseId) { case ->
            case.copy(
                units = case.units.map { u ->
                    if (u.unitId != unitId) u
                    else when (u) {
                        is VehicleUnit -> u.copy(label = trimmed)
                        is PedestrianUnit -> u.copy(label = trimmed)
                    }
                }
            )
        }
    }

    fun removeUnit(caseId: CaseId, unitId: UnitId) {
        updateCase(caseId) { case ->
            val newUnits = case.units.filterNot { it.unitId == unitId }

            val newCalcs = case.calculations.map { c ->
                if (unitId in c.attributedUnitIds) c.copy(attributedUnitIds = (c.attributedUnitIds - unitId))
                else c
            }

            case.copy(units = newUnits, calculations = newCalcs)
        }
    }

    // ---------------- persistence helpers ----------------

    private fun updateCase(caseId: CaseId, transform: (CaseFile) -> CaseFile) {
        val current = caseCache[caseId] ?: return
        val updated = transform(current)
        caseCache[caseId] = updated
        scheduleAutosave(updated)
    }

    private fun scheduleAutosave(caseFile: CaseFile) {
        saveJobs[caseFile.caseId]?.cancel()
        saveJobs[caseFile.caseId] = scope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            persistNow(caseFile)
        }
    }

    private fun persistNow(caseFile: CaseFile) {
        val snapshot = DbMappers.caseToRows(caseFile)
        scope.launch(Dispatchers.IO) {
            dao.replaceCaseSnapshot(
                caseRow = snapshot.caseRow,
                vehicles = snapshot.vehicles,
                occupants = snapshot.occupants,
                units = snapshot.units,
                notes = snapshot.notes,
                calcs = snapshot.calcs,
                values = snapshot.values,
                steps = snapshot.steps,
                attribUnits = snapshot.attribUnits,
                attribVehicles = snapshot.attribVehicles
            )
        }
    }
}
