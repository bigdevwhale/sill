package app.sill.data

import android.content.Context
import app.sill.AppContainer
import app.sill.domain.Rules
import app.sill.localized
import app.sill.ru

/**
 * A sill half a year in, for the demo build and the screenshots: five pots at every stage (a
 * sprout gone dry, a bloom waiting to move out, a friend's seed just sown), a garden out of the
 * window, compost, a dried flower in the vase and a tin with a gift, a wind seed and a self-sown one.
 */
object DemoSeed {
    private const val DAY = 86_400_000L

    suspend fun seedIfEmpty(context: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.plantings().isNotEmpty()) return
        val ru = context.localized().ru
        val now = System.currentTimeMillis()

        suspend fun sow(id: String, pot: Int, daysAgo: Int, place: Place, notes: List<Pair<Int, String>>, shape: Long, fertilized: Boolean = false, endedDaysAgo: Int = 0, from: String = "", fromNote: String = "") {
            val pid = dao.insert(
                Planting(
                    seed = "c:$id", pot = pot, plantedAt = now - daysAgo * DAY, place = place.name, shape = shape, fertilized = fertilized,
                    endedAt = if (place == Place.POT) 0 else now - endedDaysAgo * DAY, from = from, fromNote = fromNote,
                ),
            )
            notes.forEach { (ago, note) -> dao.insert(Watering(plantingId = pid, at = now - ago * DAY - 3_600_000L * (ago % 7), note = note)) }
        }

        fun n(en: String, r: String) = if (ru) r else en
        fun quiet(from: Int, to: Int, count: Int) = (0 until count).map { from - (from - to) * it / (count - 1).coerceAtLeast(1) to "" }

        // Out in the garden: these took.
        sow("ethiopian", 0, 230, Place.GARDEN, listOf(228 to n("Ate with my hands. Strange, then lovely.", "Ел руками. Странно, а потом прекрасно."), 214 to "", 201 to n("Ordered without the menu.", "Заказал, не глядя в меню.")), 11, endedDaysAgo = 200)
        sow("board_games", 1, 210, Place.GARDEN, listOf(208 to n("Lost every game. Came back anyway.", "Проиграл всё. Всё равно вернусь."), 194 to "", 180 to "", 166 to n("They saved me a seat.", "Мне заняли место.")), 23, endedDaysAgo = 165)
        sow("bread", 2, 190, Place.GARDEN, listOf(188 to n("A brick.", "Кирпич."), 181 to n("Less of a brick.", "Чуть меньше кирпич."), 170 to "", 160 to "", 150 to n("Ears on the crust!", "Ушко на корке!")), 37, endedDaysAgo = 150)
        sow("birdwatching", 3, 170, Place.GARDEN, listOf(168 to n("A wren, they say.", "Говорят, крапивник.")) + quiet(150, 110, 4), 41, endedDaysAgo = 110)
        sow("guitar", 4, 160, Place.GARDEN, listOf(158 to n("G, C, D. Fingers hurt.", "G, C, D. Болят пальцы.")) + quiet(155, 50, 28) + listOf(46 to n("Played a whole song by the fire.", "Сыграл целую песню у костра.")), 53, endedDaysAgo = 45)

        // Didn't take: compost, and one kept in the vase.
        sow("improv", 0, 120, Place.COMPOST, listOf(118 to n("Froze on stage. Laughed it off.", "Застыл на сцене. Посмеялись."), 104 to ""), 61, endedDaysAgo = 80)
        sow("open_mic", 1, 100, Place.COMPOST, listOf(99 to n("Not my room.", "Не моя комната.")), 67, endedDaysAgo = 70)
        sow("opera", 2, 90, Place.DRIED, listOf(88 to n("Three hours. Beautiful, not for me.", "Три часа. Красиво, но не моё.")), 71, endedDaysAgo = 60)

        // On the sill now.
        sow("language", 3, 64, Place.POT, listOf(63 to n("Hiragana, first row.", "Хирагана, первый ряд.")) + quiet(56, 6, 8) + listOf(1 to n("Read a sign in the shop!", "Прочитал вывеску в магазине!")), 83)
        sow("pottery", 1, 35, Place.POT, listOf(33 to n("Clay everywhere. The bowl fell.", "Глина везде. Миска упала."), 22 to "", 12 to n("Centred it for the first time.", "Впервые отцентровал."), 5 to n("A bowl that holds water.", "Миска, которая держит воду.")), 97, fertilized = true)
        sow("jazz_jam", 0, 20, Place.POT, listOf(18 to n("Didn't get it. Stayed for the drummer.", "Не понял. Остался ради барабанщика."), 3 to n("Recognised a standard.", "Узнал стандарт.")), 101)
        sow("cold_swim", 2, 26, Place.POT, listOf(12 to n("Twenty seconds. Screamed.", "Двадцать секунд. Орал.")), 113)
        sow(
            "throat_singing", 4, 2, Place.POT, emptyList(), 127,
            from = n("Masha", "Маша"), fromNote = n("Trust me, live it's something else.", "Поверь, вживую это совсем другое."),
        )

        // The tin.
        dao.insert(Packet(seed = "c:dance_social", origin = Origin.GIFT.name, from = n("Lena", "Лена"), note = n("Swing on Thursdays, come!", "Свинг по четвергам, приходи!"), addedAt = now - 4 * DAY))
        dao.insert(Packet(seed = "c:theremin", origin = Origin.WIND.name, addedAt = now - 1 * DAY))
        dao.insert(Packet(seed = "c:flamenco", origin = Origin.SELF.name, from = "c:guitar", addedAt = now - 45 * DAY))
        dao.insert(Packet(seed = "c:sup", origin = Origin.SAVED.name, addedAt = now - 30 * DAY))

        c.settings.update { it.copy(onboarded = true, name = n("Alex", "Саша"), windWeek = Rules.week(now)) }
    }
}
