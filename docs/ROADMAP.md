# Categorias, recomendações e roadmap

Escolha a categoria pelo objetivo principal do plugin; isso mantém abas compreensíveis, facilita descoberta e evita duplicação. Cada categoria só aparece se houver pelo menos um plugin registrado.

| Categoria | Ideias recomendadas | Estado / notas |
|---|---|---|
| Otimização | Downscale por jogo (`cmd game set`), limite de FPS, perfis Seguro/Equilibrado/Performance, limpeza de memória | Existe plugin de exemplo de downscale. Comandos variam por versão Android/OEM. `trim-memory` não garante liberação de RAM e pode piorar recarga de apps. |
| Renderização | Seleção de driver OpenGL ES/Vulkan/ANGLE, DPI, otimização AOT | Troca de driver exige suporte do sistema e geralmente reinício do app/jogo. `dex2oat` manual não é um acelerador genérico; não execute sem validar o fluxo oficial e o impacto em armazenamento/energia. |
| Rede | Modo Wi-Fi de baixa latência quando suportado, latência/jitter/perda | Modo de baixa latência é condicionado ao hardware e às APIs disponíveis; não prometa menor ping em toda rede. |
| Monitoramento | HUD com FPS/frame time, CPU, RAM, temperatura e diagnóstico térmico | Existe plugin de exemplo com RAM, temperatura da bateria e HUD customizado. FPS/frame time e temperatura da CPU podem não estar disponíveis sem APIs/OEM privilegiados. |
| Automação | Perfis por jogo/pacote, modo OEM, agendamento antes do jogo | Use eventos de ciclo de vida para reverter mudanças. Agendamentos devem ser canceláveis e visíveis. |
| Experimental | Integrações root, Magisk/KernelSU, scripts e tweaks comunitários | Marque riscos, compatibilidade e reversão. Não execute em aparelhos sem root nem esconda alterações do usuário. |

## Regras para todos os plugins

1. **Idempotente:** aplicar duas vezes não acumula efeitos nem quebra configuração.
2. **Reversível:** guarde o estado anterior e restaure-o ao sair do jogo ou desligar o recurso.
3. **Seguro:** use allowlists de comandos/parâmetros, tempos limite e validação; nunca use `rm -rf`, escrita ampla em `/system` ou comandos destrutivos.
4. **Transparente:** explique o que será alterado, o que depende de OEM/root e como desfazer.
5. **Eficiente:** telemetria em intervalos moderados, coroutines canceláveis e nenhuma leitura intensiva em loop curto.
6. **Compatível:** teste em API mínima 26 e versões atuais, incluindo aparelho de entrada; forneça degradação graciosa.
7. **Privacidade:** não envie identificadores, perfis nem telemetria para servidores sem consentimento explícito.
