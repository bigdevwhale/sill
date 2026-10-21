package app.sill.domain

import app.sill.data.Place
import app.sill.data.Planting
import app.sill.data.Section
import app.sill.data.Seed
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.random.Random

/** How far along a plant is. */
enum class Stage { SEED, SPROUT, LEAVES, BUD, BLOOM }

/** How long since it was last watered, against how often it wants water. */
enum class Thirst { FRESH, DRY, WILTED }

/** The rules of the sill. Pure, so the tests can hold them. */
object Rules {
    /** Five pots: room enough to try things, too little to start twenty and drop them all. */
    const val POTS = 5

    /** Compost knocks a fifth off the waterings a new planting needs, at least one, never below two. */
    fun target(seed: Seed, fertilized: Boolean): Int =
        if (fertilized) (seed.bloom - maxOf(1, seed.bloom / 5)).coerceAtLeast(2) else seed.bloom

    /** 0 when sown, 1 when it blooms. */
    fun growth(waterings: Int, target: Int): Float = (waterings.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)

    fun stage(waterings: Int, target: Int): Stage {
        val g = growth(waterings, target)
        return when {
            waterings <= 0 -> Stage.SEED
            g >= 1f -> Stage.BLOOM
            g >= 0.75f -> Stage.BUD
            g >= 0.34f || waterings >= 3 -> Stage.LEAVES
            else -> Stage.SPROUT
        }
    }

    /**
     * After how many tries something new usually starts to feel good: the packet's "germination".
     * Roughly a third of the way to bloom, never the first time.
     */
    fun germination(seed: Seed): Int = maxOf(2, ceil(seed.bloom * 0.3).toInt())

    /** Long skills want water twice a week, everything else once. */
    fun interval(section: Section): Int = if (section == Section.TREES) 4 else 7

    fun days(from: Long, now: Long): Int {
        val z = ZoneId.systemDefault()
        return ChronoUnit.DAYS.between(Instant.ofEpochMilli(from).atZone(z).toLocalDate(), Instant.ofEpochMilli(now).atZone(z).toLocalDate()).toInt().coerceAtLeast(0)
    }

    /** Thirst from the last watering, or from the sowing if it was never watered. Blooms don't thirst. */
    fun thirst(section: Section, lastWater: Long?, plantedAt: Long, now: Long, stage: Stage): Thirst {
        if (stage == Stage.BLOOM) return Thirst.FRESH
        val d = days(lastWater ?: plantedAt, now)
        val i = interval(section)
        return when {
            d < i -> Thirst.FRESH
            d < 2 * i -> Thirst.DRY
            else -> Thirst.WILTED
        }
    }

    /** How much the leaves hang: 0 fresh, 1 wilted. */
    fun droop(t: Thirst): Float = when (t) {
        Thirst.FRESH -> 0f
        Thirst.DRY -> 0.45f
        Thirst.WILTED -> 1f
    }

    /** Compost left to feed a planting: everything composted, less what was already sown on it. */
    fun compost(plantings: List<Planting>): Int =
        plantings.count { it.placeOf == Place.COMPOST } - plantings.count { it.fertilized }

    /** The first free pot, or null when all five are taken. */
    fun freePot(plantings: List<Planting>): Int? {
        val taken = plantings.filter { it.placeOf == Place.POT }.map { it.pot }.toSet()
        return (0 until POTS).firstOrNull { it !in taken }
    }

    /** Weeks since the epoch, Monday-based: the wind blows once in each. */
    fun week(now: Long): Long = Math.floorDiv(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay() + 3, 7)

    fun month(now: Long): Int = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).monthValue

    /**
     * What the wind brings this week: a seed from the section you've sown least, never one you've
     * sown or already have in the tin, in season if it can be. The same answer all week.
     */
    fun wind(all: List<Seed>, plantings: List<Planting>, inTin: Set<String>, now: Long): Seed? {
        val sown = plantings.map { it.seed }.toSet()
        val open = all.filter { !it.own && it.key !in sown && it.key !in inTin }
        if (open.isEmpty()) return null
        val sections = plantings.mapNotNull { p -> all.firstOrNull { it.key == p.seed }?.section }.groupingBy { it }.eachCount()
        val rnd = Random(week(now))
        val far = Section.entries.shuffled(rnd).minBy { sections[it] ?: 0 }
        val month = month(now)
        val pool = open.filter { it.section == far }.ifEmpty { open }
        return pool.filter { it.inSeason(month) }.ifEmpty { pool }.shuffled(rnd).first()
    }

    /** What a bloom drops into the tin: its neighbours you haven't sown and don't have yet. */
    fun selfSown(bloom: Seed, plantings: List<Planting>, inTin: Set<String>): List<String> {
        val sown = plantings.map { it.seed }.toSet()
        return bloom.neighbors.filter { it !in sown && it !in inTin }.take(2)
    }

    fun year(at: Long): Int = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).year

    fun today(now: Long): LocalDate = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
}
