package app.sill.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import app.sill.R
import app.sill.data.Origin
import app.sill.data.Section
import app.sill.domain.Stage
import app.sill.domain.Thirst
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Words and dates in the app's language. */
object T {
    private fun locale(c: Context): Locale = c.resources.configuration.locales[0]
    private fun zone() = ZoneId.systemDefault()

    fun plural(c: Context, id: Int, n: Int): String = c.resources.getQuantityString(id, n, n)

    /** "24 October", or with the year when it isn't this one. */
    fun date(c: Context, at: Long, today: LocalDate = LocalDate.now()): String {
        val d = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate()
        return d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "d MMMM" else "d MMMM yyyy", locale(c)))
    }

    /** "12.03.26", for a herbarium label. */
    fun stamp(at: Long): String = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yy"))

    /** "today", "yesterday", "5 days ago", or the date for anything older than two weeks. */
    fun ago(c: Context, at: Long, today: LocalDate = LocalDate.now()): String {
        val d = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate()
        val days = java.time.temporal.ChronoUnit.DAYS.between(d, today).toInt()
        return when {
            days <= 0 -> c.getString(R.string.today)
            days == 1 -> c.getString(R.string.yesterday)
            days < 14 -> plural(c, R.plurals.days_ago, days)
            else -> date(c, at, today)
        }
    }

    /** "Oct–Feb" style sowing months, or "all year". */
    fun months(c: Context, months: Set<Int>): String {
        if (months.isEmpty()) return c.getString(R.string.all_year)
        val names = months.sorted().map { Month.of(it).getDisplayName(TextStyle.SHORT_STANDALONE, locale(c)).trimEnd('.') }
        return names.joinToString(", ")
    }

    fun section(c: Context, s: Section): String = c.getString(
        when (s) {
            Section.HERBS -> R.string.s_herbs
            Section.FLOWERS -> R.string.s_flowers
            Section.ROOTS -> R.string.s_roots
            Section.GRAINS -> R.string.s_grains
            Section.VINES -> R.string.s_vines
            Section.CACTI -> R.string.s_cacti
            Section.MUSHROOMS -> R.string.s_mushrooms
            Section.TREES -> R.string.s_trees
        },
    )

    /** What a section holds: "tastes, kitchens, drinks". */
    fun holds(c: Context, s: Section): String = c.getString(
        when (s) {
            Section.HERBS -> R.string.h_herbs
            Section.FLOWERS -> R.string.h_flowers
            Section.ROOTS -> R.string.h_roots
            Section.GRAINS -> R.string.h_grains
            Section.VINES -> R.string.h_vines
            Section.CACTI -> R.string.h_cacti
            Section.MUSHROOMS -> R.string.h_mushrooms
            Section.TREES -> R.string.h_trees
        },
    )

    fun stage(c: Context, s: Stage): String = c.getString(
        when (s) {
            Stage.SEED -> R.string.st_seed
            Stage.SPROUT -> R.string.st_sprout
            Stage.LEAVES -> R.string.st_leaves
            Stage.BUD -> R.string.st_bud
            Stage.BLOOM -> R.string.st_bloom
        },
    )

    fun thirst(c: Context, t: Thirst): String = c.getString(
        when (t) {
            Thirst.FRESH -> R.string.th_fresh
            Thirst.DRY -> R.string.th_dry
            Thirst.WILTED -> R.string.th_wilted
        },
    )

    /** "A gift from Masha", "The wind brought it", "Self-sown from Guitar", "Put aside". */
    fun origin(c: Context, o: Origin, from: String): String = when (o) {
        Origin.GIFT -> c.getString(R.string.o_gift, from.ifBlank { c.getString(R.string.a_friend) })
        Origin.WIND -> c.getString(R.string.o_wind)
        Origin.SELF -> c.getString(R.string.o_self, from)
        Origin.SAVED -> c.getString(R.string.o_saved)
    }
}

object Language {
    const val SYSTEM = ""
    const val EN = "en"
    const val RU = "ru"

    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}

@Composable
fun ctx(): Context = LocalContext.current
