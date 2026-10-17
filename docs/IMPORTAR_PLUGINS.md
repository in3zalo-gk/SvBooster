# Importar plugins `.plgs` no SvBooster

## Importação no Android

1. Abra **Ajustes → Plugins → Importar arquivo .plgs**.
2. Selecione o pacote `.plgs` no seletor de arquivos.
3. O host valida a extensão, o manifesto, a versão da API, a classe de entrada, o DEX e o ícone antes de carregá-lo.
4. O plugin aparece como uma aba própria na barra inferior, com o nome e o ícone incluídos no pacote. Se houver várias abas, deslize a barra horizontalmente.

Somente arquivos cujo nome termina em `.plgs` e cuja estrutura interna é válida são aceitos. Renomear um APK/ZIP não transforma o conteúdo em um plugin válido: o arquivo precisa ser um pacote SvBooster com exatamente `manifest.json`, `classes.dex` e `icon.png`. Pacotes inválidos, entradas extras e IDs já instalados são recusados.

Plugins importados podem ser removidos em **Ajustes → Plugins**. Para descarregar completamente uma classe já carregada pelo Android, feche e reabra o app após remover o pacote.

## O que é um `.plgs`

É um arquivo ZIP com a extensão própria `.plgs` e três itens na raiz:

```text
manifest.json
classes.dex
icon.png
```

Exemplo de `manifest.json`:

```json
{
  "formatVersion": 1,
  "apiVersion": 1,
  "id": "com.example.svbooster.gpu",
  "name": "GPU experimental",
  "version": "1.0.0",
  "category": "RENDERIZACAO",
  "entryClass": "com.example.svbooster.gpu.GpuPlugin",
  "icon": "icon.png"
}
```

`entryClass` precisa implementar `com.svbooster.pluginapi.SvPlugin`. O ID, nome, versão e categoria do objeto precisam corresponder aos valores do manifesto. O ícone precisa ser PNG e ter até 512 × 512 pixels. O DEX precisa ter até 16 MiB; o pacote completo, até 20 MiB. Categorias aceitas: `OTIMIZACAO`, `RENDERIZACAO`, `REDE`, `MONITORAMENTO`, `AUTOMACAO` e `EXPERIMENTAL`.

## Criar e empacotar um plugin

Use um módulo Android/Kotlin com o plugin Compose do mesmo Kotlin usado pelo host. Implemente `SvPlugin` e compile o módulo como APK de debug, usando a API e as dependências Compose como `compileOnly` para não duplicá-las dentro do plugin. O host fornece essas classes em runtime.

1. Prepare o `manifest.json` e um `icon.png`.
2. Compile o módulo:

   ```sh
   ./gradlew :plugin-example:assembleDebug
   ```

3. Empacote somente o DEX e os metadados como `.plgs`:

   ```sh
   python3 tools/package_plgs.py \
     --apk plugin-example/build/outputs/apk/debug/plugin-example-debug.apk \
     --manifest plugin-example/src/main/plugin/manifest.json \
     --icon plugin-example/src/main/plugin/icon.png \
     --output dist/plugin-example.plgs
   ```

4. Copie o `.plgs` para o celular e importe pelo app. Não instale o APK usado como etapa de compilação: ele é apenas uma fonte do `classes.dex`.

O repositório contém `:plugin-optimization`, `:plugin-monitoring` e `:plugin-diagnostics` como exemplos empacotados automaticamente em `.plgs`. Cada um ganha sua própria aba e ícone. O workflow publica os três pacotes de exemplo separadamente do APK host. Veja [`PLUGIN_DIAGNOSTICS.md`](PLUGIN_DIAGNOSTICS.md) para o exemplo de diagnóstico somente leitura.

## Permissões e segurança

Um `.plgs` executa código dentro do processo do SvBooster, com a identidade e os acessos do host. O formato e a validação impedem arquivos fora da estrutura esperada, mas **não são uma sandbox nem uma assinatura de confiança**. Um plugin pode usar a `HostApi` e, como código no mesmo processo, pode tentar operações adicionais. Importe somente plugins cujo código e origem você confia. Revise comandos de shell, prefira ações reversíveis e solicite confirmação antes de alterar configurações.
