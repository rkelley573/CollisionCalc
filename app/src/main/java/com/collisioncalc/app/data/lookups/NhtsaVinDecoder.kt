package com.collisioncalc.app.data.lookups

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NhtsaVinDecoder @Inject constructor() : VinDecoder {

    override suspend fun decode(vin: String): VinDecoded? {
        val trimmed = vin.trim().uppercase()
        if (trimmed.length < 11) return null

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://vpic.nhtsa.dot.gov/api/vehicles/decodevinvalues/$trimmed?format=json"
                Log.d("NhtsaVinDecoder", "Calling URL: $url")

                val json = URL(url).readText()
                Log.d("NhtsaVinDecoder", "Full response: $json")

                val root = JSONObject(json)
                val results = root.getJSONArray("Results")
                Log.d("NhtsaVinDecoder", "Results count: ${results.length()}")

                if (results.length() == 0) {
                    Log.e("NhtsaVinDecoder", "Empty results array")
                    return@withContext null
                }

                val result = results.getJSONObject(0)
                Log.d("NhtsaVinDecoder", "Result keys: ${result.keys().asSequence().toList()}")

                val year = result.optString("ModelYear", "").trim()
                val make = result.optString("Make", "").trim()
                val model = result.optString("Model", "").trim()

                Log.d("NhtsaVinDecoder", "year='$year' make='$make' model='$model'")

                if (year.isEmpty() && make.isEmpty() && model.isEmpty()) null
                else VinDecoded(year = year, make = make, model = model)

            } catch (e: Exception) {
                Log.e("NhtsaVinDecoder", "Exception: ${e.javaClass.simpleName}: ${e.message}", e)
                null
            }
        }
    }
}