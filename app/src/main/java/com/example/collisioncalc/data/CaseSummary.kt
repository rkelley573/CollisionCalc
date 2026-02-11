package com.example.collisioncalc.data

data class CaseSummary(
    val caseId: CaseId,
    val serviceNumber: String,
    val createdAtEpochMs: Long,
    val vehiclesCount: Int,
    val unitsCount: Int,
    val notesCount: Int,
    val calculationsCount: Int,
    val lastActivityEpochMs: Long
)
