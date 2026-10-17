# WXDynamicPlugin: avaliação e trabalho restante

A referência escolhida é [wgllss/WXDynamicPlugin](https://github.com/wgllss/WXDynamicPlugin). O projeto declara suporte a Compose e carregamento dinâmico. A documentação do upstream descreve uma arquitetura com runtime, geração de artefatos/versionamento e hospedagem de arquivos; portanto, adicionar somente uma dependência Maven ou chamar um `ClassLoader` não constitui integração correta.

## O que já está no fork

- Contrato de UI Compose em `:plugin-api`.
- Registro e navegação por categorias usando `SvPlugin`.
- Dois exemplos carregados pelo registro do host: `:plugin-optimization` e `:plugin-monitoring`.
- Uma ponte `HostApi` para shell, HUD, perfil e eventos.

## O que ainda falta para runtime de APK externo

- Integrar os módulos/runtime oficiais do WXDynamicPlugin e fixar um commit/release compatível com AGP, Kotlin e Compose usados pelo host.
- Definir o mecanismo de entrada dos plugins, preservando identidade de classe do `:plugin-api` e versão binária do contrato.
- Configurar serviço de distribuição, manifesto/índice de versões, geração/assinatura dos pacotes e política de atualização/rollback.
- Implementar download autenticado, verificação criptográfica, armazenamento local, falhas offline, compatibilidade host-plugin e tela de loja/instalação.
- Adicionar testes de carregamento em aparelho real, upgrade, remoção, falha de plugin e regressão de Compose.

Até que esses pontos sejam implementados e auditados, **não use a etiqueta “plugin APK carregado dinamicamente” para os exemplos atuais**. Eles são módulos de biblioteca integrados em build-time, não APKs separados. A tela de categorias é dinâmica em relação aos plugins registrados no host, não em relação a APKs instalados.

## Decisão técnica

WXDynamicPlugin é preferido a Shadow por declarar suporte nativo a Compose; Shadow exigiria adaptar conteúdo Compose via `ComposeView`. A integração foi isolada do contrato para que o adaptador futuro não altere a API dos plugins. Consulte o README upstream para a configuração atual e valide cada instrução contra o commit que for fixado.
