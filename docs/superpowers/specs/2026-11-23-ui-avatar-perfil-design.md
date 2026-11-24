# Design — UI em nível de referência + foto de perfil do vizinho

Data: 2026-11-23
Issue: a abrir (enhancement)
Branch: a criar a partir de `main` (`b7bd616`)

## Contexto

O usuário trouxe 7 mockups (UX Pilot) como alvo visual: paleta azul-marinho + laranja, botões pill, chips/badges arredondados, cards com sombra suave, bottom nav de 4 itens e avatar circular. O app hoje usa `MaterialTheme` sem customização, perfil com ícone genérico e `Usuario` sem campo de foto.

Decisões do usuário:
1. Abordagem **B — Progressiva**: tema + componentes + avatar nas telas principais; mapa mantém a estrutura atual (pins/bottom sheet ficam para fatia posterior).
2. Foto por **galeria + câmera**.
3. Avatar aparece em **perfil, detalhe da ocorrência, lista de ocorrências e painel admin**.

## Objetivo

Chegar ao "nível" das referências nas telas de entrada, perfil, lista e detalhe, com foto de perfil real — sem quebrar o fluxo demo já validado (autoria, admin, reposicionamento no mapa) e sem afetar o guard de release.

## Fora de escopo

- Carrossel de fotos com indicador `1/4`, comentários, confirmações, conquistas, reputação, exclusão de conta, preferências de alerta, slider de raio (telas 5–7 das referências).
- Pins personalizados, círculos de destaque e bottom sheet deslizante no MapLibre.
- "Continuar com Google" e "Esqueci senha" na UI: virariam affordances mortas nesta fatia (backend já existe, mas não faz parte deste trabalho).
- Troca do provedor de tiles. A marca d'água das referências é artefato do mockup.

## Sistema visual

Tokens em `ui/theme/Theme.kt` (novo arquivo), aplicados pelo `AppTheme` em `MainActivity`:

| Token | Valor | Uso |
|---|---|---|
| `primary` | `#FF7A1A` | botões, FAB, links, abas ativas |
| `onPrimary` | `#FFFFFF` | texto sobre laranja |
| `secondary` | `#12203A` | header/hero azul-marinho |
| `onSecondary` | `#FFFFFF` | texto sobre marinho |
| `surface` / `background` | `#FFFFFF` / `#F6F7F9` | fundo de cards/telas |
| `error` | `#C62828` | urgência, exclusão |
| `tertiary` | `#2E7D32` | resolvido/verificado |
| `shape` | `extraLarge = 28.dp`, botão `pill` | cantos grandes |
| tipografia | `headline*` bold, corpo `bodyMedium` | hierarquia das referências |

Componentes reutilizáveis em `ui/components/` (um arquivo curto, sem biblioteca nova):
- `PrimaryButton` (laranja, pill, largura configurável).
- `StatusBadge` (`ABERTO`/`RESOLVIDO`/`URGENTE`) — reutiliza as cores já usadas em `OcorrenciasListScreen` e `DetalhesScreen`.
- `Avatar` — círculo com `AsyncImage` (Coil já é dependência), placeholder com inicial do nome, tamanhos `32/40/96.dp`, opcional badge de câmera.
- `FilterChip` — só se o filtro do mapa entrar nesta fatia (ver Tasks).

## Foto de perfil

### Modelo e API

- `Usuario` ganha `val fotoUrl: String = ""` (compatível com `doc.toObject(Usuario::class.java)`; documentos antigos sem campo ficam com `""`).
- `AuthRepository.salvarFotoPerfil(uri: Uri, context: Context): Result<String>`:
  - **Demo**: copia os bytes para `filesDir/perfil/<uid>.jpg`, grava o caminho em `SharedPreferences` (`foto_perfil_<uid>`) e atualiza `DemoData.usuarios`. `getDadosUsuario`/`listarUsuarios` mesclam o caminho persistido, então a foto sobrevive a logout/restart do app. `DemoData.reset()` não apaga o arquivo.
  - **Produção**: upload para `perfil/<uid>.jpg` no Firebase Storage, `user.updateProfile { photoUri = ... }`, `usuarios/<uid>.fotoUrl = url`.
- Validação antes do upload: `contentType` imagem e `<= 2 MB` (as referências pedem JPG/PNG até 2 MB; `storage.rules` hoje limita a 5 MB em `/fotos`, então a regra de `/perfil` será mais restritiva).
- `storage.rules`: novo match `/perfil/{uid}/{file}` com `allow read: request.auth != null`, `allow write: request.auth.uid == uid`, `size < 2MB`, `contentType` `image/jpeg|image/png`. Mutação permanece restrita ao dono.
- `AndroidManifest.xml` + `res/xml/file_paths.xml`: `FileProvider` para o `TakePicture` gravar no cache do app. Permissão `CAMERA` já é solicitada em `MainActivity`.

### Captação

`PerfilScreen` usa dois `ActivityResultLauncher`: `GetContent` (`image/*`) e `TakePicture` (Uri do FileProvider). Ao selecionar/tirar: valida → mostra progresso → chama `salvarFotoPerfil` → recarrega `usuario` → exibe mensagem pt-BR em caso de falha. "Cancelar" mantém foto anterior. Câmera indisponível no AVD: teste de galeria no AVD, teste de câmera por código/review.

### Exibição

| Tela | Avatar | Fonte |
|---|---|---|
| `PerfilScreen` | grande (96 dp), badge de câmera, sobreposto ao card | `usuario.fotoUrl` |
| `DetalhesScreen` | pequeno (32 dp) ao lado de `usuarioNome` | `authRepository.getDadosUsuario(o.userId)?.fotoUrl` |
| `OcorrenciasListScreen` | pequeno (32 dp) no card | mesma busca, cache local por `userId` na composable |
| `AdminUsuariosTab` | médio (40 dp) na linha | `usuario.fotoUrl` de `listarUsuarios()` |

Nenhum avatar novo é gravado na ocorrência; `userId` continua sendo a fonte da autoria (regra já validada no branch anterior). Busca por autor é local à tela; se o autor não existir ou falhar, `Avatar` mostra a inicial — nunca quebra a lista.

## Aplicação por tela

- **Welcome/Login**: hero marinho com título + card de alerta decorativo, bottom sheet branco com formulário; campo de senha com toggle de visibilidade; botão primário pill. Sem Google/Esqueci.
- **Cadastro**: app bar com logo central, campos em card, botão laranja; mantém validações atuais (8+, maiúscula, número, especial).
- **Mapa**: `TopAppBar` com busca por texto (tipo/bairro/descrição) e chips de filtro funcionais mapeados para os tipos existentes (`Todas`, `Segurança` → Suspeita/Briga/Vandalismo/Animal perigoso, `Infra` → Iluminação danificada/Outra, `Suspeita` → Suspeita); FAB "Reportar" permanece. Lista filtrada alimenta o mapa e a tela de lista via estado compartilhado no grafo (simples: estado hoisted em `VizinhoAlertaApp`).
- **Lista**: cards com ícone de categoria, badge de status, avatar do autor, bairro/cidade e descrição.
- **Detalhes**: cabeçalho marinho com título e badges, avatar do autor e ações condicionais existentes intactas; modo de edição de localização continua igual.
- **Perfil**: avatar editável, botão "Editar perfil" pill, dados, "Painel Admin" (só admin) e "Sair".
- **Bottom nav**: 4 itens `Mapa`, `Alertas` (lista), `Nova` (reportar), `Perfil`; rotas atuais reaproveitadas, sem novo grafo. `Sair` continua no perfil. Itens ativos em laranja.

## Tasks previstas

1. Tema + componentes (`Theme.kt`, `components/`) e `BottomNav` com integração no `NavHost`.
2. Foto de perfil: modelo, `salvarFotoPerfil`, FileProvider, `storage.rules`, validação e testes de repositório (demo: caminho persistido; falha: >2 MB; sem uid: failure).
3. Aplicar tema/avatar em Welcome, Cadastro, Perfil e Admin.
4. Aplicar tema/avatar em Mapa (busca + filtros), Lista e Detalhes, preservando ações de autoria.
5. Verificação final: suíte, debug build, release guard, AVD (perfil troca foto pela galeria; autor/admin veem avatar; não-autor continua sem ações) e PR com Issue.

## Riscos e mitigações

- **Quebrar autoria/localização**: Tasks 3–4 só trocam apresentação; não tocar em `exigirPermissaoDemo`, `atualizarLocalizacao` nem no guard de release.
- **Foto ∆ custo no Firestore**: busca de autor por tela, com cache local; se crescer, mover para campo denormalizado na ocorrência (YAGNI agora).
- **Permissão de câmera negada**: fallback para galeria com mensagem clara.
- **DUMMY no release**: nenhuma mudança em `build.gradle.kts`; `checkReleaseGoogleServices` continua falhando com DUMMY.

## Verificação

- `./gradlew testDebugUnitTest assembleDebug --no-daemon`.
- `./gradlew compileReleaseKotlin --no-daemon` → deve falhar no `checkReleaseGoogleServices` com DUMMY.
- AVD: login demo → Perfil → trocar foto pela galeria → foto aparece no perfil; abrir ocorrência própria e alheia → avatar do autor correto; admin → lista de usuários com avatar; usuário comum continua sem ações em ocorrência alheia; reposicionamento no mapa continua persistindo.
- `git diff --check` limpo; PR sem merge até aprovação explícita.
