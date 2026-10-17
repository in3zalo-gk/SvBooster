package com.turbo.gamebooster.plugins

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import com.svbooster.monitoring.MonitoringPlugin
import com.svbooster.optimization.OptimizationPlugin
import com.svbooster.pluginapi.GameLifecycleListener
import com.svbooster.pluginapi.GameProfile as ApiGameProfile
import com.svbooster.pluginapi.HostApi
import com.svbooster.pluginapi.PluginCategory
import com.svbooster.pluginapi.ShellResult
import com.svbooster.pluginapi.SvPlugin
import com.turbo.gamebooster.core.Prefs
import com.turbo.gamebooster.overlay.OverlayService
import com.turbo.gamebooster.shell.Shell
import java.util.concurrent.CopyOnWriteArraySet

/** Built-in examples are registered through the same contract used by future WX-loaded modules. */
object PluginRegistry {
    val plugins = mutableStateListOf<SvPlugin>()

    init {
        register(OptimizationPlugin)
        register(MonitoringPlugin)
    }

    @Synchronized
    fun register(plugin: SvPlugin) {
        require(plugin.id.isNotBlank()) { "Plugin id must not be blank" }
        val previous = plugins.indexOfFirst { it.id == plugin.id }
        if (previous >= 0) plugins.removeAt(previous)
        plugins.add(plugin)
    }

    fun categories(): List<PluginCategory> = plugins.map { it.category }.distinct()
}

private object GameEvents {
    val listeners = CopyOnWriteArraySet<GameLifecycleListener>()
    fun started(pkg: String) = listeners.forEach { it.onGameStarted(pkg) }
    fun stopped(pkg: String) = listeners.forEach { it.onGameStopped(pkg) }
}

object PluginGameEvents {
    fun started(packageName: String) = GameEvents.started(packageName)
    fun stopped(packageName: String) = GameEvents.stopped(packageName)
}

class PluginHostApi(context: Context) : HostApi {
    private val appContext = context.applicationContext

    override fun requestShizukuPermission(): Boolean {
        if (Shell.shizukuGranted()) return true
        Shell.requestShizuku()
        return false
    }

    override suspend fun executeShell(command: String): ShellResult {
        // Called only from explicit plugin UI actions; execution is delegated to the host shell layer.
        val result = Shell.run(command)
        return ShellResult(result.code, result.out, result.err)
    }

    override fun showOverlay(content: @Composable () -> Unit) {
        OverlayService.showPlugin(appContext, content)
    }

    override fun hideOverlay() {
        OverlayService.stop(appContext)
    }

    override fun getGameProfile(): ApiGameProfile? {
        val pkg = Prefs.lastGame(appContext) ?: return null
        val profile = Prefs.profile(appContext, pkg)
        return ApiGameProfile(
            packageName = pkg,
            downscale = profile.downscale,
            fpsLimit = profile.fps.takeIf { it > 0 },
            preset = null
        )
    }

    override fun registerGameLifecycleListener(listener: GameLifecycleListener) {
        GameEvents.listeners += listener
    }
}
