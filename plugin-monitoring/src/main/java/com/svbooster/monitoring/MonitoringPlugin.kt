package com.svbooster.monitoring

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.svbooster.pluginapi.HostApi
import com.svbooster.pluginapi.PluginCategory
import com.svbooster.pluginapi.SvPlugin

object MonitoringPlugin : SvPlugin {
    override val id = "org.svbooster.monitoring"
    override val name = "Monitoramento e HUD"
    override val version = "1.0.0"
    override val category = PluginCategory.MONITORAMENTO

    @Composable
    override fun Content(context: Context, hostApi: HostApi) {
        val metrics = remember {
            val am = context.getSystemService(ActivityManager::class.java)
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = (battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
            val totalMb = info.totalMem / 1_048_576
            val usedMb = totalMb - info.availMem / 1_048_576
            Triple("$usedMb / $totalMb MB", "$temp °C", info.lowMemory)
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Telemetria do aparelho")
            Text("RAM usada: ${metrics.first}")
            Text("Temperatura da bateria: ${metrics.second}")
            Text(if (metrics.third) "Memória baixa detectada" else "Memória dentro do estado normal")
            Button(onClick = {
                hostApi.showOverlay {
                    Text("SV • HUD ativo", modifier = Modifier.padding(12.dp))
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Mostrar HUD de teste") }
            Button(onClick = hostApi::hideOverlay, modifier = Modifier.fillMaxWidth()) { Text("Ocultar HUD") }
            Text("O Android pode limitar a precisão térmica e a leitura de CPU em apps sem privilégios.")
        }
    }
}
