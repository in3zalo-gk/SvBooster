package com.turbo.gamebooster.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.turbo.gamebooster.core.Tweaks
import com.turbo.gamebooster.overlay.OverlayService
import com.turbo.gamebooster.shell.Shell
import kotlinx.coroutines.launch

@Composable
private fun rememberResumePermissionState(refreshKey: Int, checkPermission: () -> Boolean): Boolean =
    remember(refreshKey) { checkPermission() }

@Composable
private fun StatusLine(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
            tint = if (ok) Neon else Muted, modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun SetupScreen(tick: Int, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val installed = remember(tick) {
        try { ctx.packageManager.getApplicationInfo("moe.shizuku.privileged.api", 0); true }
        catch (_: Exception) { false }
    }
    val running = remember(tick) { Shell.shizukuRunning() }
    val granted = remember(tick) { Shell.shizukuGranted() }
    val overlay = rememberResumePermissionState(tick) { Settings.canDrawOverlays(ctx) }
    val dnd = remember(tick) { Tweaks.dndAllowed(ctx) }
    val notif = remember(tick) {
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
    var rootMsg by remember { mutableStateOf<String?>(null) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onChanged() }

    fun open(intent: Intent) = try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TurboModeCard(tick, onChanged)

        SectionCard("Shizuku", Icons.Filled.AdminPanelSettings) {
            StatusLine(installed, if (installed) "1/3 • Aplicativo Shizuku instalado" else "1/3 • Shizuku não instalado")
            StatusLine(running, if (running) "2/3 • Binder disponível (pingBinder OK)" else "2/3 • Binder indisponível — inicie o Shizuku")
            StatusLine(granted, if (granted) "3/3 • Permissão concedida" else "3/3 • Permissão deste app pendente")
            Spacer(Modifier.height(6.dp))
            Hint("Para iniciar sem root: abra o Shizuku → Iniciar por depuração sem fio. Em Opções do desenvolvedor, habilite Depuração sem fio e faça o pareamento indicado pelo Shizuku; depois volte aqui e conceda a permissão.")
            if (!running && installed) Hint("Se o binder não iniciar, confira se a Depuração sem fio continua ativa e tente iniciar novamente no app Shizuku. Em aparelhos MediaTek/Moto G24, se a versão da Play Store falhar, tente a versão oficial mais recente disponibilizada no GitHub e verifique as notas de compatibilidade do dispositivo.")
            Spacer(Modifier.height(10.dp))
            Spacer(Modifier.height(10.dp))
            when {
                granted -> {}
                running -> Button(onClick = { Shell.requestShizuku(); onChanged() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Conceder permissão")
                }
                installed -> OutlinedButton(onClick = {
                    ctx.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")?.let(::open)
                }, modifier = Modifier.fillMaxWidth()) { Text("Abrir Shizuku e iniciar") }
                else -> {
                    Button(onClick = {
                        open(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Baixar Shizuku") }

                }
            }
        }

        SectionCard("Root (alternativa)", Icons.Filled.Security) {
            StatusLine(Shell.rootAvailable, if (Shell.rootAvailable) "Root disponível" else "Root não verificado")
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    rootMsg = if (Shell.checkRoot()) "Root OK!" else "Root não encontrado"
                    onChanged()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Verificar root") }
            rootMsg?.let { Text(it, color = Cyan, fontSize = 13.sp) }
        }

        SectionCard("Permissões", Icons.Filled.Info) {
            StatusLine(overlay, "Sobreposição (HUD e mira)")
            if (!overlay) OutlinedButton(onClick = { openOverlaySettings(ctx) }, modifier = Modifier.fillMaxWidth()) {
                Text("Permitir sobreposição")
            }
            OutlinedButton(onClick = {
                if (Settings.canDrawOverlays(ctx)) OverlayService.start(ctx, hud = true, crosshair = false)
                else openOverlaySettings(ctx)
            }, modifier = Modifier.fillMaxWidth()) { Text(if (overlay) "Testar HUD" else "Testar HUD • conceder permissão") }
            Hint("Na Motorola, defina Shizuku e Sv Booster como ‘Sem restrições’ em Bateria → Uso da bateria para reduzir encerramentos em segundo plano.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) OutlinedButton(onClick = {
                open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }, modifier = Modifier.fillMaxWidth()) { Text("Abrir otimização de bateria") }
            StatusLine(dnd, "Acesso ao Não perturbe")
            if (!dnd) OutlinedButton(onClick = {
                open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }, modifier = Modifier.fillMaxWidth()) { Text("Permitir Não perturbe") }
            StatusLine(notif, "Notificações (botão Restaurar)")
            if (!notif && Build.VERSION.SDK_INT >= 33) OutlinedButton(onClick = {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }, modifier = Modifier.fillMaxWidth()) { Text("Permitir notificações") }
        }

        SectionCard("Como funciona", Icons.Filled.Info) {
            Hint(
                "• Resolução e FPS por jogo usam o Game Mode do Android (12+). Alguns jogos/fabricantes ignoram — " +
                    "nesse caso use a Resolução global.\n" +
                    "• O jogo é reiniciado ao dar Boost para aplicar a nova resolução.\n" +
                    "• A temperatura mostrada é a da bateria (o Android não libera a da CPU para apps).\n" +
                    "• Ao terminar, use 'Restaurar tudo' em Ferramentas ou na notificação."
            )
        }
    }
}
