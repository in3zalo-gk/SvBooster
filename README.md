# SvBooster — Game Booster modular para Android

Fork técnico do SvBooster com Kotlin, Jetpack Compose, Shizuku/Depuração por Wi‑Fi, diagnóstico de permissões e uma base modular para plugins. O app mantém as funções existentes de perfis por jogo, downscale/FPS, modo turbo e HUD.

## O que foi refatorado

- **Abas por plugin:** Início → uma aba individual por `.plgs`, com o ícone próprio do pacote → Ajustes. A barra inferior desliza horizontalmente quando há muitos plugins.
- **Contrato público:** módulo `:plugin-api` com `SvPlugin`, `PluginCategory`, `HostApi`, `GameProfile` e callbacks de ciclo de vida.
- **Plugins `.plgs`:** `:plugin-optimization`, `:plugin-monitoring` e `:plugin-diagnostics` viram pacotes separados com manifesto, DEX e ícone; todos são carregados pelo mesmo runtime dos plugins importados. O diagnóstico tem uma aba própria e testa comandos shell somente de leitura via Shizuku, com status e tempo por comando.
- **Diagnóstico Shizuku:** UI separa instalação, binder (`pingBinder`) e autorização do app; orienta início pela Depuração sem fio.
- **Overlay:** HUD de teste em Ajustes; a permissão de sobreposição é reavaliada ao voltar ao app. O overlay Compose registra LifecycleOwner e SavedStateRegistryOwner.
- **Home:** painel de RAM/temperatura/estado Shizuku e botão Boost Rápido.
- **Bateria Motorola:** instruções para deixar Shizuku e SvBooster sem restrições de bateria.
- **Docs da comunidade:** guia de plugin, referência da API, roadmap e nota de integração WX em `docs/`.

## Formato e limite de segurança

O host importa arquivos `.plgs` com manifesto, `classes.dex` e `icon.png`; os exemplos também são empacotados nesse formato e publicados como artefato do Actions. Não se instala APK de plugin: APK é apenas insumo do empacotamento. O carregador valida extensão, estrutura, limites de tamanho e API, mas **não é sandbox nem comprova assinatura/autoria**. Plugins executam código dentro do processo do host; importe somente arquivos de fontes confiáveis.

## Compilar

- Requisitos: JDK 17, Android SDK 35 e Android Studio/Gradle Wrapper.
- Host e pacotes embutidos `.plgs`: `./gradlew :app:assembleDebug`
- Contrato e plugins de exemplo: `./gradlew :plugin-api:build :plugin-optimization:assembleDebug :plugin-monitoring:assembleDebug :plugin-diagnostics:assembleDebug`
- Testes: `./gradlew test` e, com emulador conectado, `./gradlew connectedDebugAndroidTest`
- O workflow `.github/workflows/build.yml` compila o host e publica `SvBooster-beta-debug` e `SvBooster-example-plugins-plgs` como artefatos do GitHub Actions por 30 dias. O segundo inclui os três pacotes `.plgs` de exemplo.

APK host: `app/build/outputs/apk/debug/app-debug.apk`. Os `.plgs` de exemplo ficam em `app/build/generated/pluginAssets/plugins/` e também são publicados em um artefato separado do Actions.

## Configuração do Shizuku / Depuração sem fio

1. Instale Shizuku da fonte oficial e abra-o.
2. Em Opções do desenvolvedor, habilite **Depuração sem fio**; siga o fluxo de pareamento do app Shizuku e inicie o serviço.
3. No SvBooster, confira os três estados em Ajustes: aplicativo instalado, binder disponível e permissão concedida. Autorize o app na tela do Shizuku.
4. Em Motorola, defina **Shizuku** e **SvBooster** como **Sem restrições** no uso de bateria.
5. Para o modo turbo próprio do SvBooster, use o cartão “Modo Turbo” e siga o pareamento guiado por notificação.

Em alguns aparelhos MediaTek, uma versão do Shizuku pode falhar ao iniciar. Confira a versão oficial atual e as notas de compatibilidade antes de instalar APKs alternativos. [Releases oficiais do Shizuku](https://github.com/RikkaApps/Shizuku/releases).

## Desenvolver plugins

Comece por [`docs/PLUGIN_GUIDE.md`](docs/PLUGIN_GUIDE.md). Veja também [`docs/IMPORTAR_PLUGINS.md`](docs/IMPORTAR_PLUGINS.md), a referência da API em [`docs/PLUGIN_API.md`](docs/PLUGIN_API.md) e o [`roadmap`](docs/ROADMAP.md). APKs não são importados: o usuário seleciona arquivos `.plgs`. Plugins devem solicitar ações pela `HostApi`, mostrar erros e reverter ajustes quando o jogo termina.

## Licença e créditos

Preserve a licença e os avisos do projeto original. O upstream escolhido para a futura camada de carregamento é [WXDynamicPlugin](https://github.com/wgllss/WXDynamicPlugin). Confirme licenças e compatibilidade de dependências antes de publicar binários derivados.
