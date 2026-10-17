package com.svbooster.pluginapi

import android.content.Context
import androidx.compose.runtime.Composable

/** Public, versioned contract implemented by SvBooster plugins. */
interface SvPlugin {
    val id: String
    val name: String
    val version: String
    val category: PluginCategory

    @Composable
    fun Content(context: Context, hostApi: HostApi)
}

enum class PluginCategory(val title: String) {
    OTIMIZACAO("Otimização"),
    RENDERIZACAO("Renderização"),
    REDE("Rede"),
    MONITORAMENTO("Monitoramento"),
    AUTOMACAO("Automação"),
    EXPERIMENTAL("Experimental")
}

data class ShellResult(val exitCode: Int, val stdout: String, val stderr: String) {
    val success: Boolean get() = exitCode == 0
}

data class GameProfile(
    val packageName: String,
    val downscale: Float? = null,
    val fpsLimit: Int? = null,
    val preset: String? = null
)

interface GameLifecycleListener {
    fun onGameStarted(packageName: String)
    fun onGameStopped(packageName: String)
}

/**
 * Capability-limited bridge into the host. Plugins must not declare privileged or dangerous
 * permissions to perform host operations; ask the user through the host instead.
 */
interface HostApi {
    /** Returns true only when Shizuku permission is already granted; otherwise requests it. */
    fun requestShizukuPermission(): Boolean

    /** Executes one user-initiated shell command off the main thread. */
    suspend fun executeShell(command: String): ShellResult

    /** Replaces the HUD contents with this Compose UI. Requires SYSTEM_ALERT_WINDOW consent. */
    fun showOverlay(content: @Composable () -> Unit)
    fun hideOverlay()

    fun getGameProfile(): GameProfile?
    fun registerGameLifecycleListener(listener: GameLifecycleListener)
}
