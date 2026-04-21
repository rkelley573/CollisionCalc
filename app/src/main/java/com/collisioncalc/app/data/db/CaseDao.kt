package com.collisioncalc.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {

    // --------- list / summaries ----------
    @Query(
        """
        SELECT * FROM cases
        ORDER BY createdAtEpochMs DESC
        """
    )
    fun observeCases(): Flow<List<CaseRow>>
    @Query(
        """
    SELECT
        c.caseId AS caseId,
        c.serviceNumber AS serviceNumber,
        c.createdAtEpochMs AS createdAtEpochMs,
        c.vehiclesCount AS vehiclesCount,
        c.unitsCount AS unitsCount,
        c.notesCount AS notesCount,
        c.calculationsCount AS calculationsCount,
        MAX(
            c.createdAtEpochMs,
            IFNULL((SELECT MAX(n.createdAtEpochMs) FROM notes n WHERE n.caseId = c.caseId), 0),
            IFNULL((SELECT MAX(k.createdAtEpochMs) FROM calcs k WHERE k.caseId = c.caseId), 0)
        ) AS lastActivityEpochMs
    FROM cases c
    ORDER BY lastActivityEpochMs DESC
    """
    )
    fun observeCaseListRows(): Flow<List<CaseListRow>>


    // --------- read full case ----------
    @Query("SELECT * FROM cases WHERE caseId = :caseId LIMIT 1")
    suspend fun getCase(caseId: String): CaseRow?

    @Query("SELECT * FROM vehicles WHERE caseId = :caseId")
    suspend fun getVehicles(caseId: String): List<VehicleRow>

    @Query("SELECT * FROM occupants WHERE vehicleId IN (:vehicleIds) ORDER BY idx ASC")
    suspend fun getOccupants(vehicleIds: List<String>): List<OccupantRow>

    @Query("SELECT * FROM units WHERE caseId = :caseId")
    suspend fun getUnits(caseId: String): List<UnitRow>

    @Query("SELECT * FROM notes WHERE caseId = :caseId ORDER BY createdAtEpochMs ASC")
    suspend fun getNotes(caseId: String): List<NoteRow>

    @Query("SELECT * FROM calcs WHERE caseId = :caseId ORDER BY createdAtEpochMs ASC")
    suspend fun getCalcs(caseId: String): List<CalcRow>

    @Query("SELECT * FROM calc_values WHERE calcId IN (:calcIds)")
    suspend fun getCalcValues(calcIds: List<String>): List<CalcValueRow>

    @Query("SELECT * FROM calc_steps WHERE calcId IN (:calcIds) ORDER BY idx ASC")
    suspend fun getCalcSteps(calcIds: List<String>): List<CalcStepRow>

    @Query("SELECT * FROM calc_attrib_units WHERE calcId IN (:calcIds)")
    suspend fun getCalcAttribUnits(calcIds: List<String>): List<CalcAttribUnitRow>

    @Query("SELECT * FROM calc_attrib_vehicles WHERE calcId IN (:calcIds)")
    suspend fun getCalcAttribVehicles(calcIds: List<String>): List<CalcAttribVehicleRow>

    // --------- upserts ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCase(row: CaseRow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVehicles(rows: List<VehicleRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOccupants(rows: List<OccupantRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnits(rows: List<UnitRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(rows: List<NoteRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalcs(rows: List<CalcRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalcValues(rows: List<CalcValueRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalcSteps(rows: List<CalcStepRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalcAttribUnits(rows: List<CalcAttribUnitRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCalcAttribVehicles(rows: List<CalcAttribVehicleRow>)

    // --------- deletes for replacement ----------
    @Query("DELETE FROM vehicles WHERE caseId = :caseId")
    suspend fun deleteVehiclesForCase(caseId: String)

    @Query("DELETE FROM occupants WHERE vehicleId IN (:vehicleIds)")
    suspend fun deleteOccupantsForVehicles(vehicleIds: List<String>)

    @Query("DELETE FROM units WHERE caseId = :caseId")
    suspend fun deleteUnitsForCase(caseId: String)

    @Query("DELETE FROM notes WHERE caseId = :caseId")
    suspend fun deleteNotesForCase(caseId: String)

    @Query("DELETE FROM calcs WHERE caseId = :caseId")
    suspend fun deleteCalcsForCase(caseId: String)

    @Query("DELETE FROM calc_values WHERE calcId IN (:calcIds)")
    suspend fun deleteCalcValues(calcIds: List<String>)

    @Query("DELETE FROM calc_steps WHERE calcId IN (:calcIds)")
    suspend fun deleteCalcSteps(calcIds: List<String>)

    @Query("DELETE FROM calc_attrib_units WHERE calcId IN (:calcIds)")
    suspend fun deleteCalcAttribUnits(calcIds: List<String>)

    @Query("DELETE FROM calc_attrib_vehicles WHERE calcId IN (:calcIds)")
    suspend fun deleteCalcAttribVehicles(calcIds: List<String>)

    // --------- NEW: hard delete an entire case ----------
    @Query("DELETE FROM cases WHERE caseId = :caseId")
    suspend fun deleteCaseRow(caseId: String)

    @Transaction
    suspend fun deleteEntireCase(caseId: String) {
        // Vehicles/occupants
        val vehicles = getVehicles(caseId)
        val vehicleIds = vehicles.map { it.vehicleId }
        if (vehicleIds.isNotEmpty()) deleteOccupantsForVehicles(vehicleIds)
        deleteVehiclesForCase(caseId)

        // Units / notes
        deleteUnitsForCase(caseId)
        deleteNotesForCase(caseId)

        // Calcs + children
        val calcs = getCalcs(caseId)
        val calcIds = calcs.map { it.calcId }
        if (calcIds.isNotEmpty()) {
            deleteCalcValues(calcIds)
            deleteCalcSteps(calcIds)
            deleteCalcAttribUnits(calcIds)
            deleteCalcAttribVehicles(calcIds)
        }
        deleteCalcsForCase(caseId)

        // Case row last
        deleteCaseRow(caseId)
    }

    @Transaction
    suspend fun replaceCaseSnapshot(
        caseRow: CaseRow,
        vehicles: List<VehicleRow>,
        occupants: List<OccupantRow>,
        units: List<UnitRow>,
        notes: List<NoteRow>,
        calcs: List<CalcRow>,
        values: List<CalcValueRow>,
        steps: List<CalcStepRow>,
        attribUnits: List<CalcAttribUnitRow>,
        attribVehicles: List<CalcAttribVehicleRow>
    ) {
        upsertCase(caseRow)

        // Vehicles + occupants
        val vehicleIds = vehicles.map { it.vehicleId }
        deleteVehiclesForCase(caseRow.caseId)
        if (vehicleIds.isNotEmpty()) deleteOccupantsForVehicles(vehicleIds)
        if (vehicles.isNotEmpty()) upsertVehicles(vehicles)
        if (occupants.isNotEmpty()) upsertOccupants(occupants)

        // Units
        deleteUnitsForCase(caseRow.caseId)
        if (units.isNotEmpty()) upsertUnits(units)

        // Notes
        deleteNotesForCase(caseRow.caseId)
        if (notes.isNotEmpty()) upsertNotes(notes)

        // Calcs + children
        val calcIds = calcs.map { it.calcId }
        deleteCalcsForCase(caseRow.caseId)
        if (calcIds.isNotEmpty()) {
            deleteCalcValues(calcIds)
            deleteCalcSteps(calcIds)
            deleteCalcAttribUnits(calcIds)
            deleteCalcAttribVehicles(calcIds)
        }
        if (calcs.isNotEmpty()) upsertCalcs(calcs)
        if (values.isNotEmpty()) upsertCalcValues(values)
        if (steps.isNotEmpty()) upsertCalcSteps(steps)
        if (attribUnits.isNotEmpty()) upsertCalcAttribUnits(attribUnits)
        if (attribVehicles.isNotEmpty()) upsertCalcAttribVehicles(attribVehicles)
    }
}
