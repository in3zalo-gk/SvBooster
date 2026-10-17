# Importar plugins no SvBooster Beta

## Importante: importação de APK ainda não foi liberada

Nesta beta não existe uma loja, botão para selecionar um APK, nem carregamento de APK externo instalado/downloadado. Os exemplos `:plugin-optimization` e `:plugin-monitoring` são módulos Android Library incorporados ao host durante a compilação. A aba dinâmica só reflete implementações já registradas no build. **Não tente instalar um APK de plugin separado esperando que ele apareça na navegação.**

A etapa de carregamento dinâmico depende do runtime WXDynamicPlugin, de uma estratégia de assinatura/verificação e de distribuição. O estado e os requisitos estão em [WX_RUNTIME.md](WX_RUNTIME.md).

## Para desenvolvedores: adicionar um módulo conhecido ao host

1. Obtenha o código-fonte do plugin de uma origem confiável e revise os comandos/permissões.
2. Copie o módulo para a raiz do projeto, por exemplo `plugin-example/`.
3. Inclua-o no `settings.gradle.kts`:

   ```kotlin
   include(":plugin-example")
   ```

4. No `plugin-example/build.gradle.kts`, use o plugin Android Library, Kotlin Android e Compose; dependa de `implementation(project(":plugin-api"))`.
5. Implemente `SvPlugin` e componha a UI pelo método `Content(context, hostApi)`.
6. Faça o host depender do módulo, em `app/build.gradle.kts`:

   ```kotlin
   implementation(project(":plugin-example"))
   ```

7. Registre a implementação no `PluginRegistry` em `app/src/main/java/com/turbo/gamebooster/plugins/PluginHost.kt`:

   ```kotlin
   register(ExamplePlugin)
   ```

8. Compile com `./gradlew :app:assembleDebug`, instale o APK host e confirme que a categoria do plugin aparece.

Com essa integração build-time, os módulos ficam dentro do APK host; não são plug-ins removíveis ou atualizáveis independentemente. Consulte [PLUGIN_GUIDE.md](PLUGIN_GUIDE.md) e [PLUGIN_API.md](PLUGIN_API.md).

## Como deverá funcionar a importação por WX (planejada)

Após integrar o runtime e publicar um catálogo assinado, o fluxo esperado é: abrir Loja de Plugins → conferir nome, versão, permissões/capacidades e assinatura → baixar o pacote do catálogo → validar assinatura e compatibilidade com o host → instalar no armazenamento privado → carregar pelo runtime WX → adicionar a categoria e o composable às abas. A remoção deve descarregar o módulo e reverter efeitos ativos.

Esse fluxo é **plano de implementação, não uma função disponível na beta**. Não importe APKs de terceiros nem execute módulos sem validação de integridade e origem. O host e os plugins têm capacidades de shell e overlay; `HostApi` não é sandbox de segurança.
