# SvBooster Beta — plugins da comunidade

Guia para entender a arquitetura, criar módulos e importar plugins no SvBooster.

## Como funciona

O host apresenta Início, uma aba por `PluginCategory` registrada e Ajustes. Cada plugin implementa `SvPlugin`, que fornece identificador, nome, versão, categoria e uma tela Compose. O host chama essa tela e entrega uma instância de `HostApi`, a ponte para permissões Shizuku, comandos de shell, perfil de jogo, eventos de ciclo de vida e sobreposição.

Nesta beta, o carregamento é **em tempo de compilação**: o `PluginRegistry` registra as bibliotecas `:plugin-optimization` e `:plugin-monitoring`; por isso as categorias delas aparecem na navegação. Não há ainda loja, descoberta de APK instalado ou importação de APK externo.

## Criar um plugin

1. Crie um módulo Android Library Compose e dependa de `:plugin-api`.
2. Implemente `SvPlugin`, escolha um `id` reverso estável e uma categoria correta.
3. Faça a UI no método `Content(context, hostApi)` e use `HostApi` para capacidades do host.
4. Inclua o módulo em `settings.gradle.kts`, adicione-o às dependências de `:app` e registre-o no `PluginRegistry`.
5. Compile `./gradlew :app:assembleDebug` e teste em um aparelho real.

Exemplo mínimo:

```kotlin
object GpuExample : SvPlugin {
    override val id = "org.exemplo.svbooster.gpu"
    override val name = "GPU"
    override val version = "1.0.0"
    override val category = PluginCategory.RENDERIZACAO

    @Composable
    override fun Content(context: Context, hostApi: HostApi) {
        val scope = rememberCoroutineScope()
        Button(onClick = {
            scope.launch { hostApi.executeShell("cmd game list") }
        }) { Text("Consultar jogos") }
    }
}
```

Guia completo: [Criar um primeiro plugin](PLUGIN_GUIDE.md). Referência de contrato e capacidades: [HostApi](PLUGIN_API.md).

## Importar plugins

Para esta beta, “importar” significa adicionar o **código-fonte confiável** como um módulo Gradle, incluir sua dependência no host e registrar a implementação. Depois recompila-se e reinstala-se o APK do host. O plugin não é instalado nem removido separadamente.

**Importar APK diretamente pelo app ainda não é suportado.** Esse fluxo depende de configurar o runtime WXDynamicPlugin, distribuição e versionamento, verificação de assinatura, compatibilidade do contrato e rollback. Até lá, não instale APKs esperando que apareçam automaticamente. Passo a passo: [Importar plugins](IMPORTAR_PLUGINS.md) e [estado do runtime WX](WX_RUNTIME.md).

## Segurança e boas práticas

Plugins executam código dentro do processo do host; `HostApi` não é uma sandbox. Use fontes auditáveis, comandos seguros, idempotentes e reversíveis, e descreva os riscos. Não concatene entrada não confiável em comandos shell; peça autorização explícita para alterações e restaure os ajustes quando a sessão de jogo terminar.

## Roadmap

| Categoria | Ideias para a comunidade |
|---|---|
| Otimização | Downscale, limite de FPS, perfis e limpeza consciente |
| Renderização | Drivers gráficos, DPI e diagnóstico AOT, quando suportado |
| Rede | Latência, jitter e modos Wi-Fi compatíveis |
| Monitoramento | HUD, frame time, CPU/RAM e térmica, dentro das APIs disponíveis |
| Automação | Perfis por pacote, detecção OEM e rotinas pré-jogo |
| Experimental | Integrações root/Magisk/KernelSU e tweaks reversíveis |

Detalhes: [roadmap e compatibilidade](ROADMAP.md).
