package com.botpa.turbophotos.gallery

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.GZIPInputStream
import kotlin.math.*

data class CityRecord(
    val name: String,
    val countryCode: String,
    val admin1Code: String,
    val latitude: Double,
    val longitude: Double
)

class LocalGeocoder(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: LocalGeocoder? = null

        fun getInstance(context: Context): LocalGeocoder {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalGeocoder(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val cities: List<CityRecord> by lazy { loadCitiesFromAssets(context) }

    private val admin1Names: Map<String, String> by lazy { loadAdmin1Map(context.applicationContext) }

    //Info
    private fun loadCitiesFromAssets(context: Context): List<CityRecord> {
        val list = mutableListOf<CityRecord>()
        try {
            context.assets.open("cities500_stripped.bin").use { rawStream ->
                GZIPInputStream(rawStream).use { gzStream ->
                    InputStreamReader(gzStream, Charsets.UTF_8).use { streamReader ->
                        BufferedReader(streamReader).use { reader ->
                            reader.forEachLine { line ->
                                val tokens = line.split("\t")
                                //Compacted columns: 0: Name | 1: Lat | 2: Lng | 3: Country | 4: Admin1
                                if (tokens.size >= 5) {
                                    val name = tokens[0]
                                    val lat = tokens[1].toDoubleOrNull()
                                    val lng = tokens[2].toDoubleOrNull()
                                    val countryCode = tokens[3]
                                    val admin1Code = tokens[4]

                                    if (lat != null && lng != null) {
                                        list.add(CityRecord(name, countryCode, admin1Code, lat, lng))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun loadAdmin1Map(context: Context): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            context.assets.open("admin1CodesASCII.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        val tokens = line.split("\t")
                        // File format: Code (e.g., "ES.29"), Name (e.g., "Madrid"), AsciiName, GeonameId
                        if (tokens.size >= 2) {
                            val code = tokens[0] // "ES.29"
                            val name = tokens[1] // "Madrid"
                            map[code] = name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    //Actions
    fun getLocationLabel(lat: Double, lng: Double, maxCityDistanceKm: Double = 30.0): String {
        if (cities.isEmpty()) return "Unknown Location"

        var nearestCity: CityRecord? = null
        var minDistanceKm = Double.MAX_VALUE

        for (city in cities) {
            val dist = calculateHaversineKm(lat, lng, city.latitude, city.longitude)
            if (dist < minDistanceKm) {
                minDistanceKm = dist
                nearestCity = city
            }
        }

        val city = nearestCity ?: return "Unknown Location"
        val countryName = getCountryNameByCode(city.countryCode)

        // Lookup the region (e.g., "ES.29" -> "Madrid")
        val adminKey = "${city.countryCode}.${city.admin1Code}"
        val regionName = admin1Names[adminKey]

        return when {
            minDistanceKm <= maxCityDistanceKm && regionName != null -> {
                // Returns: "Majadahonda, Madrid, Spain"
                "${city.name}, $regionName, $countryName"
            }
            minDistanceKm <= maxCityDistanceKm -> {
                // Fallback if region code isn't in mapping: "Majadahonda, Spain"
                "${city.name}, $countryName"
            }
            regionName != null -> {
                // Far out in nature: "Madrid, Spain"
                "$regionName, $countryName"
            }
            else -> {
                countryName
            }
        }
    }

    private fun calculateHaversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun getCountryNameByCode(code: String): String {
        return if (code.isBlank())
            code
        else try {
            Locale.Builder().setRegion(code).build().displayCountry.ifEmpty { code }
        } catch (_: Exception) {
            code
        }
    }

}