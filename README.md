# SvBooster — Game Booster modular para Android

Fork técnico do SvBooster com Kotlin, Jetpack Compose, Shizuku/Depuração por Wi‑Fi, diagnóstico de permissões e uma base modular para plugins. O app mantém as funções existentes de perfis por jogo, downscale/FPS, modo turbo e HUD.

## O que foi refatorado

- **Abas dinâmicas:** Início → categorias presentes nos plugins registrados → Ajustes.
- **Contrato público:** módulo `:plugin-api` com `SvPlugin`, `PluginCategory`, `HostApi`, `GameProfile` e callbacks de ciclo de vida.
- **Exemplos Compose:** `:plugin-optimization` (downscale via shell, com validação do pacote) e `:plugin-monitoring` (telemetria básica e HUD customizado).
- **Diagnóstico Shizuku:** UI separa instalação, binder (`pingBinder`) e autorização do app; orienta início pela Depuração sem fio.
- **Overlay:** HUD de teste em Ajustes; a permissão de sobreposição é reavaliada ao voltar ao app. O overlay Compose registra LifecycleOwner e SavedStateRegistryOwner.
- **Home:** painel de RAM/temperatura/estado Shizuku e botão Boost Rápido.
- **Bateria Motorola:** instruções para deixar Shizuku e SvBooster sem restrições de bateria.
- **Docs da comunidade:** guia de plugin, referência da API, roadmap e nota de integração WX em `docs/`.

## Limite importante desta entrega

Os exemplos são Android Library Modules compilados dentro do APK host e registrados no `PluginRegistry`. **Eles ainda não são APKs externos baixáveis nem carregados em runtime.** WXDynamicPlugin declara suporte a Compose, mas o upstream exige integração do runtime, versão/artefatos e servidor de distribuição. Não afirmamos que o carregamento dinâmico esteja concluído. Verifique `docs/WX_RUNTIME.md` antes de distribuir ou prometer plugins externos.

## Compilar

- Requisitos: JDK 17, Android SDK 35 e Android Studio/Gradle Wrapper.
- Host: `./gradlew :app:assembleDebug`
- Contrato e exemplos: `./gradlew :plugin-api:build :plugin-optimization:build :plugin-monitoring:build`
- Testes: `./gradlew test` e, com emulador conectado, `./gradlew connectedDebugAndroidTest`
- O workflow `.github/workflows/build.yml` compila o host e publica o APK como artefato do GitHub Actions por 30 dias. Abra a execução concluída, baixe `SvBooster-beta-debug` em **Artifacts** e instale o APK de debug.

APK host: `app/build/outputs/apk/debug/app-debug.apk`. Os módulos de plugin de exemplo, por serem bibliotecas, não produzem APKs separados. Esta beta ainda precisa ser validada por build e teste em aparelho; não use para aplicar comandos que não entenda.

## Configuração do Shizuku / Depuração sem fio

1. Instale Shizuku da fonte oficial e abra-o.
2. Em Opções do desenvolvedor, habilite **Depuração sem fio**; siga o fluxo de pareamento do app Shizuku e inicie o serviço.
3. No SvBooster, confira os três estados em Ajustes: aplicativo instalado, binder disponível e permissão concedida. Autorize o app na tela do Shizuku.
4. Em Motorola, defina **Shizuku** e **SvBooster** como **Sem restrições** no uso de bateria.
5. Para o modo turbo próprio do SvBooster, use o cartão “Modo Turbo” e siga o pareamento guiado por notificação.

Em alguns aparelhos MediaTek, uma versão do Shizuku pode falhar ao iniciar. Confira a versão oficial atual e as notas de compatibilidade antes de instalar APKs alternativos. [Releases oficiais do Shizuku](https://github.com/RikkaApps/Shizuku/releases).

## Desenvolver plugins

Comece por [`docs/PLUGIN_GUIDE.md`](docs/PLUGIN_GUIDE.md). Veja também [`docs/IMPORTAR_PLUGINS.md`](docs/IMPORTAR_PLUGINS.md), a referência da API em [`docs/PLUGIN_API.md`](docs/PLUGIN_API.md) e o [`roadmap`](docs/ROADMAP.md). Na beta, importar APK externo ainda não está disponível: exemplos são integrados ao host durante a compilação. Plugins não devem declarar permissões privilegiadas próprias; devem operar pela `HostApi`, mostrar erros e reverter ajustes quando o jogo termina.

## Licença e créditos

Preserve a licença e os avisos do projeto original. O upstream escolhido para a futura camada de carregamento é [WXDynamicPlugin](https://github.com/wgllss/WXDynamicPlugin). Confirme licenças e compatibilidade de dependências antes de publicar binários derivados.
