package de.grocerycompare.app.data.repo

import de.grocerycompare.app.data.local.OfferDao
import de.grocerycompare.app.data.local.OfferEntity
import de.grocerycompare.app.data.local.WatchedDao
import de.grocerycompare.app.data.local.WatchedItemEntity
import de.grocerycompare.app.data.remote.GroceryApi
import de.grocerycompare.app.data.remote.dto.BasketDto
import de.grocerycompare.app.data.remote.dto.OfferDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first: search returns cached Room data immediately and refreshes it from the
 * backend when reachable. All scraping/parsing is server-side; the app only reads the
 * normalized API and persists the result.
 */
@Singleton
class GroceryRepository @Inject constructor(
    private val api: GroceryApi,
    private val offerDao: OfferDao,
    private val watchedDao: WatchedDao,
) {

    /** Cold flow of cached offers for a query (works fully offline). */
    fun observeOffers(query: String, plz: String): Flow<List<OfferEntity>> =
        offerDao.observe(query, plz)

    /** Fetch from backend and replace the cache for this query. Returns true on success. */
    suspend fun refreshOffers(
        query: String,
        plz: String,
        lat: Double? = null,
        lon: Double? = null,
        maxDistanceKm: Double? = null,
    ): Result<Unit> = runCatching {
        val resp = api.search(query, plz, lat, lon, maxDistanceKm)
        val rows = resp.results.map { it.toEntity(query, plz, resp.offerWeek) }
        offerDao.clearFor(query, plz)
        offerDao.insertAll(rows)
    }

    suspend fun autocomplete(query: String, plz: String): List<String> =
        runCatching { api.autocomplete(query, plz).suggestions }.getOrDefault(emptyList())

    /** Shopping-list optimization: best single store vs cheapest-per-item split. */
    suspend fun basket(
        items: List<String>,
        plz: String,
        lat: Double? = null,
        lon: Double? = null,
    ): Result<BasketDto> = runCatching { api.basket(items, plz, lat, lon) }

    suspend fun watch(query: String, plz: String) =
        watchedDao.upsert(WatchedItemEntity(query = query, plz = plz))

    suspend fun unwatch(query: String) = watchedDao.remove(query)

    fun observeWatched(): Flow<List<WatchedItemEntity>> = watchedDao.observeAll()

    /**
     * For each watched item: pull latest offers, find the best unit price, and report any
     * that dropped below the previously-recorded best (used by [PriceRefreshWorker]).
     */
    suspend fun checkPriceDrops(): List<PriceDrop> {
        val drops = mutableListOf<PriceDrop>()
        for (w in watchedDao.all()) {
            val resp = runCatching { api.search(w.query, w.plz) }.getOrNull() ?: continue
            val best = resp.results.minByOrNull { it.unitPrice ?: Double.MAX_VALUE } ?: continue
            val bestUnit = best.unitPrice ?: continue
            val previous = w.lastBestUnitPrice
            if (previous == null || bestUnit < previous) {
                if (previous != null) {
                    drops += PriceDrop(w.query, best.productName, best.chain, best.price, bestUnit)
                }
                watchedDao.upsert(
                    w.copy(lastBestUnitPrice = bestUnit, lastBestChain = best.chain)
                )
            }
        }
        return drops
    }
}

data class PriceDrop(
    val query: String,
    val productName: String,
    val chain: String,
    val price: Double,
    val unitPrice: Double,
)

private fun OfferDto.toEntity(query: String, plz: String, offerWeek: String) = OfferEntity(
    query = query,
    plz = plz,
    offerWeek = offerWeek,
    chain = chain,
    productName = productName,
    brand = brand,
    price = price,
    unitPrice = unitPrice,
    unitPriceUnit = unitPriceUnit,
    packageSize = packageSize,
    priceType = priceType,
    validFrom = validFrom,
    validTo = validTo,
    source = source,
    matchScore = matchScore,
    distanceKm = nearestStoreDistanceKm,
    storeAddress = nearestStoreAddress,
)
