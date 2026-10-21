package app.sill

import app.sill.data.Catalogue
import app.sill.data.Place
import app.sill.data.Planting
import app.sill.data.Section
import app.sill.data.Seed
import app.sill.domain.Gift
import app.sill.domain.Gifts
import app.sill.domain.Rules
import app.sill.domain.Stage
import app.sill.domain.Thirst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RulesTest {
    private val catalogue = Catalogue.parse(File("src/main/assets/seeds.tsv").readText())
    private fun seed(id: String) = catalogue["c:$id"]!!
    private val day = 86_400_000L

    @Test fun `catalogue is whole`() {
        assertTrue(catalogue.seeds.size >= 120)
        Section.entries.forEach { s -> assertTrue("$s is thin", catalogue.seeds.count { it.section == s } >= 15) }
        catalogue.seeds.forEach { s ->
            assertTrue("${s.key} has no name", s.nameEn.isNotBlank() && s.nameRu.isNotBlank())
            assertTrue("${s.key} has no start", s.startEn.isNotBlank() && s.startRu.isNotBlank())
            assertTrue("${s.key} blooms too fast", s.bloom >= 3)
            assertTrue("${s.key} depth", s.depth in 1..3)
            s.neighbors.forEach { n -> assertNotNull("${s.key} → $n", catalogue[n]) }
        }
        assertEquals(catalogue.seeds.size, catalogue.seeds.map { it.key }.toSet().size)
    }

    @Test fun `stages follow the waterings`() {
        assertEquals(Stage.SEED, Rules.stage(0, 6))
        assertEquals(Stage.SPROUT, Rules.stage(1, 6))
        assertEquals(Stage.LEAVES, Rules.stage(3, 6))
        assertEquals(Stage.BUD, Rules.stage(5, 6))
        assertEquals(Stage.BLOOM, Rules.stage(6, 6))
        assertEquals(Stage.BLOOM, Rules.stage(9, 6))
        // A radish: one watering is a sprout, the third is the bloom.
        assertEquals(Stage.SPROUT, Rules.stage(1, 3))
        assertEquals(Stage.LEAVES, Rules.stage(2, 3))
        assertEquals(Stage.BLOOM, Rules.stage(3, 3))
    }

    @Test fun `compost shortens the road but never below two`() {
        assertEquals(3, Rules.target(seed("ethiopian"), false))
        assertEquals(2, Rules.target(seed("ethiopian"), true))
        assertEquals(40, Rules.target(seed("cello"), false))
        assertEquals(32, Rules.target(seed("cello"), true))
    }

    @Test fun `compost left is composted minus what grew on it`() {
        val list = listOf(
            Planting(seed = "c:a", pot = 0, place = Place.COMPOST.name),
            Planting(seed = "c:b", pot = 0, place = Place.COMPOST.name),
            Planting(seed = "c:c", pot = 1, fertilized = true),
            Planting(seed = "c:d", pot = 2, place = Place.GARDEN.name),
        )
        assertEquals(1, Rules.compost(list))
    }

    @Test fun `five pots and the first free one`() {
        val full = (0 until 5).map { Planting(seed = "c:$it", pot = it) }
        assertNull(Rules.freePot(full))
        assertEquals(2, Rules.freePot(full.filter { it.pot != 2 }))
        // Moved out of a pot frees it.
        assertEquals(0, Rules.freePot(full.mapIndexed { i, p -> if (i == 0) p.copy(place = Place.GARDEN.name) else p }))
    }

    @Test fun `thirst grows with the days and blooms never thirst`() {
        val now = 100 * day
        assertEquals(Thirst.FRESH, Rules.thirst(Section.FLOWERS, now - 3 * day, 0, now, Stage.LEAVES))
        assertEquals(Thirst.DRY, Rules.thirst(Section.FLOWERS, now - 9 * day, 0, now, Stage.LEAVES))
        assertEquals(Thirst.WILTED, Rules.thirst(Section.FLOWERS, now - 20 * day, 0, now, Stage.LEAVES))
        assertEquals(Thirst.DRY, Rules.thirst(Section.TREES, now - 5 * day, 0, now, Stage.LEAVES))
        assertEquals(Thirst.FRESH, Rules.thirst(Section.FLOWERS, now - 60 * day, 0, now, Stage.BLOOM))
        // Never watered: counted from the sowing.
        assertEquals(Thirst.DRY, Rules.thirst(Section.HERBS, null, now - 8 * day, now, Stage.SEED))
    }

    @Test fun `wind brings a far seed, the same all week, never one you have`() {
        val now = 1_760_000_000_000L
        val sown = catalogue.seeds.filter { it.section == Section.HERBS }.take(6).map { Planting(seed = it.key, pot = 0) }
        val a = Rules.wind(catalogue.seeds, sown, emptySet(), now)!!
        val b = Rules.wind(catalogue.seeds, sown, emptySet(), now + day)
        assertNotNull(a)
        assertFalse(sown.any { it.seed == a.key })
        assertTrue(a.section != Section.HERBS)
        if (Rules.week(now) == Rules.week(now + day)) assertEquals(a.key, b!!.key)
        val again = Rules.wind(catalogue.seeds, sown, setOf(a.key), now)!!
        assertTrue(again.key != a.key)
    }

    @Test fun `self-sown are unsown neighbours`() {
        val guitar = seed("guitar")
        val sown = listOf(Planting(seed = "c:blues", pot = 0))
        val dropped = Rules.selfSown(guitar, sown, setOf("c:music_theory"))
        assertEquals(listOf("c:flamenco"), dropped)
    }

    @Test fun `gifts round trip`() {
        val g = Gift("c:pottery", "Маша", "Попробуй, мне зашло & ещё")
        assertEquals(g, Gifts.parse(Gifts.message(g, "Гончарный круг", "invite")))
        val own = Gift("", "Lena", "", Section.CACTI, "Sea kayaking", "Paddling = fun", "Book a tour", 8)
        assertEquals(own, Gifts.parse("hello\n" + Gifts.encode(own)))
        assertNull(Gifts.parse("just a message"))
        assertNull(Gifts.parse("sill:c/../etc"))
    }

    @Test fun `germination is never the first try`() {
        assertEquals(2, Rules.germination(seed("ethiopian")))
        assertEquals(12, Rules.germination(seed("cello")))
    }

    @Test fun `own seed keys`() {
        val s = Seed.own(app.sill.data.OwnSeed(id = 4, section = "TREES", name = "Kayak", bloom = 12))
        assertEquals("o:4", s.key)
        assertEquals(Section.TREES, s.section)
        assertTrue(s.own)
    }
}
