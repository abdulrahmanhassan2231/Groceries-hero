package de.grocerycompare.app.data.remote

import de.grocerycompare.app.data.remote.dto.AutocompleteDto
import de.grocerycompare.app.data.remote.dto.BasketDto
import de.grocerycompare.app.data.remote.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface GroceryApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("plz") plz: String,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("max_distance_km") maxDistanceKm: Double? = null,
        @Query("limit") limit: Int = 50,
    ): SearchResponseDto

    @GET("autocomplete")
    suspend fun autocomplete(
        @Query("q") query: String,
        @Query("plz") plz: String,
    ): AutocompleteDto

    @GET("basket")
    suspend fun basket(
        @Query("items") items: List<String>,
        @Query("plz") plz: String,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("max_distance_km") maxDistanceKm: Double? = null,
    ): BasketDto
}
