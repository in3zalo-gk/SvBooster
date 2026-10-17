package com.svbooster.diagnostics

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.svbooster.pluginapi.HostApi
import com.svbooster.pluginapi.PluginCategory
import com.svbooster.pluginapi.ShellResult
import com.svbooster.pluginapi.SvPlugin
import kotlinx.coroutines.launch

private data class DiagnosticResult(
    val label: String,
    val command: String,
    val status: String,
    val elapsedMs: Long,
    val output: String
)

object DiagnosticsPlugin : SvPlugin {
    override val id = "org.svbooster.diagnostics"
    override val name = "Diagnóstico Shizuku"
    override val version = "1.0.0"
    override val category = PluginCategory.EXPERIMENTAL

    @Composable
    override fun Content(context: Context, hostApi: HostApi) {
        val results = remember { mutableStateListOf<DiagnosticResult>() }
        val scope = rememberCoroutineScope()
        val running = remember { androidx.compose.runtime.mutableStateOf(false) }

        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Diagnóstico somente leitura", style = MaterialTheme.typography.titleMedium)
            Text("Executa comandos curtos pela mesma ponte shell dos plugins e mede o tempo de resposta. Não altera configurações do sistema.")
            Button(
                enabled = !running.value,
                onClick = {
                    scope.launch {
                        running.value = true
                        results.clear()
                        val checks = listOf(
                            "Identidade shell" to "id -un",
                            "Android" to "getprop ro.build.version.release",
                            "Build do sistema" to "getprop ro.build.display.id",
                            "Resolução da tela" to "wm size"
                        )
                        for ((label, command) in checks) {
                            val started = System.nanoTime()
                            val result = try {
                                hostApi.executeShell(command)
                            } catch (t: Throwable) {
                                ShellResult(-1, "", t.message ?: t.javaClass.simpleName)
                            }
                            val elapsed = (System.nanoTime() - started) / 1_000_000L
                            val status = when {
                                result.success -> "OK"
                                result.exitCode == -2 -> "TIMEOUT"
                                result.exitCode == -1 -> "SHELL / PERMISSÃO"
                                else -> "ERRO (${result.exitCode})"
                            }
                            val output = (if (result.success) result.stdout else result.stderr.ifBlank { result.stdout })
                                .trim().ifBlank { if (result.success) "Comando concluído sem saída." else "Sem detalhes retornados." }
                            results += DiagnosticResult(label, command, status, elapsed, output.take(500))
                        }
                        running.value = false
                    }
                }
            ) { Text(if (running.value) "Executando…" else "Executar diagnóstico") }

            if (results.isEmpty() && !running.value) {
                Text("Toque para testar acesso ao shell. Se o Shizuku não estiver ativo ou autorizado, o erro será mostrado por comando.")
            }
            results.forEach { item ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.label, style = MaterialTheme.typography.titleSmall)
                        Text("${item.status} • ${item.elapsedMs} ms", color = if (item.status == "OK") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                    Text(item.command, style = MaterialTheme.typography.labelSmall)
                    Text(item.output, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Comandos: id -un, getprop e wm size. Nenhum comando de escrita é executado.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
