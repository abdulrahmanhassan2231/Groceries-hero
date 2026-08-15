package de.grocerycompare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs mirror the backend's clean normalized API (see backend/app/models.py).
 * The phone only ever sees this shape; upstream retailer chaos stays server-side.
 */
@Serializable
data class SearchResponseDto(
    val query: String,
    val plz: String,
    @SerialName("offer_week") val offerWeek: String,
    val results: List<OfferDto>,
    val disclaimer: String = "",
)

@Serializable
data class OfferDto(
    val chain: String,
    @SerialName("product_name") val productName: String,
    val brand: String? = null,
    val price: Double,
    val currency: String = "EUR",
    @SerialName("unit_price") val unitPrice: Double? = null,
    @SerialName("unit_price_unit") val unitPriceUnit: String? = null,
    @SerialName("unit_price_derived") val unitPriceDerived: Boolean = true,
    @SerialName("package_size") val packageSize: String? = null,
    @SerialName("price_type") val priceType: String = "offer",
    @SerialName("valid_from") val validFrom: String? = null,
    @SerialName("valid_to") val validTo: String? = null,
    val plz: String,
    val source: String,
    @SerialName("match_score") val matchScore: Double = 0.0,
    @SerialName("nearest_store_distance_km") val nearestStoreDistanceKm: Double? = null,
    @SerialName("nearest_store_address") val nearestStoreAddress: String? = null,
)

@Serializable
data class AutocompleteDto(val suggestions: List<String>)

@Serializable
data class BasketDto(
    val plz: String,
    @SerialName("single_store") val singleStore: SingleStoreDto? = null,
    @SerialName("optimal_split") val optimalSplit: OptimalSplitDto,
    val disclaimer: String = "",
)

@Serializable
data class SingleStoreDto(
    val chain: String,
    val total: Double,
    val count: Int,
    val items: Map<String, OfferDto>,
)

@Serializable
data class OptimalSplitDto(
    val items: Map<String, OfferDto>,
    val total: Double,
)
