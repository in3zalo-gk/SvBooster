package com.turbo.gamebooster

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.svbooster.pluginapi.PluginCategory
import com.svbooster.pluginapi.SvPlugin
import com.turbo.gamebooster.core.Booster
import com.turbo.gamebooster.core.Prefs
import com.turbo.gamebooster.core.Shortcuts
import com.turbo.gamebooster.core.SystemInfo
import com.turbo.gamebooster.plugins.PluginHostApi
import com.turbo.gamebooster.plugins.PluginRegistry
import com.turbo.gamebooster.shell.AdbShell
import com.turbo.gamebooster.shell.Shell
import com.turbo.gamebooster.ui.Amber
import com.turbo.gamebooster.ui.FilesScreen
import com.turbo.gamebooster.ui.GamesScreen
import com.turbo.gamebooster.ui.Muted
import com.turbo.gamebooster.ui.Neon
import com.turbo.gamebooster.ui.PickerScreen
import com.turbo.gamebooster.ui.ProfileScreen
import com.turbo.gamebooster.ui.SetupScreen
import com.turbo.gamebooster.ui.SvLogo
import com.turbo.gamebooster.ui.Tag
import com.turbo.gamebooster.ui.ToolsScreen
import com.turbo.gamebooster.ui.TurboTheme
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)
    private val permListener = Shizuku.OnRequestPermissionResultListener { _, _ -> tick.intValue++ }
    private val binderListener = Shizuku.OnBinderReceivedListener { tick.intValue++ }
    private val deadListener = Shizuku.OnBinderDeadListener { tick.intValue++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Shizuku.addRequestPermissionResultListener(permListener)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(deadListener)
        setContent { TurboTheme { App(tick.intValue) { tick.intValue++ } } }
        handleShortcut(intent)
        lifecycleScope.launch {
            if (AdbShell.paired() && !Shell.shizukuGranted()) {
                AdbShell.connect()
                tick.intValue++
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcut(intent)
    }

    private fun handleShortcut(i: Intent?) {
        val pkg = i?.getStringExtra(Shortcuts.EXTRA_BOOST) ?: return
        i.removeExtra(Shortcuts.EXTRA_BOOST)
        lifecycleScope.launch {
            if (AdbShell.paired()) AdbShell.connect()
            var waited = 0
            while (Shell.mode() == Shell.Mode.NONE && waited < 30) { delay(100); waited++ }
            Toast.makeText(this@MainActivity, "⚡ Sv Booster: preparando…", Toast.LENGTH_SHORT).show()
            Booster.boostAndLaunch(this@MainActivity, pkg, Prefs.profile(this@MainActivity, pkg)) {}
            moveTaskToBack(true)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh on every return from Android Settings, including overlay/Shizuku settings.
        tick.intValue++
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeBinderDeadListener(deadListener)
        super.onDestroy()
    }
}

private sealed interface Screen {
    data object Tabs : Screen
    data object Picker : Screen
    data class Profile(val pkg: String) : Screen
}

@Composable
private fun App(tick: Int, refresh: () -> Unit) {
    var screen by remember { mutableStateOf<Screen>(Screen.Tabs) }
    var selectedPage by rememberSaveable { mutableStateOf("home") }
    var subPage by rememberSaveable { mutableStateOf("games") }
    val context = LocalContext.current
    val mode = remember(tick) { Shell.mode() }
    val plugins = PluginRegistry.plugins
    val categories = plugins.map { it.category }.distinct()
    val hostApi = remember(context) { PluginHostApi(context) }

    when (val current = screen) {
        is Screen.Profile -> ProfileScreen(current.pkg, mode, onSetup = { screen = Screen.Tabs; selectedPage = "settings" }) {
            screen = Screen.Tabs
            refresh()
        }
        Screen.Picker -> PickerScreen { screen = Screen.Tabs; refresh() }
        Screen.Tabs -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { Header(mode) },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = selectedPage == "home", onClick = { selectedPage = "home" },
                        icon = { Icon(Icons.Filled.SportsEsports, null) }, label = { Text("Início", maxLines = 1, fontSize = 11.sp) },
                        colors = navColors()
                    )
                    categories.forEach { category ->
                        NavigationBarItem(
                            selected = selectedPage == category.name,
                            onClick = { selectedPage = category.name },
                            icon = { Icon(categoryIcon(category), null) },
                            label = { Text(category.title, maxLines = 1, fontSize = 10.sp) },
                            colors = navColors()
                        )
                    }
                    NavigationBarItem(
                        selected = selectedPage == "settings", onClick = { selectedPage = "settings" },
                        icon = { Icon(Icons.Filled.Settings, null) }, label = { Text("Ajustes", maxLines = 1, fontSize = 11.sp) },
                        colors = navColors()
                    )
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when {
                    selectedPage == "home" -> HomeDashboard(
                        tick = tick,
                        onOpenGame = { screen = Screen.Profile(it) },
                        onAddGame = { screen = Screen.Picker },
                        onSetup = { selectedPage = "settings" },
                        subPage = subPage,
                        onSubPage = { subPage = it },
                        onRefresh = refresh
                    )
                    selectedPage == "settings" -> SetupScreen(tick, refresh)
                    else -> {
                        val category = categories.firstOrNull { it.name == selectedPage }
                        val categoryPlugins = plugins.filter { it.category == category }
                        Column(Modifier.fillMaxSize()) {
                            categoryPlugins.forEach { plugin ->
                                PluginScreen(plugin, hostApi)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color.Black,
    indicatorColor = Neon
)

private fun categoryIcon(category: PluginCategory): ImageVector = when (category) {
    PluginCategory.OTIMIZACAO -> Icons.Filled.Bolt
    PluginCategory.MONITORAMENTO -> Icons.Filled.Build
    else -> Icons.Filled.Folder
}

@Composable
private fun PluginScreen(plugin: SvPlugin, hostApi: PluginHostApi) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Text(plugin.name, modifier = Modifier.padding(start = 18.dp, top = 16.dp), fontWeight = FontWeight.Bold)
        plugin.Content(context, hostApi)
    }
}

@Composable
private fun HomeDashboard(
    tick: Int,
    onOpenGame: (String) -> Unit,
    onAddGame: () -> Unit,
    onSetup: () -> Unit,
    subPage: String,
    onSubPage: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val ctx = LocalContext.current
    val activity = ctx as? MainActivity
    val stats = remember(tick) { SystemInfo.snapshot(ctx) }
    var cpuUsage by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            cpuUsage = withContext(Dispatchers.IO) { SystemInfo.cpuUsagePercent() }
            delay(2000)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(Modifier.weight(1f)) { Text("RAM\n${stats.ramUsedMb}/${stats.ramTotalMb} MB", Modifier.padding(10.dp), fontSize = 12.sp) }
                Card(Modifier.weight(1f)) { Text("CPU\n${cpuUsage?.let { "$it%" } ?: "lendo…"}", Modifier.padding(10.dp), fontSize = 12.sp) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(Modifier.weight(1f)) { Text("Térmica\n${stats.batteryTempC} °C", Modifier.padding(10.dp), fontSize = 12.sp) }
                Card(Modifier.weight(1f)) { Text("Shizuku\n${if (Shell.shizukuGranted()) "conectado" else "indisponível"}", Modifier.padding(10.dp), fontSize = 12.sp) }
            }
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = {
                    val pkg = Prefs.games(ctx).firstOrNull()
                    if (pkg == null) {
                        onSubPage("games")
                        Toast.makeText(ctx, "Adicione um jogo antes do Boost Rápido.", Toast.LENGTH_SHORT).show()
                    } else if (activity != null) {
                        activity.lifecycleScope.launch {
                            if (AdbShell.paired() && !AdbShell.connected) AdbShell.connect()
                            Booster.boostAndLaunch(activity, pkg, Prefs.profile(activity, pkg)) {}
                            onRefresh()
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()
            ) { Text("⚡ Boost Rápido") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { onSubPage("games") }, modifier = Modifier.weight(1f)) { Text("Jogos") }
                Button(onClick = { onSubPage("tools") }, modifier = Modifier.weight(1f)) { Text("Extras") }
                Button(onClick = { onSubPage("files") }, modifier = Modifier.weight(1f)) { Text("Arquivos") }
                Button(onClick = onSetup, modifier = Modifier.weight(1f)) { Text("Ajustes") }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (subPage) {
                "tools" -> ToolsScreen(Shell.mode())
                "files" -> FilesScreen(tick, onRefresh)
                else -> GamesScreen(tick, Shell.mode(), onOpen = onOpenGame, onAdd = onAddGame, onSetup = onSetup)
            }
        }
    }
}

@Composable
private fun Header(mode: Shell.Mode) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SvLogo(36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("SV BOOSTER", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
            Text("Mais FPS, menos travadas", color = Muted, fontSize = 11.sp)
        }
        when (mode) {
            Shell.Mode.NONE -> Tag("MODO BÁSICO", Amber)
            else -> Tag("TURBO ✓", Neon)
        }
    }
}
