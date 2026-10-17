# Contrato público `:plugin-api`

A API está em `plugin-api/src/main/java/com/svbooster/pluginapi/SvPlugin.kt`. Mantenha compatibilidade semântica e incremente versões de plugin quando o comportamento mudar.

## `SvPlugin`

- `id: String`: identificador estável e globalmente único (use domínio reverso).
- `name: String`: nome para a navegação.
- `version: String`: versão do plugin.
- `category: PluginCategory`: grupo exibido como aba dinâmica.
- `Content(context, hostApi)`: tela Compose raiz. O host chama o composable dentro da aba da categoria.

Categorias disponíveis: `OTIMIZACAO`, `RENDERIZACAO`, `REDE`, `MONITORAMENTO`, `AUTOMACAO` e `EXPERIMENTAL`.

## `HostApi`

| Método | Uso e retorno | Requisito / observações |
|---|---|---|
| `requestShizukuPermission(): Boolean` | Retorna `true` se já estiver autorizado; caso contrário, dispara o pedido e retorna `false`. | Shizuku instalado, binder em execução e decisão do usuário. Reavalie depois que a tela de autorização fechar. |
| `suspend executeShell(command): ShellResult` | Suspende até terminar e retorna `exitCode`, `stdout`, `stderr` e `success`. | Shizuku, modo ADB próprio ou root; o Android/fabricante ainda pode negar o comando. Chamar de coroutine, não bloquear a UI. |
| `showOverlay(content)` | Exibe um composable no serviço de sobreposição do host. | Permissão Android “Exibir sobre outros apps” e serviço foreground disponível. O overlay é arrastável pelo HUD nativo; o conteúdo customizado fica em janela tocável. |
| `hideOverlay()` | Para o serviço de overlay do host. | Não requer permissão adicional. |
| `getGameProfile()` | Perfil selecionado mais recentemente, ou `null` se nenhum jogo tiver sido selecionado. | A estrutura atual retorna pacote, downscale, FPS (se definido) e preset (ainda nulo). |
| `registerGameLifecycleListener(listener)` | Registra callbacks de início/fim de sessão. | Eventos de início vêm do fluxo de Boost; os de encerramento dependem do monitor de sessão do host estar ativo. |

## Modelos

`ShellResult(exitCode, stdout, stderr)` — `success` é derivado de `exitCode == 0`.

`GameProfile(packageName, downscale, fpsLimit, preset)` — os campos opcionais podem estar ausentes; não assuma que o usuário selecionou jogo.

`GameLifecycleListener` declara `onGameStarted(packageName)` e `onGameStopped(packageName)`.

## Segurança e permissões

Os plugins de exemplo não solicitam permissões privilegiadas próprias. O host declara e gerencia as permissões necessárias ao seu funcionamento. A permissão de sobreposição continua sendo uma autorização explícita Android do usuário. `HostApi` não é sandbox: um plugin carregado no mesmo processo pode executar código Kotlin com as capacidades do host. Distribua somente artefatos confiáveis, valide assinatura/origem no futuro carregador e nunca trate `executeShell` como uma fronteira de segurança suficiente.
