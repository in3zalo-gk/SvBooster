package com.turbo.gamebooster

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.svbooster.pluginapi.SvPlugin
import com.turbo.gamebooster.core.Booster
import com.turbo.gamebooster.core.Prefs
import com.turbo.gamebooster.core.Shortcuts
import com.turbo.gamebooster.core.SystemInfo
import com.turbo.gamebooster.plugins.LoadedPlugin
import com.turbo.gamebooster.plugins.PluginHostApi
import com.turbo.gamebooster.plugins.PluginPackageManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var pluginMessage by remember { mutableStateOf<String?>(null) }
    var pluginsReady by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode = remember(tick) { Shell.mode() }
    val pluginEntries = PluginRegistry.entries
    val pluginIds = pluginEntries.map { it.plugin.id }
    val hostApi = remember(context) { PluginHostApi(context) }

    LaunchedEffect(Unit) {
        try {
            val loaded = withContext(Dispatchers.IO) {
                PluginPackageManager.loadInstalled(context.applicationContext)
            }
            PluginRegistry.replaceAll(loaded.plugins)
            pluginMessage = loaded.errors.takeIf { it.isNotEmpty() }?.joinToString("\n")
        } catch (t: Throwable) {
            pluginMessage = "Falha ao iniciar plugins: ${t.message ?: "erro inesperado"}"
        } finally {
            pluginsReady = true
        }
    }
    LaunchedEffect(pluginIds) {
        if (selectedPage.startsWith("plugin:") && selectedPage.removePrefix("plugin:") !in pluginIds) {
            selectedPage = "home"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val existingIds = PluginRegistry.entries.map { it.plugin.id }.toSet()
            pluginMessage = "Validando pacote .plgs…"
            scope.launch {
                try {
                    val loaded = withContext(Dispatchers.IO) {
                        PluginPackageManager.importPlugin(context.applicationContext, uri, existingIds)
                    }
                    PluginRegistry.add(loaded)
                    selectedPage = "plugin:${loaded.plugin.id}"
                    pluginMessage = "${loaded.plugin.name} importado."
                } catch (t: Throwable) {
                    pluginMessage = t.message ?: "Não foi possível importar esse .plgs."
                }
            }
        }
    }

    val removePlugin: (LoadedPlugin) -> Unit = { entry ->
        scope.launch {
            try {
                withContext(Dispatchers.IO) { PluginPackageManager.removePlugin(context.applicationContext, entry) }
                PluginRegistry.remove(entry.plugin.id)
                pluginMessage = "${entry.plugin.name} removido. Reinicie o app para descarregar completamente o código."
            } catch (t: Throwable) {
                pluginMessage = t.message ?: "Não foi possível remover o plugin."
            }
        }
    }

    when (val current = screen) {
        is Screen.Profile -> ProfileScreen(
            current.pkg,
            mode,
            onSetup = { screen = Screen.Tabs; selectedPage = "settings" },
            onBack = { screen = Screen.Tabs; refresh() }
        )
        Screen.Picker -> PickerScreen { screen = Screen.Tabs; refresh() }
        Screen.Tabs -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { Header(mode) },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavTab("Início", selectedPage == "home", {
                            Icon(Icons.Filled.SportsEsports, "Início")
                        }) { selectedPage = "home" }
                        pluginEntries.forEach { entry ->
                            BottomNavTab(entry.plugin.name, selectedPage == "plugin:${entry.plugin.id}", {
                                PluginNavIcon(entry)
                            }) { selectedPage = "plugin:${entry.plugin.id}" }
                        }
                        BottomNavTab("Ajustes", selectedPage == "settings", {
                            Icon(Icons.Filled.Settings, "Ajustes")
                        }) { selectedPage = "settings" }
                    }
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
                    selectedPage == "settings" -> SetupScreen(
                        tick = tick,
                        onChanged = refresh,
                        plugins = pluginEntries.toList(),
                        pluginMessage = pluginMessage,
                        onImportPlugin = {
                            if (pluginsReady) importLauncher.launch(arrayOf("*/*"))
                            else pluginMessage = "Aguarde o carregamento dos plugins antes de importar outro arquivo."
                        },
                        onRemovePlugin = removePlugin
                    )
                    else -> {
                        val entry = pluginEntries.firstOrNull { "plugin:${it.plugin.id}" == selectedPage }
                        if (entry != null) PluginScreen(entry, hostApi)
                        else HomeDashboard(
                            tick = tick,
                            onOpenGame = { screen = Screen.Profile(it) },
                            onAddGame = { screen = Screen.Picker },
                            onSetup = { selectedPage = "settings" },
                            subPage = subPage,
                            onSubPage = { subPage = it },
                            onRefresh = refresh
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavTab(label: String, selected: Boolean, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Column(
        Modifier.width(78.dp).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.width(42.dp).height(26.dp).clip(RoundedCornerShape(14.dp))
                .background(if (selected) Neon else Color.Transparent),
            contentAlignment = Alignment.Center
        ) { icon() }
        Text(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 9.sp,
            color = if (selected) MaterialTheme.colorScheme.onSurface else Muted
        )
    }
}

@Composable
private fun PluginNavIcon(entry: LoadedPlugin) {
    val bitmap = remember(entry.iconFile.absolutePath) {
        BitmapFactory.decodeFile(entry.iconFile.absolutePath)?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription = entry.plugin.name, modifier = Modifier.size(20.dp))
    } else {
        Icon(Icons.Filled.Extension, contentDescription = entry.plugin.name, tint = Color.Black, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PluginScreen(entry: LoadedPlugin, hostApi: PluginHostApi) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Text(entry.plugin.name, modifier = Modifier.padding(start = 18.dp, top = 16.dp), fontWeight = FontWeight.Bold)
        Text("v${entry.plugin.version}", modifier = Modifier.padding(start = 18.dp, top = 2.dp), color = Muted, fontSize = 12.sp)
        entry.plugin.Content(context, hostApi)
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
