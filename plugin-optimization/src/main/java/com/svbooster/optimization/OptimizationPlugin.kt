package com.svbooster.optimization

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.svbooster.pluginapi.HostApi
import com.svbooster.pluginapi.PluginCategory
import com.svbooster.pluginapi.SvPlugin
import kotlinx.coroutines.launch

object OptimizationPlugin : SvPlugin {
    override val id = "org.svbooster.optimization"
    override val name = "Otimização de jogos"
    override val version = "1.0.0"
    override val category = PluginCategory.OTIMIZACAO

    @Composable
    override fun Content(context: Context, hostApi: HostApi) {
        var packageName by remember { mutableStateOf("") }
        var scale by remember { mutableFloatStateOf(0.8f) }
        var result by remember { mutableStateOf("Informe o pacote do jogo e aplique por sua conta.") }
        val scope = rememberCoroutineScope()
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Redução de resolução por jogo")
            Text("O jogo pode precisar ser reiniciado. O suporte depende da versão do Android e do fabricante.")
            OutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it.filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' } },
                label = { Text("Pacote do jogo") },
                placeholder = { Text("com.exemplo.jogo") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Escala")
                Text("${(scale * 100).toInt()}%")
            }
            Slider(value = scale, onValueChange = { scale = it }, valueRange = 0.5f..1f)
            Button(
                enabled = packageName.contains('.') && packageName.length >= 5,
                onClick = {
                    scope.launch {
                        if (!hostApi.requestShizukuPermission()) {
                            result = "Autorize o acesso Shizuku e tente novamente."
                        } else {
                            val r = hostApi.executeShell("cmd game set --mode 2 --downscale ${"%.2f".format(java.util.Locale.US, scale)} $packageName")
                            result = if (r.success) "Comando aplicado. Reinicie o jogo para validar." else "Não aplicado: ${r.stderr.ifBlank { r.stdout }}"
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()
            ) { Text("Aplicar otimização") }
            Text(result)
        }
    }
}
