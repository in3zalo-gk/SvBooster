# Criar um plugin `.plgs` para o SvBooster

O repositório inclui exemplos de otimização, monitoramento e diagnóstico. O diagnóstico Shizuku é um exemplo útil de execução pelo mesmo runtime dinâmico dos pacotes importados; consulte [`PLUGIN_DIAGNOSTICS.md`](PLUGIN_DIAGNOSTICS.md).

Plugins são carregados de arquivos `.plgs`. Cada um tem sua aba inferior e seu ícone PNG próprios. O formato desta versão contém `manifest.json`, `classes.dex` e `icon.png`; o plugin implementa o contrato versionado em `:plugin-api` e desenha a interface com Jetpack Compose.

## 1. Prepare um módulo de plugin

Use `com.android.application`, Kotlin Android e o plugin Compose configurado no projeto. O módulo é uma aplicação de build usada para gerar DEX; não será instalada no celular. Configure `minSdk 26`, `compileSdk 35` e Java 17.

Dependências da API do host e do Compose devem ser `compileOnly`, não `implementation`, para não empacotar cópias incompatíveis no DEX do plugin:

```kotlin
dependencies {
    compileOnly(project(":plugin-api"))
    compileOnly(platform("androidx.compose:compose-bom:2024.12.01"))
    compileOnly("androidx.compose.material3:material3")
    compileOnly("androidx.compose.foundation:foundation")
    compileOnly("androidx.compose.ui:ui")
}
```

## 2. Implemente a classe de entrada

```kotlin
object GpuPlugin : SvPlugin {
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
                    val result = hostApi.executeShell("cmd game list")
                    status = if (result.success) result.stdout else result.stderr
                }
            }) { Text("Consultar jogos") }
            Text(status)
        }
    }
}
```

A classe pode ser um `object` Kotlin ou ter construtor público sem parâmetros. Mantenha os quatro campos (`id`, `name`, `version`, `category`) iguais aos do manifesto. Evite text input livre em comandos shell; valide qualquer parâmetro e mostre o resultado ao usuário.

## 3. Crie o manifesto e o ícone

Salve `manifest.json` e `icon.png` em `plugin-example/src/main/plugin/`. Use os mesmos valores declarados pela classe:

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

O PNG pode ter até 512 × 512 pixels. Categorias: `OTIMIZACAO`, `RENDERIZACAO`, `REDE`, `MONITORAMENTO`, `AUTOMACAO`, `EXPERIMENTAL`.

## 4. Compile e empacote

```sh
./gradlew :plugin-example:assembleDebug
python3 tools/package_plgs.py \
  --apk plugin-example/build/outputs/apk/debug/plugin-example-debug.apk \
  --manifest plugin-example/src/main/plugin/manifest.json \
  --icon plugin-example/src/main/plugin/icon.png \
  --output dist/gpu-experimental.plgs
```

O empacotador inclui somente `classes.dex`, `manifest.json` e `icon.png`. Compartilhe o arquivo `.plgs`; o APK de debug é apenas um insumo do build e não deve ser instalado pelo usuário.

## Segurança e compatibilidade

O carregamento dinâmico **não é um sandbox**: o DEX executa dentro do processo do host e pode usar a mesma identidade/acessos do app. A extensão `.plgs` e as validações estruturais não comprovam autoria nem assinatura. Distribua apenas plugins revisados e de origem confiável; não execute código ou shell recebido de uma URL sem validação e consentimento.

Peça confirmação para ajustes persistentes, prefira operações idempotentes e reversíveis, e restaure as alterações quando o jogo terminar. `HostApi` é uma ponte de integração, não uma fronteira de segurança contra código malicioso.
