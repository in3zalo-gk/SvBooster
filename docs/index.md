# SvBooster Beta — plugins `.plgs`

Guia para entender o formato de plugin, criar pacotes e importar pelo app.

## Como funciona

A barra inferior mostra Início, Ajustes e **uma aba por plugin**. Cada aba usa o nome e o ícone PNG declarados no pacote. Os plugins implementam `SvPlugin` e desenham a tela com Compose; a ponte `HostApi` disponibiliza as capacidades do host.

Um `.plgs` é um contêiner ZIP com `manifest.json`, `classes.dex` e `icon.png`. Os exemplos `:plugin-optimization`, `:plugin-monitoring` e `:plugin-diagnostics` também são empacotados nesse formato e carregados pelo mesmo caminho dos plugins importados. Cada plugin tem uma aba e um ícone próprios; o diagnóstico executa testes Shizuku somente de leitura. O host não instala APK de plugin.

## Criar um plugin

1. Crie um módulo Android/Kotlin com Compose e dependa de `:plugin-api` e das bibliotecas Compose como `compileOnly`.
2. Implemente `SvPlugin`, com ID reverso e versão estável.
3. Crie `manifest.json` e `icon.png` com os mesmos metadados da classe.
4. Compile um APK de debug do módulo apenas para gerar `classes.dex` e empacote-o com `tools/package_plgs.py`.
5. Copie o `.plgs` ao celular e importe-o em **Ajustes → Plugins**.

Passos completos: [Criar plugins](PLUGIN_GUIDE.md) · [Importar `.plgs`](IMPORTAR_PLUGINS.md) · [Contrato `HostApi`](PLUGIN_API.md).

## Validação do pacote

O app exige extensão `.plgs`, `formatVersion=1`, `apiVersion=1`, exatamente os três arquivos previstos, categoria conhecida, IDs únicos e limites de tamanho. Outros arquivos, incluindo APKs e ZIPs comuns, são recusados.

## Segurança

`.plgs` **não é sandbox nem assinatura de confiança**. O DEX roda dentro do processo do host e pode usar a identidade e os acessos do app. Importe apenas arquivos cujo código e origem você confia. Não execute comandos shell sem validação, consentimento, limite de tempo e plano de reversão.

## Status do WXDynamicPlugin

WXDynamicPlugin não é usado pelo carregador atual; continua apenas como uma alternativa avaliada. Detalhes e limites: [runtime e segurança](WX_RUNTIME.md).

## Roadmap

[Compatibilidade, ideias e boas práticas](ROADMAP.md).
