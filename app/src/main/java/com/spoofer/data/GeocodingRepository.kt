package com.spoofer.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocodingRepository
    @Inject
    constructor(
        private val okHttpClient: OkHttpClient,
        private val gson: Gson,
    ) {
        suspend fun search(
            query: String,
            biasLat: Double? = null,
            biasLon: Double? = null,
            limit: Int = 8,
        ): List<PlaceSuggestion> {
            if (query.isBlank()) return emptyList()

            return withContext(Dispatchers.IO) {
                val urlBuilder = java.lang.StringBuilder("https://photon.komoot.io/api/")
                urlBuilder.append("?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
                urlBuilder.append("&limit=$limit")
                urlBuilder.append("&lang=en")

                if (biasLat != null && biasLon != null) {
                    urlBuilder.append("&lat=$biasLat")
                    urlBuilder.append("&lon=$biasLon")
                }

                val request =
                    Request.Builder()
                        .url(urlBuilder.toString())
                        .header("Accept", "application/json")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: throw IOException("Empty response")

                if (!response.isSuccessful) {
                    throw IOException("Photon error ${response.code}: $body")
                }

                val json = gson.fromJson(body, JsonObject::class.java)
                val features = json.getAsJsonArray("features") ?: return@withContext emptyList()

                features.mapNotNull { feature ->
                    try {
                        val props = feature.asJsonObject.getAsJsonObject("properties")
                        val geom = feature.asJsonObject.getAsJsonObject("geometry")
                        val coords = geom.getAsJsonArray("coordinates")

                        val name = props.get("name")?.asString ?: ""
                        val street = props.get("street")?.asString
                        val housenumber = props.get("housenumber")?.asString
                        val city =
                            props.get("city")?.asString
                                ?: props.get("town")?.asString
                                ?: props.get("village")?.asString
                        val state = props.get("state")?.asString
                        val country = props.get("country")?.asString
                        val osmValue = props.get("osm_value")?.asString ?: ""

                        val addressParts =
                            listOfNotNull(
                                if (housenumber != null && street != null) {
                                    "$housenumber $street"
                                } else {
                                    street
                                },
                                city,
                                state,
                            ).filter { it.isNotBlank() }

                        val label =
                            if (name.isNotBlank() && addressParts.isNotEmpty()) {
                                "$name, ${addressParts.joinToString(", ")}"
                            } else if (name.isNotBlank()) {
                                name
                            } else {
                                addressParts.joinToString(", ")
                            }

                        val locality =
                            listOfNotNull(city, state, country)
                                .filter { it.isNotBlank() }
                                .joinToString(", ")

                        PlaceSuggestion(
                            label = label.ifBlank { "Unknown location" },
                            name = name,
                            locality = locality,
                            latitude = coords[1].asDouble,
                            longitude = coords[0].asDouble,
                            type = osmValue,
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }

data class PlaceSuggestion(
    val label: String,
    val name: String,
    val locality: String,
    val latitude: Double,
    val longitude: Double,
    val type: String = "",
)
