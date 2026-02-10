package com.example.collisioncalc.data.db

import com.example.collisioncalc.data.*

internal object DbMappers {

    // ---------- CrashLocation flatten/unflatten ----------

    private fun CrashLocation.toFlat(): FlatLocation = when (this) {
        is CrashLocation.Unspecified -> FlatLocation("UNSPEC")
        is CrashLocation.Intersection -> FlatLocation(
            type = "INT",
            street1 = street1,
            street2 = street2,
            city = city,
            state = state,
            zip = zip,
            speedLimitMph = speedLimitMph
        )
        is CrashLocation.NonIntersection -> FlatLocation(
            type = "NONINT",
            blockNumber = blockNumber,
            streetName = streetName,
            city = city,
            state = state,
            zip = zip,
            speedLimitMph = speedLimitMph
        )
    }

    private fun FlatLocation.toCrashLocation(): CrashLocation = when (type) {
        "INT" -> CrashLocation.Intersection(
            street1 = street1,
            street2 = street2,
            city = city,
            state = state,
            zip = zip,
            speedLimitMph = speedLimitMph
        )
        "NONINT" -> CrashLocation.NonIntersection(
            blockNumber = blockNumber,
            streetName = streetName,
            city = city,
            state = state,
            zip = zip,
            speedLimitMph = speedLimitMph
        )
        else -> CrashLocation.Unspecified
    }

    private data class FlatLocation(
        val type: String,
        val street1: String = "",
        val street2: String = "",
        val city: String = "",
        val state: String = "",
        val zip: String = "",
        val speedLimitMph: String = "",
        val blockNumber: String = "",
        val streetName: String = ""
    )

    // ---------- Case <-> rows ----------

    fun caseToRows(caseFile: CaseFile): CaseSnapshotRows {
        val loc = caseFile.crashInfo.location.toFlat()
        val ref = caseFile.crashInfo.nearestReference.toFlat()

        val caseRow = CaseRow(
            caseId = caseFile.caseId,
            serviceNumber = caseFile.serviceNumber,
            createdAtEpochMs = caseFile.createdAtEpochMs,
            locationLegacy = caseFile.location,
            caseNotesLegacy = caseFile.caseNotes,

            dateIso = caseFile.crashInfo.dateIso,
            time24h = caseFile.crashInfo.time24h,

            locType = loc.type,
            locStreet1 = loc.street1,
            locStreet2 = loc.street2,
            locCity = loc.city,
            locState = loc.state,
            locZip = loc.zip,
            locSpeedLimitMph = loc.speedLimitMph,
            locBlockNumber = loc.blockNumber,
            locStreetName = loc.streetName,

            refType = ref.type,
            refStreet1 = ref.street1,
            refStreet2 = ref.street2,
            refCity = ref.city,
            refState = ref.state,
            refZip = ref.zip,
            refSpeedLimitMph = ref.speedLimitMph,
            refBlockNumber = ref.blockNumber,
            refStreetName = ref.streetName,

            vehiclesCount = caseFile.vehicles.size,
            unitsCount = caseFile.units.size,
            notesCount = caseFile.notes.size,
            calculationsCount = caseFile.calculations.size
        )

        val vehicleRows = caseFile.vehicles.map { v ->
            VehicleRow(
                vehicleId = v.vehicleId,
                caseId = caseFile.caseId,
                label = v.label,
                color = v.color,
                year = v.year,
                make = v.make,
                model = v.model,
                vin = v.vin,
                weightLb = v.weightLb,
                insCompany = v.insurance.company,
                insPolicyNumber = v.insurance.policyNumber,
                insPhone = v.insurance.phone,
                stockTireWidthMm = v.stockTireWidthMm,
                stockTireAspectPct = v.stockTireAspectPct,
                stockTireWheelIn = v.stockTireWheelIn,
                currentTireWidthMm = v.currentTireWidthMm,
                currentTireAspectPct = v.currentTireAspectPct,
                currentTireWheelIn = v.currentTireWheelIn,
                notes = v.notes
            )
        }

        val occupantRows = buildList {
            caseFile.vehicles.forEach { v ->
                v.occupants.forEachIndexed { idx, o ->
                    add(
                        OccupantRow(
                            vehicleId = v.vehicleId,
                            idx = idx,
                            last = o.name.last,
                            first = o.name.first,
                            middle = o.name.middle,
                            suffix = o.name.suffix,
                            dobIso = o.dobIso,
                            seatingPosition = o.seatingPosition,
                            seatbeltWorn = o.seatbeltWorn?.let { if (it) 1 else 0 },
                            idNumber = o.idNumber,
                            idClass = o.idClass,
                            idRestrictions = o.idRestrictions,
                            phone = o.phone
                        )
                    )
                }
            }
        }

        val unitRows = caseFile.units.map { u ->
            when (u) {
                is VehicleUnit -> UnitRow(
                    unitId = u.unitId,
                    caseId = caseFile.caseId,
                    kind = UnitKind.VEHICLE.name,
                    label = u.label,
                    vehicleId = u.vehicleId,
                    pedLast = "",
                    pedFirst = "",
                    pedMiddle = "",
                    pedSuffix = "",
                    pedDobIso = "",
                    pedAddress = "",
                    pedPhone = ""
                )
                is PedestrianUnit -> UnitRow(
                    unitId = u.unitId,
                    caseId = caseFile.caseId,
                    kind = UnitKind.PEDESTRIAN.name,
                    label = u.label,
                    vehicleId = null,
                    pedLast = u.name.last,
                    pedFirst = u.name.first,
                    pedMiddle = u.name.middle,
                    pedSuffix = u.name.suffix,
                    pedDobIso = u.dobIso,
                    pedAddress = u.address,
                    pedPhone = u.phone
                )
            }
        }

        val noteRows = caseFile.notes.map { n ->
            NoteRow(
                noteId = n.noteId,
                caseId = caseFile.caseId,
                createdAtEpochMs = n.createdAtEpochMs,
                text = n.text
            )
        }

        val calcRows = caseFile.calculations.map { c ->
            CalcRow(
                calcId = c.calcId,
                caseId = caseFile.caseId,
                createdAtEpochMs = c.createdAtEpochMs,
                type = c.type.name,
                title = c.title,
                equationText = c.equationText,
                notes = c.notes
            )
        }

        val valueRows = buildList {
            caseFile.calculations.forEach { c ->
                c.inputs.forEach { v ->
                    add(
                        CalcValueRow(
                            calcId = c.calcId,
                            role = "IN",
                            name = v.name,
                            value = v.value,
                            unit = v.unit,
                            vehicleId = v.vehicleId
                        )
                    )
                }
                c.outputs.forEach { v ->
                    add(
                        CalcValueRow(
                            calcId = c.calcId,
                            role = "OUT",
                            name = v.name,
                            value = v.value,
                            unit = v.unit,
                            vehicleId = v.vehicleId
                        )
                    )
                }
            }
        }

        val stepRows = buildList {
            caseFile.calculations.forEach { c ->
                c.steps.forEachIndexed { idx, s ->
                    add(CalcStepRow(calcId = c.calcId, idx = idx, text = s))
                }
            }
        }

        val attribUnitRows = buildList {
            caseFile.calculations.forEach { c ->
                c.attributedUnitIds.forEach { unitId ->
                    add(CalcAttribUnitRow(calcId = c.calcId, unitId = unitId))
                }
            }
        }

        val attribVehicleRows = buildList {
            caseFile.calculations.forEach { c ->
                c.attributedVehicleIds.forEach { vehicleId ->
                    add(CalcAttribVehicleRow(calcId = c.calcId, vehicleId = vehicleId))
                }
            }
        }

        return CaseSnapshotRows(
            caseRow = caseRow,
            vehicles = vehicleRows,
            occupants = occupantRows,
            units = unitRows,
            notes = noteRows,
            calcs = calcRows,
            values = valueRows,
            steps = stepRows,
            attribUnits = attribUnitRows,
            attribVehicles = attribVehicleRows
        )
    }

    suspend fun rowsToCase(
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
    ): CaseFile {
        val crashInfo = CollisionInfo(
            dateIso = caseRow.dateIso,
            time24h = caseRow.time24h,
            location = FlatLocation(
                type = caseRow.locType,
                street1 = caseRow.locStreet1,
                street2 = caseRow.locStreet2,
                city = caseRow.locCity,
                state = caseRow.locState,
                zip = caseRow.locZip,
                speedLimitMph = caseRow.locSpeedLimitMph,
                blockNumber = caseRow.locBlockNumber,
                streetName = caseRow.locStreetName
            ).toCrashLocation(),
            nearestReference = FlatLocation(
                type = caseRow.refType,
                street1 = caseRow.refStreet1,
                street2 = caseRow.refStreet2,
                city = caseRow.refCity,
                state = caseRow.refState,
                zip = caseRow.refZip,
                speedLimitMph = caseRow.refSpeedLimitMph,
                blockNumber = caseRow.refBlockNumber,
                streetName = caseRow.refStreetName
            ).toCrashLocation()
        )

        val occByVehicle = occupants.groupBy { it.vehicleId }

        val domainVehicles = vehicles.map { v ->
            val occs = occByVehicle[v.vehicleId].orEmpty()
                .sortedBy { it.idx }
                .map { o ->
                    Occupant(
                        name = NameParts(
                            last = o.last,
                            first = o.first,
                            middle = o.middle,
                            suffix = o.suffix
                        ),
                        dobIso = o.dobIso,
                        seatingPosition = o.seatingPosition,
                        seatbeltWorn = o.seatbeltWorn?.let { it == 1 },
                        idNumber = o.idNumber,
                        idClass = o.idClass,
                        idRestrictions = o.idRestrictions,
                        phone = o.phone
                    )
                }

            Vehicle(
                vehicleId = v.vehicleId,
                label = v.label,
                color = v.color,
                year = v.year,
                make = v.make,
                model = v.model,
                vin = v.vin,
                weightLb = v.weightLb,
                occupants = occs,
                insurance = InsuranceInfo(
                    company = v.insCompany,
                    policyNumber = v.insPolicyNumber,
                    phone = v.insPhone
                ),
                stockTireWidthMm = v.stockTireWidthMm,
                stockTireAspectPct = v.stockTireAspectPct,
                stockTireWheelIn = v.stockTireWheelIn,
                currentTireWidthMm = v.currentTireWidthMm,
                currentTireAspectPct = v.currentTireAspectPct,
                currentTireWheelIn = v.currentTireWheelIn,
                notes = v.notes
            )
        }

        val domainUnits: List<UnitEntity> = units.map { u ->
            when (u.kind) {
                UnitKind.VEHICLE.name -> VehicleUnit(
                    unitId = u.unitId,
                    vehicleId = u.vehicleId ?: "",
                    label = u.label
                )
                else -> PedestrianUnit(
                    unitId = u.unitId,
                    label = u.label,
                    name = NameParts(
                        last = u.pedLast,
                        first = u.pedFirst,
                        middle = u.pedMiddle,
                        suffix = u.pedSuffix
                    ),
                    dobIso = u.pedDobIso,
                    address = u.pedAddress,
                    phone = u.pedPhone
                )
            }
        }

        val valuesByCalc = values.groupBy { it.calcId }
        val stepsByCalc = steps.groupBy { it.calcId }
        val attribUnitsByCalc = attribUnits.groupBy { it.calcId }
        val attribVehiclesByCalc = attribVehicles.groupBy { it.calcId }

        val domainCalcs = calcs.map { c ->
            val vs = valuesByCalc[c.calcId].orEmpty()
            val ins = vs.filter { it.role == "IN" }.map { CalcValue(it.name, it.value, it.unit, it.vehicleId) }
            val outs = vs.filter { it.role == "OUT" }.map { CalcValue(it.name, it.value, it.unit, it.vehicleId) }
            val st = stepsByCalc[c.calcId].orEmpty().sortedBy { it.idx }.map { it.text }
            val au = attribUnitsByCalc[c.calcId].orEmpty().map { it.unitId }.toSet()
            val av = attribVehiclesByCalc[c.calcId].orEmpty().map { it.vehicleId }.toSet()

            SavedCalculation(
                calcId = c.calcId,
                createdAtEpochMs = c.createdAtEpochMs,
                type = CalcType.valueOf(c.type),
                title = c.title,
                inputs = ins,
                outputs = outs,
                equationText = c.equationText,
                steps = st,
                notes = c.notes,
                attributedVehicleIds = av,
                attributedUnitIds = au
            )
        }

        return CaseFile(
            caseId = caseRow.caseId,
            serviceNumber = caseRow.serviceNumber,
            createdAtEpochMs = caseRow.createdAtEpochMs,
            location = caseRow.locationLegacy,
            caseNotes = caseRow.caseNotesLegacy,
            crashInfo = crashInfo,
            vehicles = domainVehicles,
            units = domainUnits,
            notes = notes.map { CaseNote(it.noteId, it.createdAtEpochMs, it.text) },
            calculations = domainCalcs
        )
    }

    data class CaseSnapshotRows(
        val caseRow: CaseRow,
        val vehicles: List<VehicleRow>,
        val occupants: List<OccupantRow>,
        val units: List<UnitRow>,
        val notes: List<NoteRow>,
        val calcs: List<CalcRow>,
        val values: List<CalcValueRow>,
        val steps: List<CalcStepRow>,
        val attribUnits: List<CalcAttribUnitRow>,
        val attribVehicles: List<CalcAttribVehicleRow>
    )
}
