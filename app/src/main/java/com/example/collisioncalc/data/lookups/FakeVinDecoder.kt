package com.example.collisioncalc.data.lookups

class FakeVinDecoder : VinDecoder {
    override suspend fun decode(vin: String): VinDecoded? {
        val v = vin.trim()
        if (v.length < 11) return null
        // Placeholder: return empty fields until you wire NHTSA vPIC later
        return VinDecoded()
    }
}