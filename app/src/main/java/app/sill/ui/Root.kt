package app.sill.ui

import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.sill.R
import app.sill.container
import app.sill.data.Catalogue
import app.sill.data.Origin
import app.sill.data.OwnSeed
import app.sill.data.Packet
import app.sill.data.Place
import app.sill.data.Planting
import app.sill.data.Seed
import app.sill.data.Settings
import app.sill.data.Watering
import app.sill.domain.Rules
import app.sill.domain.Stage
import app.sill.domain.Thirst
import app.sill.ru
import app.sill.ui.art.PlantLook
import app.sill.ui.art.PotView
import app.sill.ui.catalogue.CatalogueScreen
import app.sill.ui.components.rememberNow
import app.sill.ui.garden.GardenScreen
import app.sill.ui.onboarding.WelcomeScreen
import app.sill.ui.own.OwnSeedScreen
import app.sill.ui.packet.PacketScreen
import app.sill.ui.plant.PlantScreen
import app.sill.ui.settings.SettingsScreen
import app.sill.ui.sill.SillScreen
import app.sill.ui.theme.S
import app.sill.ui.tin.TinScreen
import app.sill.ui.year.YearScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where a seed stands with you. */
enum class Standing { NONE, TIN, POT, GARDEN, COMPOST, DRIED }

/** Everything written down, plus what follows from it, at one moment. Screens read through here. */
@Immutable
class Home(
    val plantings: List<Planting>,
    val waterings: List<Watering>,
    val own: List<OwnSeed>,
    val packets: List<Packet>,
    val settings: Settings,
    val catalogue: Catalogue,
    val now: Long,
    val ru: Boolean,
) {
    private val byPlanting = waterings.groupBy { it.plantingId }
    private val ownSeeds = own.map(Seed::own).associateBy { it.key }

    fun seed(key: String): Seed? = catalogue[key] ?: ownSeeds[key]
    fun planting(id: Long) = plantings.firstOrNull { it.id == id }

    /** Every seed there is: the catalogue, then the hand-written packets. */
    val seeds: List<Seed> = catalogue.seeds + ownSeeds.values

    /** A planting's waterings, the latest first. */
    fun waterings(id: Long): List<Watering> = byPlanting[id].orEmpty().sortedByDescending { it.at }
    fun count(id: Long) = byPlanting[id]?.size ?: 0
    fun lastWater(id: Long): Long? = byPlanting[id]?.maxOfOrNull { it.at }

    fun target(p: Planting): Int = seed(p.seed)?.let { Rules.target(it, p.fertilized) } ?: 5
    fun stage(p: Planting): Stage = Rules.stage(count(p.id), target(p))
    fun growth(p: Planting): Float = Rules.growth(count(p.id), target(p))

    fun thirst(p: Planting): Thirst {
        if (p.placeOf != Place.POT) return Thirst.FRESH
        val s = seed(p.seed) ?: return Thirst.FRESH
        return Rules.thirst(s.section, lastWater(p.id), p.plantedAt, now, stage(p))
    }

    fun look(p: Planting, pressed: Boolean = false): PlantLook {
        val s = seed(p.seed)
        val stage = if (p.placeOf == Place.GARDEN) Stage.BLOOM else stage(p)
        return PlantLook(
            section = s?.section ?: app.sill.data.Section.HERBS, kind = s?.hash ?: p.seed.hashCode(), shape = p.shape,
            growth = if (p.placeOf == Place.GARDEN) 1f else growth(p), stage = stage, droop = Rules.droop(thirst(p)), pressed = pressed,
        )
    }

    fun view(p: Planting) = PotView(look(p), p.fertilized)

    /** The five pots, empty ones as null. */
    val pots: List<Planting?> = (0 until Rules.POTS).map { i -> plantings.firstOrNull { it.placeOf == Place.POT && it.pot == i } }
    val sown: List<Planting> = pots.filterNotNull()
    val garden: List<Planting> = plantings.filter { it.placeOf == Place.GARDEN }.sortedByDescending { it.endedAt }
    val compost: List<Planting> = plantings.filter { it.placeOf == Place.COMPOST }.sortedByDescending { it.endedAt }
    val dried: List<Planting> = plantings.filter { it.placeOf == Place.DRIED }.sortedByDescending { it.endedAt }
    val compostLeft: Int = Rules.compost(plantings)
    val freePot: Int? = Rules.freePot(plantings)

    /** The latest planting of a seed, if it was ever sown. */
    fun plantingOf(key: String): Planting? = plantings.filter { it.seed == key }.maxByOrNull { it.plantedAt }
    fun packetOf(key: String): Packet? = packets.firstOrNull { it.seed == key }

    fun standing(key: String): Standing = when (plantingOf(key)?.placeOf) {
        Place.POT -> Standing.POT
        Place.GARDEN -> Standing.GARDEN
        Place.COMPOST -> if (packetOf(key) != null) Standing.TIN else Standing.COMPOST
        Place.DRIED -> if (packetOf(key) != null) Standing.TIN else Standing.DRIED
        null -> if (packetOf(key) != null) Standing.TIN else Standing.NONE
    }

    /** When the last seed was sown: the last time something was tried for the first time. */
    val lastSown: Long? = plantings.maxOfOrNull { it.plantedAt }

    val thirsty: List<Planting> = sown.filter { thirst(it) != Thirst.FRESH }
    val blooming: List<Planting> = sown.filter { stage(it) == Stage.BLOOM }

    /** The newest seed the wind brought this week, still unsown: it lies on the sill. */
    val windPacket: Packet? = packets.filter { it.originOf == Origin.WIND && now - it.addedAt < 7 * 86_400_000L }.maxByOrNull { it.addedAt }
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun catalogue() = nav.navigate("catalogue") { launchSingleTop = true }
    fun packet(key: String, tin: Long = 0) = nav.navigate("packet/${key.replace(':', '~')}/$tin") { launchSingleTop = true }
    fun plant(id: Long) = nav.navigate("plant/$id") { launchSingleTop = true }
    fun garden() = nav.navigate("garden") { launchSingleTop = true }
    fun year() = nav.navigate("year") { launchSingleTop = true }
    fun tin() = nav.navigate("tin") { launchSingleTop = true }
    fun own() = nav.navigate("own") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun back() { nav.popBackStack() }

    /** Just sown: back to the sill, where the new pot is. */
    fun sill() { nav.popBackStack("sill", false) }

    /** A packet just written by hand: its front, instead of the form. */
    fun written(key: String) = nav.navigate("packet/${key.replace(':', '~')}/0") { popUpTo("catalogue") }
}

@Composable
fun SillRoot(settings: Settings) {
    val context = ctx()
    val container = context.container
    val nav = rememberNavController()
    val start = remember { if (settings.onboarded) "sill" else "welcome" }

    val dao = container.db.dao()
    val plantings by dao.observePlantings().collectAsStateWithLifecycle(initialValue = emptyList())
    val waterings by dao.observeWaterings().collectAsStateWithLifecycle(initialValue = emptyList())
    val own by dao.observeOwn().collectAsStateWithLifecycle(initialValue = emptyList())
    val packets by dao.observePackets().collectAsStateWithLifecycle(initialValue = emptyList())
    val now = rememberNow(60_000)
    val ru = context.ru
    val home = remember(plantings, waterings, own, packets, settings, now, ru) { Home(plantings, waterings, own, packets, settings, container.catalogue, now, ru) }
    val go = remember(nav) { Go(nav) }

    // Onboarded elsewhere (the demo seed): leave the welcome screen.
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("sill") { popUpTo("welcome") { inclusive = true } }
    }
    // Once a week the wind leaves a seed on the sill.
    LaunchedEffect(settings.onboarded, now / 3_600_000L) {
        if (settings.onboarded) container.scope.launch { container.garden.wind(container.settings) }
    }
    // A packet shared in by a friend: into the tin, and open its front.
    val gift by container.gift.collectAsStateWithLifecycle()
    LaunchedEffect(gift, settings.onboarded) {
        val g = gift ?: return@LaunchedEffect
        if (!settings.onboarded) return@LaunchedEffect
        container.gift.value = null
        container.scope.launch {
            val id = container.garden.receive(g)
            val key = if (id != null) dao.packet(id)?.seed else null
            withContext(Dispatchers.Main) {
                if (key == null) {
                    Toast.makeText(context, R.string.gift_unknown, Toast.LENGTH_LONG).show()
                } else {
                    val who = g.from.ifBlank { context.getString(R.string.a_friend) }
                    Toast.makeText(context, context.getString(R.string.gift_received, who), Toast.LENGTH_LONG).show()
                    go.packet(key, id ?: 0)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(S.c.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(S.c.bg),
            enterTransition = { fadeIn(tween(260)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            val id = listOf(navArgument("id") { type = NavType.LongType })
            composable("welcome") { WelcomeScreen { nav.navigate("sill") { popUpTo("welcome") { inclusive = true } } } }
            composable("sill") { SillScreen(home, go) }
            composable("catalogue") { CatalogueScreen(home, go) }
            composable("packet/{key}/{tin}", arguments = listOf(navArgument("key") { type = NavType.StringType }, navArgument("tin") { type = NavType.LongType })) { e ->
                PacketScreen(e.arguments?.getString("key").orEmpty().replace('~', ':'), e.arguments?.getLong("tin") ?: 0L, home, go)
            }
            composable("plant/{id}", arguments = id) { e -> PlantScreen(e.arguments?.getLong("id") ?: 0L, home, go) }
            composable("garden") { GardenScreen(home, go) }
            composable("year") { YearScreen(home, go) }
            composable("tin") { TinScreen(home, go) }
            composable("own") { OwnSeedScreen(home, go) }
            composable("settings") { SettingsScreen(home, go) }
        }
    }
}
