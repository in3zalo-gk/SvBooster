# SvBooster — documentação da comunidade

SvBooster é um host Android de otimização de jogos com Kotlin, Jetpack Compose, Shizuku/Depuração sem fio e módulos de plugin. Esta documentação é a referência para quem usa, testa ou desenvolve módulos.

> **Estado desta versão:** o contrato `:plugin-api`, as abas por categoria e dois plugins de exemplo estão no projeto. Os exemplos são módulos Android Library integrados no build do host; ainda não são APKs externos instaláveis/carregados em runtime. O adaptador e o serviço de distribuição WXDynamicPlugin precisam ser configurados com servidor, metadados e tarefas de empacotamento próprios antes de habilitar download remoto. Consulte [integração do WXDynamicPlugin](WX_RUNTIME.md). Não instale plugins de origem desconhecida: plugins executam código no processo do host.

## Guias

- [Criar um primeiro plugin](PLUGIN_GUIDE.md)
- [Referência da HostApi](PLUGIN_API.md)
- [Categorias e roadmap](ROADMAP.md)
- [Estado da integração WXDynamicPlugin](WX_RUNTIME.md)

## Compilar localmente

1. Abra a raiz do projeto no Android Studio, com JDK 17 e Android SDK 35.
2. Sincronize o Gradle.
3. Execute `:app:assembleDebug` para gerar o host.
4. Execute `:plugin-api:build`, `:plugin-optimization:build` e `:plugin-monitoring:build` para validar os módulos e o contrato.
5. No GitHub Actions, o workflow `Build APK` compila o app e executa os testes instrumentados.

Os plugins de exemplo são bibliotecas compiladas dentro do host; portanto, este build não gera APKs instaláveis separados para eles.
