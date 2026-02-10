package com.example.collisioncalc.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cases")
data class CaseRow(
    @PrimaryKey val caseId: String,
    val serviceNumber: String,
    val createdAtEpochMs: Long,

    // legacy strings (kept for compatibility)
    val locationLegacy: String,
    val caseNotesLegacy: String,

    // crash info (flattened)
    val dateIso: String,
    val time24h: String,

    // primary crash location
    val locType: String, // "UNSPEC" | "INT" | "NONINT"
    val locStreet1: String,
    val locStreet2: String,
    val locCity: String,
    val locState: String,
    val locZip: String,
    val locSpeedLimitMph: String,
    val locBlockNumber: String,
    val locStreetName: String,

    // nearest reference location
    val refType: String,
    val refStreet1: String,
    val refStreet2: String,
    val refCity: String,
    val refState: String,
    val refZip: String,
    val refSpeedLimitMph: String,
    val refBlockNumber: String,
    val refStreetName: String,

    // counts for list screen
    val vehiclesCount: Int,
    val unitsCount: Int,
    val notesCount: Int,
    val calculationsCount: Int
)

@Entity(
    tableName = "vehicles",
    indices = [Index("caseId")]
)
data class VehicleRow(
    @PrimaryKey val vehicleId: String,
    val caseId: String,

    val label: String,
    val color: String,
    val year: String,
    val make: String,
    val model: String,
    val vin: String,
    val weightLb: Double?,

    // insurance
    val insCompany: String,
    val insPolicyNumber: String,
    val insPhone: String,

    // tires
    val stockTireWidthMm: Double?,
    val stockTireAspectPct: Double?,
    val stockTireWheelIn: Double?,
    val currentTireWidthMm: Double?,
    val currentTireAspectPct: Double?,
    val currentTireWheelIn: Double?,

    val notes: String
)

@Entity(
    tableName = "occupants",
    primaryKeys = ["vehicleId", "idx"]
)
data class OccupantRow(
    val vehicleId: String,
    val idx: Int,

    val last: String,
    val first: String,
    val middle: String,
    val suffix: String,

    val dobIso: String,
    val seatingPosition: String,
    val seatbeltWorn: Int?, // null unknown, 0 false, 1 true

    val idNumber: String,
    val idClass: String,
    val idRestrictions: String,
    val phone: String
)

@Entity(
    tableName = "units",
    indices = [Index("caseId")]
)
data class UnitRow(
    @PrimaryKey val unitId: String,
    val caseId: String,

    val kind: String, // "VEHICLE" | "PEDESTRIAN"
    val label: String,

    // vehicle unit linkage
    val vehicleId: String?,

    // pedestrian fields
    val pedLast: String,
    val pedFirst: String,
    val pedMiddle: String,
    val pedSuffix: String,
    val pedDobIso: String,
    val pedAddress: String,
    val pedPhone: String
)

@Entity(
    tableName = "notes",
    indices = [Index("caseId")]
)
data class NoteRow(
    @PrimaryKey val noteId: String,
    val caseId: String,
    val createdAtEpochMs: Long,
    val text: String
)

@Entity(
    tableName = "calcs",
    indices = [Index("caseId")]
)
data class CalcRow(
    @PrimaryKey val calcId: String,
    val caseId: String,
    val createdAtEpochMs: Long,
    val type: String,       // CalcType.name
    val title: String,
    val equationText: String,
    val notes: String
)

@Entity(
    tableName = "calc_values",
    indices = [Index("calcId")]
)
data class CalcValueRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calcId: String,
    val role: String, // "IN" | "OUT"
    val name: String,
    val value: Double,
    val unit: String,
    val vehicleId: String?
)

@Entity(
    tableName = "calc_steps",
    indices = [Index("calcId")]
)
data class CalcStepRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calcId: String,
    val idx: Int,
    val text: String
)

@Entity(
    tableName = "calc_attrib_units",
    primaryKeys = ["calcId", "unitId"]
)
data class CalcAttribUnitRow(
    val calcId: String,
    val unitId: String
)

@Entity(
    tableName = "calc_attrib_vehicles",
    primaryKeys = ["calcId", "vehicleId"]
)
data class CalcAttribVehicleRow(
    val calcId: String,
    val vehicleId: String
)
