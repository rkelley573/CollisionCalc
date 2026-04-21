package com.collisioncalc.app.data.lookups

/**
 * Decoded fields returned from a VIN lookup.
 * All fields default to empty string if not found.
 */
data class VinDecoded(
    val year: String = "",
    val make: String = "",
    val model: String = ""
)

/**
 * Contract for VIN decoding. The production implementation hits the
 * NHTSA vPIC API (no API key required):
 * https://vpic.nhtsa.dot.gov/api/vehicles/decodevin/{vin}?format=json
 *
 * [FakeVinDecoder] is used until the real implementation is wired in via Hilt.
 */
interface VinDecoder {
    suspend fun decode(vin: String): VinDecoded?
}