package com.collisioncalc.app.data

import java.util.UUID

typealias CaseId = String
typealias VehicleId = String
typealias CalcId = String
typealias NoteId = String
typealias UnitId = String

enum class UnitKind { VEHICLE, PEDESTRIAN }

sealed class UnitEntity {
    abstract val unitId: UnitId
    abstract val kind: UnitKind
    abstract val label: String
}

data class VehicleUnit(
    override val unitId: UnitId = UUID.randomUUID().toString(),
    val vehicleId: VehicleId,
    override val label: String = "Vehicle Unit"
) : UnitEntity() {
    override val kind: UnitKind = UnitKind.VEHICLE
}

data class PedestrianUnit(
    override val unitId: UnitId = UUID.randomUUID().toString(),
    override val label: String = "Pedestrian",
    val name: NameParts = NameParts(),
    val dobIso: String = "",
    val address: String = "",
    val phone: String = ""
) : UnitEntity() {
    override val kind: UnitKind = UnitKind.PEDESTRIAN
}

/* -----------------------------
   Crash Info Models
------------------------------ */

data class CollisionInfo(
    val dateIso: String = "",
    val time24h: String = "",
    val location: CrashLocation = CrashLocation.Unspecified,
    val nearestReference: CrashLocation = CrashLocation.Unspecified
)

sealed class CrashLocation {
    data object Unspecified : CrashLocation()

    data class Intersection(
        val street1: String = "",
        val street2: String = "",
        val city: String = "",
        val state: String = "",
        val zip: String = "",
        val speedLimitMph: String = ""
    ) : CrashLocation()

    data class NonIntersection(
        val blockNumber: String = "",
        val streetName: String = "",
        val city: String = "",
        val state: String = "",
        val zip: String = "",
        val speedLimitMph: String = ""
    ) : CrashLocation()
}

/* -----------------------------
   Core Models
------------------------------ */

data class DriverLicenseInfo(
    val number: String = "",
    val dlClass: String = "",
    val restrictions: String = ""
)

data class NameParts(
    val last: String = "",
    val first: String = "",
    val middle: String = "",
    val suffix: String = ""
) {
    fun display(): String {
        val lf = listOf(last.trim(), first.trim()).filter { it.isNotBlank() }.joinToString(", ")
        val mid = middle.trim().takeIf { it.isNotBlank() }
        val suf = suffix.trim().takeIf { it.isNotBlank() }
        return listOf(lf, mid, suf).filterNotNull().joinToString(" ").trim()
    }
}

data class InsuranceInfo(
    val company: String = "",
    val policyNumber: String = "",
    val phone: String = ""
)

data class Occupant(
    val name: NameParts = NameParts(),
    val dobIso: String = "",
    val seatingPosition: String = "",
    val seatbeltWorn: Boolean? = null,
    val idNumber: String = "",
    val idClass: String = "",
    val idRestrictions: String = "",
    val phone: String = ""
)

data class Vehicle(
    val vehicleId: VehicleId = UUID.randomUUID().toString(),
    val label: String = "Vehicle",
    val color: String = "",
    val year: String = "",
    val make: String = "",
    val model: String = "",
    val vin: String = "",
    val weightLb: Double? = null,
    val occupants: List<Occupant> = emptyList(),
    val insurance: InsuranceInfo = InsuranceInfo(),
    val stockTireWidthMm: Double? = null,
    val stockTireAspectPct: Double? = null,
    val stockTireWheelIn: Double? = null,
    val currentTireWidthMm: Double? = null,
    val currentTireAspectPct: Double? = null,
    val currentTireWheelIn: Double? = null,
    val notes: String = ""
)

data class CaseNote(
    val noteId: NoteId = UUID.randomUUID().toString(),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val text: String
)

data class CalcValue(
    val name: String,
    val value: Double,
    val unit: String,
    val vehicleId: VehicleId? = null
)

enum class CalcType {
    COMBINED_SPEED,
    UNIT_CONVERTER,
    SPEED_TIME_DISTANCE,
    MOMENTUM_REAR_END_SOLVE_S1,
    MOMENTUM_REAR_END_SOLVE_S2,
    MOMENTUM_HEAD_ON_SOLVE_S1,
    MOMENTUM_HEAD_ON_SOLVE_S2,
    MOMENTUM_GENERAL_SOLVE_S1,
    MOMENTUM_GENERAL_SOLVE_S2,
    TIRE_SPEED_CORRECTION,
    MOMENTUM_360,
    UNIT_TOOL
}

data class SavedCalculation(
    val calcId: CalcId = UUID.randomUUID().toString(),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val type: CalcType,
    val title: String,
    val inputs: List<CalcValue>,
    val outputs: List<CalcValue>,
    val equationText: String,
    val steps: List<String>,
    val notes: String = "",
    val attributedVehicleIds: Set<VehicleId> = emptySet(),
    val attributedUnitIds: Set<UnitId> = emptySet()
)

data class CaseFile(
    val caseId: CaseId = UUID.randomUUID().toString(),
    val serviceNumber: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val crashInfo: CollisionInfo = CollisionInfo(),
    val vehicles: List<Vehicle> = emptyList(),
    val units: List<UnitEntity> = emptyList(),
    val notes: List<CaseNote> = emptyList(),
    val calculations: List<SavedCalculation> = emptyList()
)