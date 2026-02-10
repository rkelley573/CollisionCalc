package com.example.collisioncalc.data.lookups

data class VinDecoded(
    val year: String = "",
    val make: String = "",
    val model: String = ""
)

interface VinDecoder {
    suspend fun decode(vin: String): VinDecoded?
}