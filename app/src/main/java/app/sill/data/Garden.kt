package app.sill.data

import android.content.Context
import app.sill.domain.Gift
import app.sill.domain.Rules
import java.io.File
import kotlin.random.Random

/** What happened after a watering. */
data class Watered(val bloomed: Boolean, val selfSown: Int)

/** Everything done on the sill: sowing, watering, moving out, composting, the tin and the wind. */
class Garden(private val context: Context, private val db: SillDatabase, private val catalogue: Catalogue) {
    private val dao get() = db.dao()

    suspend fun seed(key: String): Seed? = catalogue[key] ?: key.removePrefix("o:").toLongOrNull()
        ?.takeIf { key.startsWith("o:") }?.let { id -> dao.own().firstOrNull { it.id == id }?.let(Seed::own) }

    /**
     * Sows a seed in the first free pot, on compost if there is some. A packet from the tin brings
     * its giver along and leaves the tin. Null when all pots are taken.
     */
    suspend fun plant(key: String, packetId: Long = 0, now: Long = System.currentTimeMillis()): Long? {
        val all = dao.plantings()
        val pot = Rules.freePot(all) ?: return null
        val packet = if (packetId > 0) dao.packet(packetId) else dao.packets().firstOrNull { it.seed == key }
        val id = dao.insert(
            Planting(
                seed = key, pot = pot, plantedAt = now, shape = Random.nextLong(), fertilized = Rules.compost(all) > 0,
                from = packet?.from.orEmpty(), fromNote = packet?.note.orEmpty(),
            ),
        )
        packet?.let { dao.deletePacket(it.id) }
        return id
    }

    /** One more time. A watering that brings the bloom also drops the neighbours' seeds in the tin. */
    suspend fun water(plantingId: Long, note: String, photo: String? = null, now: Long = System.currentTimeMillis()): Watered {
        val p = dao.planting(plantingId) ?: return Watered(false, 0)
        val seed = seed(p.seed) ?: return Watered(false, 0)
        val before = dao.waterings(plantingId).size
        dao.insert(Watering(plantingId = plantingId, at = now, note = note.trim(), photo = photo))
        val target = Rules.target(seed, p.fertilized)
        if (before >= target || before + 1 < target) return Watered(false, 0)
        val tin = dao.packets().map { it.seed }.toSet()
        val sown = Rules.selfSown(seed, dao.plantings(), tin)
        sown.forEach { dao.insert(Packet(seed = it, origin = Origin.SELF.name, from = p.seed, addedAt = now)) }
        return Watered(true, sown.size)
    }

    suspend fun unwater(id: Long) {
        val w = dao.waterings().firstOrNull { it.id == id } ?: return
        dao.deleteWatering(id)
        Photos.delete(context, w.photo)
    }

    private suspend fun move(id: Long, place: Place, now: Long) {
        val p = dao.planting(id) ?: return
        dao.update(p.copy(place = place.name, endedAt = now))
    }

    /** Bloomed: out of the pot and into the garden for good. */
    suspend fun toGarden(id: Long, now: Long = System.currentTimeMillis()) = move(id, Place.GARDEN, now)

    suspend fun compost(id: Long, now: Long = System.currentTimeMillis()) = move(id, Place.COMPOST, now)

    suspend fun dry(id: Long, now: Long = System.currentTimeMillis()) = move(id, Place.DRIED, now)

    /** Sown by mistake: gone, as if it never was. */
    suspend fun uproot(id: Long) {
        dao.waterings(id).forEach { Photos.delete(context, it.photo) }
        dao.uproot(id)
    }

    /** Into the tin for later. A seed already there stays once. */
    suspend fun save(key: String, origin: Origin = Origin.SAVED, from: String = "", note: String = "", now: Long = System.currentTimeMillis()): Long {
        dao.packets().firstOrNull { it.seed == key }?.let { return it.id }
        return dao.insert(Packet(seed = key, origin = origin.name, from = from.trim(), note = note.trim(), addedAt = now))
    }

    suspend fun discard(packetId: Long) = dao.deletePacket(packetId)

    suspend fun writeOwn(section: Section, name: String, about: String, start: String, bloom: Int): String {
        val id = dao.insert(OwnSeed(section = section.name, name = name.trim(), about = about.trim(), start = start.trim(), bloom = bloom))
        return "o:$id"
    }

    /** A gift from a friend: into the tin, with their note. A hand-written packet is copied first. */
    suspend fun receive(g: Gift, now: Long = System.currentTimeMillis()): Long? {
        val key = if (g.own) {
            dao.own().firstOrNull { it.name.equals(g.name, ignoreCase = true) }?.let { "o:${it.id}" }
                ?: writeOwn(g.section, g.name, g.about, g.start, g.bloom)
        } else g.key.takeIf { catalogue[it] != null } ?: return null
        dao.packets().firstOrNull { it.seed == key }?.let { dao.deletePacket(it.id) }
        return dao.insert(Packet(seed = key, origin = Origin.GIFT.name, from = g.from, note = g.note, addedAt = now))
    }

    /** Once a week the wind leaves a seed on the sill, unless three of its seeds still lie unsown. */
    suspend fun wind(settings: SettingsStore, now: Long = System.currentTimeMillis()): Seed? {
        val s = settings.now()
        val week = Rules.week(now)
        if (!s.onboarded || s.windWeek >= week) return null
        settings.update { it.copy(windWeek = week) }
        val tin = dao.packets()
        if (tin.count { it.originOf == Origin.WIND } >= 3) return null
        val seed = Rules.wind(catalogue.seeds, dao.plantings(), tin.map { it.seed }.toSet(), now) ?: return null
        dao.insert(Packet(seed = seed.key, origin = Origin.WIND.name, addedAt = now))
        return seed
    }
}

/** Photos of a watering, taken by the phone's own camera and kept in the app's files. */
object Photos {
    fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }
    fun file(context: Context, name: String) = File(dir(context), name)
    fun fresh(context: Context): File = file(context, "w-${System.currentTimeMillis()}.jpg")

    fun delete(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        file(context, name).delete()
    }
}
