package de.grocerycompare.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Cached offer row so the app works offline (Room-persisted). */
@Entity(tableName = "offers")
data class OfferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val plz: String,
    val offerWeek: String,
    val chain: String,
    val productName: String,
    val brand: String?,
    val price: Double,
    val unitPrice: Double?,
    val unitPriceUnit: String?,
    val packageSize: String?,
    val priceType: String,
    val validFrom: String?,
    val validTo: String?,
    val source: String,
    val matchScore: Double,
    val distanceKm: Double?,
    val storeAddress: String?,
    val cachedAt: Long = System.currentTimeMillis(),
)

/** An item the user is watching for price drops, with the last-seen best unit price. */
@Entity(tableName = "watched")
data class WatchedItemEntity(
    @PrimaryKey val query: String,
    val plz: String,
    val lastBestUnitPrice: Double? = null,
    val lastBestChain: String? = null,
)

@Dao
interface OfferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(offers: List<OfferEntity>)

    @Query("DELETE FROM offers WHERE query = :query AND plz = :plz")
    suspend fun clearFor(query: String, plz: String)

    @Query("SELECT * FROM offers WHERE query = :query AND plz = :plz ORDER BY (unitPrice IS NULL), unitPrice ASC")
    fun observe(query: String, plz: String): Flow<List<OfferEntity>>

    @Query("SELECT * FROM offers WHERE query = :query AND plz = :plz ORDER BY (unitPrice IS NULL), unitPrice ASC")
    suspend fun get(query: String, plz: String): List<OfferEntity>
}

@Dao
interface WatchedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchedItemEntity)

    @Query("SELECT * FROM watched")
    fun observeAll(): Flow<List<WatchedItemEntity>>

    @Query("SELECT * FROM watched")
    suspend fun all(): List<WatchedItemEntity>

    @Query("DELETE FROM watched WHERE query = :query")
    suspend fun remove(query: String)
}

@Database(
    entities = [OfferEntity::class, WatchedItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun offerDao(): OfferDao
    abstract fun watchedDao(): WatchedDao
}
