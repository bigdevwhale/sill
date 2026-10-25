package app.sill.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.sill.BuildConfig
import app.sill.R
import app.sill.container
import app.sill.ui.Go
import app.sill.ui.Home
import app.sill.ui.Language
import app.sill.ui.components.CommunityLinks
import app.sill.ui.components.Field
import app.sill.ui.components.SectionTitle
import app.sill.ui.components.Segmented
import app.sill.ui.components.TopBar
import app.sill.ui.ctx
import app.sill.ui.theme.SType

@Composable
fun SettingsScreen(home: Home, go: Go) {
    val c = ctx()
    var name by remember { mutableStateOf(home.settings.name) }
    var lang by remember { mutableStateOf(Language.current()) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), go::back)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            SectionTitle(stringResource(R.string.your_name))
            Field(name, { v -> name = v.take(40); c.container.write { settings.update { it.copy(name = v.take(40).trim()) } } }, stringResource(R.string.your_name_hint))
            Text(stringResource(R.string.your_name_sub), style = SType.Muted, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
            SectionTitle(stringResource(R.string.language))
            Segmented(
                listOf(Language.SYSTEM, Language.EN, Language.RU), lang,
                { stringResource(when (it) { Language.EN -> R.string.lang_en; Language.RU -> R.string.lang_ru; else -> R.string.lang_system }) },
                { lang = it; Language.set(it) },
            )
            SectionTitle(stringResource(R.string.community))
            CommunityLinks()
            SectionTitle(stringResource(R.string.about))
            Text(stringResource(R.string.about_text), style = SType.Body)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.privacy), style = SType.Muted)
            Spacer(Modifier.height(16.dp))
            Text("Sill ${BuildConfig.VERSION_NAME} · Stillcraft", style = SType.Small)
            Spacer(Modifier.height(32.dp))
        }
    }
}
