package app.sill.domain

import app.sill.data.Section
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * A seed packet given to a friend, as plain text that goes through any messenger. The last line
 * carries the packet itself: `sill:c/<id>?from=…&note=…` for the catalogue, or `sill:own?…` with
 * the whole hand-written packet in it. The lines above are for the person reading the message.
 */
data class Gift(
    /** "c:<id>" for a catalogue seed; blank for a hand-written one. */
    val key: String,
    val from: String,
    val note: String,
    val section: Section = Section.HERBS,
    val name: String = "",
    val about: String = "",
    val start: String = "",
    val bloom: Int = 5,
) {
    val own: Boolean get() = key.isBlank()
}

object Gifts {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

    fun encode(g: Gift): String {
        val q = buildList {
            if (g.from.isNotBlank()) add("from=${enc(g.from)}")
            if (g.note.isNotBlank()) add("note=${enc(g.note)}")
            if (g.own) {
                add("s=${g.section.name}")
                add("b=${g.bloom}")
                add("n=${enc(g.name)}")
                if (g.about.isNotBlank()) add("a=${enc(g.about)}")
                if (g.start.isNotBlank()) add("st=${enc(g.start)}")
            }
        }.joinToString("&")
        val head = if (g.own) "sill:own" else "sill:c/${g.key.removePrefix("c:")}"
        return if (q.isEmpty()) head else "$head?$q"
    }

    /** The message: a line for a person, then the packet. */
    fun message(g: Gift, title: String, invite: String): String = buildString {
        append("🌱 ").append(title)
        if (g.note.isNotBlank()) append(" — ").append(g.note)
        append("\n").append(invite).append("\n\n").append(encode(g))
    }

    fun parse(text: String): Gift? {
        val line = text.lineSequence().map { it.trim() }.lastOrNull { it.startsWith("sill:") } ?: return null
        val head = line.substringBefore('?')
        val q = line.substringAfter('?', "").split('&').filter { '=' in it }
            .associate { it.substringBefore('=') to runCatching { dec(it.substringAfter('=')) }.getOrDefault("") }
        val from = q["from"].orEmpty().take(40)
        val note = q["note"].orEmpty().take(200)
        return when {
            head.startsWith("sill:c/") -> head.removePrefix("sill:c/").takeIf { id -> id.isNotBlank() && id.all { it.isLetterOrDigit() || it == '_' } }
                ?.let { Gift("c:$it", from, note) }
            head == "sill:own" -> q["n"]?.takeIf { it.isNotBlank() }?.let { name ->
                Gift(
                    key = "", from = from, note = note, section = Section.of(q["s"]), name = name.take(60),
                    about = q["a"].orEmpty().take(300), start = q["st"].orEmpty().take(300), bloom = (q["b"]?.toIntOrNull() ?: 5).coerceIn(2, 60),
                )
            }
            else -> null
        }
    }
}
