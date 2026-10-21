package app.sill

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.sill.data.Catalogue
import app.sill.data.DemoSeed
import app.sill.data.Garden
import app.sill.data.SettingsStore
import app.sill.data.SillDatabase
import app.sill.domain.Gift
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val db = SillDatabase.create(context)
    val settings = SettingsStore(context)
    val catalogue = Catalogue.load(context)
    val garden = Garden(context, db, catalogue)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** A packet shared into the app by a friend, waiting to go into the tin. */
    val gift = MutableStateFlow<Gift?>(null)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class SillApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (BuildConfig.DEMO) container.scope.launch { DemoSeed.seedIfEmpty(this@SillApp, container) }
    }
}

val Context.container: AppContainer get() = (applicationContext as SillApp).container

/** Resources in the in-app language, which AppCompat doesn't localise outside activities before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
