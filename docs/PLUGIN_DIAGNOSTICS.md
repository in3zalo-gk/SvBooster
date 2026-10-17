# Plugin de diagnóstico Shizuku

O módulo `:plugin-diagnostics` é um exemplo empacotado como `diagnostics.plgs`. Ele carrega pelo mesmo `DexClassLoader` e pelo mesmo fluxo de validação que os arquivos `.plgs` importados; após carregar, aparece como uma aba própria, com ícone de diagnóstico.

Na aba **Diagnóstico Shizuku**, toque em **Executar diagnóstico** para testar, um por vez:

| Verificação | Comando | O que mostra |
|---|---|---|
| Identidade shell | `id -un` | Identidade usada pelo processo privilegiado (normalmente `shell`) |
| Versão Android | `getprop ro.build.version.release` | Versão pública do Android |
| Build | `getprop ro.build.display.id` | Identificação de build fornecida pelo sistema |
| Resolução | `wm size` | Resolução física/forçada informada pelo Android |

Cada linha exibe seu próprio status (`OK`, `TIMEOUT`, erro de shell/permissão), tempo decorrido e saída/erro. O host limita cada chamada a 8 segundos; um comando lento pode expirar sem esconder os resultados dos demais. Se houver falha de acesso, confira **Ajustes → Shizuku**: app instalado, binder ativo e permissão concedida. O teste não solicita permissão automaticamente, não altera configurações nem escreve valores no sistema.

A execução acontece no processo do host por meio da `HostApi`; por isso, este exemplo testa tanto o carregamento dinâmico de `.plgs` quanto a ponte de comandos do host. A leitura de propriedades e da resolução pode variar por fabricante. Como todos os plugins, este código **não é isolado por sandbox** e deve ser tratado como código confiável, mesmo sendo somente leitura.
