package app.sill.data

import android.content.Context

/**
 * The sections of the seed catalogue, and the kind of plant each one grows on the sill: tastes are
 * herbs, music and art are flowers, crafts are roots, learning is grain, people are vines, the
 * uncomfortable is a cactus, the night and the strange are mushrooms, long skills are trees.
 */
enum class Section(val ink: Long, val leaf: Long) {
    HERBS(0xFF5E7F33, 0xFF6E9A3E),
    FLOWERS(0xFFBF4E63, 0xFF5D8A3F),
    ROOTS(0xFFC9692F, 0xFF4F8A3A),
    GRAINS(0xFFB8902E, 0xFF8FA046),
    VINES(0xFF3F6F9E, 0xFF4E8743),
    CACTI(0xFF2F7F6E, 0xFF5B8F5A),
    MUSHROOMS(0xFF6E4683, 0xFF6C8A4A),
    TREES(0xFF35603F, 0xFF3F7A3C);

    companion object {
        fun of(s: String?) = entries.firstOrNull { it.name == s } ?: HERBS
    }
}

/**
 * One seed packet. [key] is "c:<id>" for the catalogue and "o:<id>" for a packet written by hand.
 * [depth] is how hard it is to begin (1–3), [bloom] how many waterings until it's yours. [months]
 * is when to sow (empty: all year), [neighbors] the keys that grow well beside it.
 */
data class Seed(
    val key: String,
    val section: Section,
    val depth: Int,
    val bloom: Int,
    val months: Set<Int>,
    val neighbors: List<String>,
    val nameEn: String,
    val nameRu: String,
    val aboutEn: String,
    val aboutRu: String,
    val startEn: String,
    val startRu: String,
    val own: Boolean = false,
) {
    fun name(ru: Boolean) = if (ru) nameRu.ifBlank { nameEn } else nameEn.ifBlank { nameRu }
    fun about(ru: Boolean) = if (ru) aboutRu.ifBlank { aboutEn } else aboutEn.ifBlank { aboutRu }
    fun start(ru: Boolean) = if (ru) startRu.ifBlank { startEn } else startEn.ifBlank { startRu }
    fun inSeason(month: Int) = months.isEmpty() || month in months

    /** A number that picks the colours and shape of this kind of plant, the same every time. */
    val hash: Int get() = key.hashCode()

    companion object {
        fun own(o: OwnSeed) = Seed(
            key = "o:${o.id}", section = Section.of(o.section), depth = 1, bloom = o.bloom, months = emptySet(), neighbors = emptyList(),
            nameEn = o.name, nameRu = o.name, aboutEn = o.about, aboutRu = o.about, startEn = o.start, startRu = o.start, own = true,
        )
    }
}

/** The printed catalogue, read once from assets/seeds.tsv. */
class Catalogue(val seeds: List<Seed>) {
    private val byKey = seeds.associateBy { it.key }
    operator fun get(key: String): Seed? = byKey[key]

    companion object {
        fun load(context: Context): Catalogue = context.assets.open("seeds.tsv").bufferedReader().use { parse(it.readText()) }

        fun parse(tsv: String): Catalogue = Catalogue(
            tsv.lineSequence().drop(1).filter { it.isNotBlank() }.map { line ->
                val f = line.split('\t')
                Seed(
                    key = "c:${f[0]}", section = Section.of(f[1]), depth = f[2].toInt(), bloom = f[3].toInt(),
                    months = f[4].split(',').mapNotNull { it.trim().toIntOrNull() }.toSet(),
                    neighbors = f[5].split(',').filter { it.isNotBlank() }.map { "c:${it.trim()}" },
                    nameEn = f[6], nameRu = f[7], aboutEn = f[8], aboutRu = f[9], startEn = f[10], startRu = f[11],
                )
            }.toList(),
        )
    }
}
