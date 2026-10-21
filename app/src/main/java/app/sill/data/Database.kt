package app.sill.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Where a planting is now. */
enum class Place {
    /** In one of the five pots on the sill. */
    POT,

    /** Bloomed and moved out into the garden: part of life now. */
    GARDEN,

    /** Didn't take. It feeds the next planting. */
    COMPOST,

    /** Didn't take, but kept in the vase as a dried flower. */
    DRIED;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: POT
    }
}

/** How a packet got into the tin. */
enum class Origin {
    /** A friend's gift. */
    GIFT,

    /** Blown in on its own: something far from you. */
    WIND,

    /** Dropped by a plant that bloomed: one of its neighbours. */
    SELF,

    /** Put aside from the catalogue for later. */
    SAVED;

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: SAVED
    }
}

/**
 * One seed sown: something new being tried. [pot] is its place on the sill (0–4) while it's
 * there. [shape] is the random number its plant is drawn from. [fertilized] means it was sown on
 * compost and needs fewer waterings. [from] and [fromNote] come with a gift.
 */
@Entity(tableName = "plantings")
data class Planting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seed: String,
    val pot: Int,
    val plantedAt: Long = System.currentTimeMillis(),
    val place: String = Place.POT.name,
    /** When it left the pot: moved to the garden, composted or dried. */
    val endedAt: Long = 0,
    val shape: Long = 0,
    val fertilized: Boolean = false,
    val from: String = "",
    val fromNote: String = "",
) {
    val placeOf: Place get() = Place.of(place)
}

/** One more time: one watering, a line about it and maybe a photo. */
@Entity(tableName = "waterings", indices = [Index("plantingId")])
data class Watering(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantingId: Long,
    val at: Long = System.currentTimeMillis(),
    val note: String = "",
    /** A photo in the app's photos folder. */
    val photo: String? = null,
)

/** A packet written by hand: something that isn't in the catalogue. */
@Entity(tableName = "own_seeds")
data class OwnSeed(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val section: String,
    val name: String,
    val about: String = "",
    val start: String = "",
    val bloom: Int = 5,
    val createdAt: Long = System.currentTimeMillis(),
)

/** A packet in the tin, waiting to be sown. */
@Entity(tableName = "packets")
data class Packet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seed: String,
    val origin: String = Origin.SAVED.name,
    val from: String = "",
    val note: String = "",
    val addedAt: Long = System.currentTimeMillis(),
) {
    val originOf: Origin get() = Origin.of(origin)
}

@Dao
interface SillDao {
    @Query("SELECT * FROM plantings ORDER BY plantedAt")
    fun observePlantings(): Flow<List<Planting>>

    @Query("SELECT * FROM plantings ORDER BY plantedAt")
    suspend fun plantings(): List<Planting>

    @Query("SELECT * FROM plantings WHERE id = :id")
    suspend fun planting(id: Long): Planting?

    @Insert
    suspend fun insert(p: Planting): Long

    @Update
    suspend fun update(p: Planting)

    @Query("SELECT * FROM waterings ORDER BY at")
    fun observeWaterings(): Flow<List<Watering>>

    @Query("SELECT * FROM waterings ORDER BY at")
    suspend fun waterings(): List<Watering>

    @Query("SELECT * FROM waterings WHERE plantingId = :id")
    suspend fun waterings(id: Long): List<Watering>

    @Insert
    suspend fun insert(w: Watering): Long

    @Query("DELETE FROM waterings WHERE id = :id")
    suspend fun deleteWatering(id: Long)

    @Query("SELECT * FROM own_seeds ORDER BY createdAt")
    fun observeOwn(): Flow<List<OwnSeed>>

    @Query("SELECT * FROM own_seeds ORDER BY createdAt")
    suspend fun own(): List<OwnSeed>

    @Insert
    suspend fun insert(o: OwnSeed): Long

    @Update
    suspend fun update(o: OwnSeed)

    @Query("SELECT * FROM packets ORDER BY addedAt DESC")
    fun observePackets(): Flow<List<Packet>>

    @Query("SELECT * FROM packets ORDER BY addedAt DESC")
    suspend fun packets(): List<Packet>

    @Query("SELECT * FROM packets WHERE id = :id")
    suspend fun packet(id: Long): Packet?

    @Insert
    suspend fun insert(p: Packet): Long

    @Query("DELETE FROM packets WHERE id = :id")
    suspend fun deletePacket(id: Long)

    @Query("DELETE FROM waterings WHERE plantingId = :id")
    suspend fun deleteWaterings(id: Long)

    @Query("DELETE FROM plantings WHERE id = :id")
    suspend fun deletePlanting(id: Long)

    /** Pulled out as if never sown: a mistake, not a try. */
    @Transaction
    suspend fun uproot(id: Long) {
        deleteWaterings(id)
        deletePlanting(id)
    }
}

@Database(entities = [Planting::class, Watering::class, OwnSeed::class, Packet::class], version = 1, exportSchema = true)
abstract class SillDatabase : RoomDatabase() {
    abstract fun dao(): SillDao

    companion object {
        fun create(context: Context): SillDatabase =
            Room.databaseBuilder(context.applicationContext, SillDatabase::class.java, "sill.db").build()
    }
}
