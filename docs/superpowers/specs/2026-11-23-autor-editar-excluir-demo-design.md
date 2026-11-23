# Design: autoria e localização editável no demo (Issue #1)

## Intent
Fazer o modo demo (debug sem Firebase) respeitar a mesma regra de autoria já imposta em produção por `firestore.rules`: apenas o autor (`userId`) ou um admin pode alterar uma ocorrência. Além de situação/status e exclusão, o titular deve poder **reposicionar a ocorrência no mapa** — hoje `lat`/`lng` são imutáveis.

Motivo: no teste manual, uma conta comum (`testador@demo.com`) conseguiu marcar como RESOLVIDO uma ocorrência pertencente a `demo-admin`. O demo deve servir como ensaio fiel do comportamento de produção. A correção de localização é necessária quando o reporte original marca o ponto errado.

## Goals
- Mutações em demo (situação/status, resolver, excluir, localização) permitidas somente para autor ou admin.
- Usuário comum não vê ações de mutação em ocorrência alheia.
- Autor e admin veem "MARCAR COMO RESOLVIDO", "EXCLUIR" e "ALTERAR LOCALIZAÇÃO" em `DetalhesScreen`.
- No modo de edição, tocar no mapa move o marcador; "SALVAR LOCALIZAÇÃO" persiste `lat`/`lng`.
- Cobertura unitária para autor, não-autor, admin e fail-closed.

## Non-goals / anti-goals
- Não alterar o fluxo de criação (`ReportarScreen`) nem permitir trocar o `userId` da ocorrência.
- Não mudar autenticação, cadastro, perfil ou navegação fora de `detalhes/{ocorrenciaId}`.
- Não habilitar demo em release; o guard de build permanece.
- Não criar abstração nova para uma regra de duas condições.

## Constraints
- `OcorrenciaRepository` é instanciado em `MainActivity` e em `OcorrenciaRepositoryDemoTest` (que hoje não faz login).
- `AuthRepository.currentUid` já expõe o uid demo e `isAdmin()` (suspend); `DemoData.usuarios` permite consulta síncrona do papel.
- As 5 fixtures de `DemoData` pertencem a `demo-admin`; a conta de teste comum é `demo-user-N`.
- `DetalhesScreen` já contém um `MapView` MapLibre 13.7.1; a API confirmada é `MapLibreMap.addOnMapClickListener(MapLibreMap.OnMapClickListener { point -> ... })`.
- `firestore.rules:47-55` hoje restringe atualização a `situacao`/`status`; mudar `lat`/`lng` exigiria ajuste das regras.

## Approaches
- **A: botão "Usar minha localização atual".** Menor diff, reusa `LocalizacaoService`. Limitação: só corrige para onde a pessoa está agora.
- **B (escolhida): mapa interativo.** Botão "ALTERAR LOCALIZAÇÃO" habilita edição; toque no mapa move o marcador; "SALVAR LOCALIZAÇÃO" confirma. Corrige para qualquer ponto e aproveita o `MapView` já presente. Custo: estado extra e um callback de clique.
- **C: campos numéricos de latitude/longitude.** Fácil de testar, mas UX ruim e aceita coordenada inválida sem perceber.

## Decisões de implementação
1. `OcorrenciaRepository(demo, firestore, storage, authRepository = null)`: quando `demo == true`, toda mutação passa por `exigirPermissaoDemo(id)`. Em produção, segue delegando ao Firestore.
2. `exigirPermissaoDemo(id)` localiza a ocorrência em `DemoData` (falha com "Ocorrência não encontrada" se ausente), obtém `uid = authRepository?.currentUid`, calcula `admin` via `DemoData.usuarios` e nega com "Você não tem permissão para alterar esta ocorrência." quando `uid == null || (o.userId != uid && !admin)`.
3. `resolver(id, userId)` mantém a assinatura por compatibilidade e usa o uid autenticado; o parâmetro não autoriza nada.
4. `atualizarLocalizacao(id, lat, lng)` valida a faixa (`-90..90`, `-180..180`) e usa o mesmo guard; em produção atualiza `lat`/`lng` no Firestore.
5. `firestore.rules`: autor e admin passam a poder alterar `lat`/`lng` junto de `situacao`/`status`, com `hasOnly(['situacao', 'status', 'lat', 'lng'])` e validação de tipo e faixa para as coordenadas.
6. `DetalhesScreen` recebe `authRepository`, calcula `podeGerenciar = o.userId == authRepository.currentUid || authRepository.isAdmin()` e mostra as ações somente quando autorizado.
7. Estado `editandoLocalizacao` e `posicaoSelecionada`: "ALTERAR LOCALIZAÇÃO" entra no modo; `addOnMapClickListener` atualiza a posição e redesenha o marcador; "SALVAR LOCALIZAÇÃO" chama o repositório e atualiza a UI; "CANCELAR" sai do modo sem salvar.
8. `deletarComMidia` em demo usa o mesmo guard de `deletar`.

## Verificação
- `./gradlew testDebugUnitTest --no-daemon` — inclui autor, não-autor, admin, fail-closed e faixa de coordenada.
- `./gradlew assembleDebug --no-daemon`.
- Teste manual no AVD: conta comum sem ações em ocorrência alheia; admin com ações; toque no mapa + salvar muda o marcador e a posição na lista/detalhes.
- Build de release com `google-services.json` DUMMY continua falhando.

## Left to the builder
Nada. Escopo fechado pelo que foi discutido; qualquer extensão (ex.: editar descrição) volta ao brainstorm.
