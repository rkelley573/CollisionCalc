package com.collisioncalc.app.util

import com.collisioncalc.app.data.CrashLocation

fun formatCrashLocation(loc: CrashLocation): String {
    return when (loc) {
        is CrashLocation.Unspecified -> ""

        is CrashLocation.Intersection -> {
            val s1 = loc.street1.trim()
            val s2 = loc.street2.trim()
            val city = loc.city.trim()
            val state = loc.state.trim()
            val zip = loc.zip.trim()
            val spd = loc.speedLimitMph.trim().filter { it.isDigit() }

            val streets = listOf(s1, s2).filter { it.isNotBlank() }.joinToString(" & ")
            val cityStateZip = listOf(city, state, zip).filter { it.isNotBlank() }.joinToString(" ")
            val where = listOf(streets, cityStateZip).filter { it.isNotBlank() }.joinToString(", ")

            val spdPart = spd.takeIf { it.isNotBlank() }?.let { " (SL $it mph)" } ?: ""
            (where + spdPart).trim().trimStart(',').trim()
        }

        is CrashLocation.NonIntersection -> {
            val block = loc.blockNumber.trim().takeIf { it.isNotBlank() }?.let { "$it " } ?: ""
            val street = loc.streetName.trim()
            val city = loc.city.trim()
            val state = loc.state.trim()
            val zip = loc.zip.trim()
            val spd = loc.speedLimitMph.trim().filter { it.isDigit() }

            val roadPart = (block + street).trim()
            val cityStateZip = listOf(city, state, zip).filter { it.isNotBlank() }.joinToString(" ")
            val where = listOf(roadPart, cityStateZip).filter { it.isNotBlank() }.joinToString(", ")

            val spdPart = spd.takeIf { it.isNotBlank() }?.let { " (SL $it mph)" } ?: ""
            (where + spdPart).trim().trimStart(',').trim()
        }
    }
}
