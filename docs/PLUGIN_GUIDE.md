# Como criar seu primeiro plugin

Este guia cria um módulo Compose que usa `:plugin-api` e pede ao host para executar uma ação. Os módulos de exemplo atuais são Android Library incluídas no app; o processo de empacotar e carregar APK externo depende da integração WXDynamicPlugin documentada em [WX_RUNTIME.md](WX_RUNTIME.md).

## 1. Adicione um módulo

No `settings.gradle.kts`:

```kotlin
include(":plugin-example")
```

Crie `plugin-example/build.gradle.kts` seguindo `plugin-optimization/build.gradle.kts`: Android Library, Kotlin Android, plugin Compose, SDK 35 e dependência `implementation(project(":plugin-api"))`. Para o exemplo ficar visível no host desta versão, adicione também `implementation(project(":plugin-example"))` em `app/build.gradle.kts`.

## 2. Implemente `SvPlugin`

```kotlin
object ExampleGpuPlugin : SvPlugin {
    override val id = "com.example.svbooster.gpu"
    override val name = "GPU experimental"
    override val version = "1.0.0"
    override val category = PluginCategory.RENDERIZACAO

    @Composable
    override fun Content(context: Context, hostApi: HostApi) {
        var status by remember { mutableStateOf("Pronto") }
        val scope = rememberCoroutineScope()
        Column {
            Button(onClick = {
                scope.launch {
                    // Use somente uma operação documentada e acionada pelo usuário.
                    val result = hostApi.executeShell("cmd game list")
                    status = if (result.success) result.stdout else result.stderr
                }
            }) { Text("Consultar jogos") }
            Text(status)
        }
    }
}
```

Para uma alteração de GPU, use somente comandos e opções suportados pelo Android e pelo fabricante. A chamada ilustrativa abaixo não garante suporte universal e não troca driver em todos os aparelhos:

```kotlin
val result = hostApi.executeShell("cmd game set --mode 2 --downscale 0.80 com.exemplo.jogo")
```

O exemplo `:plugin-optimization` filtra o nome do pacote para caracteres permitidos antes de formar a chamada. Não concatene texto livre recebido de rede, arquivo ou campo de usuário em comandos shell.

## 3. Registre o plugin

Registre-o no `PluginRegistry` do host ou use o ponto de entrada que o adaptador WX vier a definir. Nesta versão, o registro é em tempo de build e as categorias visíveis são derivadas dos plugins registrados. Isso não representa descoberta automática de APKs instalados.

## 4. Compile e teste

- `./gradlew :plugin-example:build` compila o módulo.
- `./gradlew :app:assembleDebug` compila o host com o módulo incluído.
- Instale o APK de debug gerado pelo host e teste em aparelho real com uma cópia de segurança e um jogo não crítico.
- Sem Shizuku/root/Depuração por Wi-Fi autorizada, `executeShell` deve falhar com diagnóstico retornado; mostre esse resultado ao usuário.

## Boas práticas

- Peça confirmação explícita antes de aplicar um ajuste persistente.
- Prefira operações idempotentes, reversíveis e com restauração automática ao fechar o jogo.
- Use `GameLifecycleListener` para desfazer ajustes que dependem da sessão de jogo.
- Informe claramente limitações de fabricante, versão Android, root e permissões.
- Não declare permissões perigosas no plugin para contornar o host. A ponte host é o local de controle e deve ser auditada.
- Nunca execute scripts recebidos de uma URL sem validação, assinatura e consentimento.
