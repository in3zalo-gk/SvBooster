# Runtime de plugins: formato `.plgs` e status do WXDynamicPlugin

WXDynamicPlugin foi avaliado como alternativa de runtime, mas **não é dependência do carregador nesta implementação**. O host usa o formato próprio `.plgs` e `DexClassLoader` do Android para importar e carregar uma classe Compose que implemente a API `:plugin-api`.

## O que o host suporta agora

- Pacote com extensão `.plgs` e exatamente três itens: `manifest.json`, `classes.dex` e `icon.png`.
- Versões `formatVersion=1` e `apiVersion=1`, validação dos campos de identidade/categoria, limite de tamanho, validação básica do DEX e do PNG.
- Armazenamento privado no app e carregamento do DEX com o classloader do host como pai, para reutilizar a mesma identidade de `SvPlugin` e das bibliotecas Compose.
- Uma aba inferior por plugin, com nome e ícone do pacote; exemplos também passam pelo mesmo formato e carregador.
- Remoção do pacote importado na tela Ajustes. Como a VM mantém classes carregadas, é preciso reiniciar o app para descarregá-las totalmente.

O `.plgs` é um contêiner, não um APK instalável. Renomear APK/ZIP não basta: o host exige extensão e conteúdo interno válidos.

## Limites importantes

A validação estrutural **não é uma sandbox nem uma verificação de assinatura**. O código DEX executa no processo e com a identidade do SvBooster; um plugin malicioso pode tentar usar os acessos do host. Importe apenas plugins revisados e de origem confiável. Esta beta não possui loja, catálogo remoto, assinatura criptográfica, sandbox por processo, rollback automático nem isolamento de permissões.

Antes de qualquer distribuição pública, falta definir assinatura e confiança de origem, política de atualização/rollback, compatibilidade binária da API, auditoria de plugins e testes reais de instalação, remoção, atualização, exceções e regressão Compose. O suporte a `.plgs` é real, mas esses limites precisam ser comunicados.

## Trabalho futuro do WXDynamicPlugin

O projeto [WXDynamicPlugin](https://github.com/wgllss/WXDynamicPlugin) continua apenas uma possibilidade avaliada para substituir/adaptar o carregador. Não está integrado, e os pacotes `.plgs` atuais não devem ser descritos como “WX plugins”. Qualquer migração futura precisa fixar uma versão compatível e preservar o formato/documentação ou fornecer conversão explícita.
