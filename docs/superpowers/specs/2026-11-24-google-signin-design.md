# Login/Cadastro com Google — Design

Data: 2026-11-24 (revisado 2026-11-24)

## Intenção

O README afirma "Google Sign-In (login via Gmail)", mas as telas não têm botão e
`AuthRepository.signInWithGoogle(idToken)` (declarado em `AuthRepository.kt:226`,
com guarda de demo na linha 227) está órfão.
Entregar o botão nas duas telas e ligar o fluxo real ao Firebase, mantendo-o visível
no modo demo apenas como informação de configuração.

## Objetivos

- Botão "Continuar com Google" em Login e Cadastro.
- Fluxo real: Credential Manager → ID token → `signInWithGoogle` → Firebase Auth.
- Demo ou configuração incompleta: botão visível, sem autenticação, mensagem clara.
- Preservar perfil existente; exigir cidade/bairro de usuários novos antes do mapa.
- Erro, cancelamento e carregamento tratados; nenhum token/credencial em log.
- README com os requisitos reais de configuração do Firebase.

## Não-objetivos / anti-objetivos

- Não configurar projeto Firebase real nem gerar `google-services.json` com chave.
- Não simular conta Google no modo demo.
- Não alterar o comportamento dos dados demo (continuam em memória, conforme README:162).
- Não registrar tokens, credenciais, e-mail ou resultado do seletor em log.
- Não sobrescrever documento `usuarios/{uid}` existente.
- **Não linkar contas automaticamente.** Vinculação futura será feature explícita
  nas configurações do perfil.

## Restrições constatadas

- `minSdk = 24`, `compileSdk = 34`, `targetSdk = 34`, JVM 17 (`app/build.gradle.kts:10-39`).
- Gradle wrapper 8.7, AGP 8.5.1 (`build.gradle.kts:2`), Kotlin plugin 2.1.0.
  O `agp = 9.4.1` em `gradle/libs.versions.toml` não é usado pelo build.
- Firebase BOM `33.1.2`; `firebase-auth-ktx` sem versão explícita.
- `app/google-services.example.json` tem `oauth_client: []` e
  `other_platform_oauth_client: []`; o JSON local é placeholder com `DUMMY`.
- O plugin **não gera** `R.string.default_web_client_id` com esse JSON
  (verificado em `app/build/generated`).
- Modo demo detectado em `MainActivity.kt:77-78`; bloqueia `signInWithGoogle` em
  `AuthRepository.kt:227`.
- `PerfilScreen` já edita cidade/bairro (`PerfilScreen.kt:180-181`);
  `atualizarPerfil()` persiste esses campos e revalida username (`AuthRepository.kt:257`);
  `getDadosUsuario(uid)` já existe (`AuthRepository.kt:363`).
- `ReportarScreen.kt:92-93` usa cidade/bairro do perfil para preencher o formulário;
  são campos editáveis pelo usuário, mas necessários para a UX.

## Abordagens

| Abordagem | Trade-off | Status |
|---|---|---|
| A — Credential Manager (`androidx.credentials` + `googleid`) | API atual, recomendada pelo Firebase | **Escolhida** |
| B — `GoogleSignInClient` (`play-services-auth`) | Zero deps novas, mas API legada | Rejeitada |
| C — Botão decorativo | Não entrega suporte; README continuaria incorreto | Rejeitada |

## Decisões de implementação

1. **Dependências (versões estáveis verificadas)**
   - `androidx.credentials:credentials:1.6.0` — estável (abr/2026), `minSdk 23`
     (<= 24 do app). Declara transitivamente `core 1.15.0`, `kotlin-stdlib 2.1.20`
     e `coroutines 1.9.0`; a compatibilidade efetiva com o plugin Kotlin 2.1.0 e
     o Firebase BOM 33.1.2 será confirmada pela resolução do Gradle e pelo build,
     não apenas pela versão no POM.
   - `androidx.credentials:credentials-play-services-auth:1.6.0` — mesmo `minSdk 23`.
   - `com.google.android.libraries.identity.googleid:googleid:1.2.1` — estável,
     `minSdk 24` (exatamente o do app) e declara `credentials:1.6.0`, formando par coerente.
   - Firebase BOM 33.1.2 não fixa essas libs; documentação do Firebase cita 1.3.0/1.1.1,
     mas são versões mais antigas, não requisito. Não há necessidade de alterar
     Gradle/AGP/Kotlin/compileSdk. Se o build provar incompatibilidade real com
     `compileSdk 34`, usar `credentials:1.5.0` + `googleid:1.1.1` **com justificativa
     registrada no commit** — nunca fixar versão antiga sem motivo verificado.
2. **Remoção de `play-services-auth:21.2.0`**: busca em todo o projeto (kt/kts/xml,
   exceto `build/`) encontrou apenas a própria declaração em `app/build.gradle.kts:132`.
   Nenhum código usa `GoogleSignIn`/`GoogleSignInClient`/`SignInIntent`. Remoção segura;
   o build é a confirmação final.
3. **Validação do Web Client ID** (`googleWebClientIdValido()`): ler via
   `resources.getIdentifier("default_web_client_id", "string", packageName)`
   (não `R.string...`, que quebraria com o JSON `DUMMY`). Rejeitar: ausência,
   `isBlank()`, `contém DUMMY/PLACEHOLDER/YOUR_/example`, prefixo `000000000000`,
   e formato diferente de `<dígitos>-<id>.apps.googleusercontent.com`.
   `googleDisponivel = !demo && webClientIdValido`.
4. **Comportamento do botão**
   - Demo ou client id inválido → mensagem "Login com Google exige Firebase
     configurado (google-services.json com OAuth Web)." e nenhuma chamada ao Credential Manager.
   - Válido → `GetGoogleIdOption(setServerClientId(id), filterByAuthorizedAccounts = true)`.
     Em `NoCredentialException`, repetir uma vez com `false` (padrão oficial).
   - Cancelamento (`GetCredentialCancellationException`) encerra sem erro;
     outras exceções mostram texto amigável e restauram o estado.
5. **Token**: `IdTokenCredential.createFrom(credential.data).idToken`, usado apenas
   em memória em `signInWithGoogle`. Zero `Log`/`println` com token, credencial ou e-mail.
6. **Conflito de contas (comportamento real verificado)**: com "One account per email
   address" ativo (padrão recomendado), `signInWithCredential` **não linka**; lança
   `FirebaseAuthUserCollisionException` com
   `ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL`, e `getUpdatedCredential()` é
   nulo para credencial Google. O código nunca chama `linkWithCredential`. Mapear
   esse código para mensagem específica: "Este e-mail já possui conta com senha.
   Entre com sua senha para continuar." `formatAuthException` precisa dessa regra
   **antes** do branch genérico "already in use". Linkagem ficará para feature
   futura em Configurações do Perfil.
7. **Fonte de verdade do onboarding = Firestore**
   - `signInWithGoogle` continua consultando `usuarios/{uid}` e só cria documento
     se não existir (sempre preservando perfil existente).
   - Retorna `Result<GoogleSignInResult>(user, precisaCompletarPerfil)`, onde a flag
     vem de `getDadosUsuario(uid)` → `cidade.isBlank() || bairro.isBlank()`.
   - Leitura falha/null → **falha fechada**: `precisaCompletarPerfil = true`.
   - `SharedPreferences` é **auxiliar e por uid**: guarda apenas um "hint" de que
     o onboarding ficou pendente (ex.: leitura inconclusiva/salvamento falho).
     Nunca autoriza o mapa. O `startDestination` **não é decidido por `currentUid`
     sozinho** (como hoje em `MainActivity.kt:101-103`): com sessão ativa, aguarda
     a consulta ao Firestore. Perfil ausente/ilegível (`null` ou erro) mantém o
     onboarding obrigatório. O hint é sobrescrito/limpo conforme o estado real,
     nunca usado como autorização de acesso ao mapa.
8. **Username**: **alteração necessária** — hoje `AuthRepository.kt:165`, `:188` e
   `:239` usam `while (isUsernameDisponivel(username) != true)`, que itera
   indefinidamente quando a consulta retorna `null`. Um único helper passa a
   limitar tentativas e usar sufixo de tempo; esgotando, falha com erro. Na
   conclusão do onboarding, criar
   `atualizarLocalizacaoPerfil(cidade, bairro)`, que grava **somente** esses dois
   campos via `update()` e não revalida username — evita prender o usuário por
   colisão de username gerado.
9. **Onboarding obrigatório**: enquanto `completarPerfil` estiver ativo,
   `PerfilScreen` entra em edição, exige cidade/bairro não vazios, esconde bottom
   nav e intercepta back. Falha ao salvar mantém o usuário na tela com erro.
   `Sair` continua disponível: encerra a sessão, remove o hint do uid anterior e
   navega para `welcome`; back fica bloqueado nesta etapa. Reinício do app recalcula
   a flag a partir do Firestore antes de escolher `startDestination`. Troca de conta
   recalcula para o novo uid.
10. **Sessão/logout**: Firebase Auth já persiste `currentUser`. Logout =
    `auth.signOut()` + `ClearCredentialStateRequest` no helper do Credential
    Manager, aguardado mas best-effort (falha não impede navegação). A limpeza
    fica no helper (que possui o `Context`), não no `AuthRepository`. Sessão demo
    permanece em memória, conforme README.

## Arquivos afetados

**Modificados**
- `app/build.gradle.kts` — dependências (3 adições, 1 remoção).
- `MainActivity.kt` — `modoDemo` no cadastro, estado `completarPerfil` recalculado
  do Firestore, callbacks de sucesso/logout Google.
- `AuthRepository.kt` — `signInWithGoogle` retornando `GoogleSignInResult`,
  helper de `SharedPreferences` por uid, `precisaCompletarPerfil(uid)`,
  `atualizarLocalizacaoPerfil`, regra de conflito e limite de tentativas do
  username (helper único aplicado aos 3 loops).
- `LoginScreen.kt` — botão (já recebe `modoDemo`) + callback `onGoogleSucesso`.
- `CadastroScreen.kt` — botão + novo parâmetro `modoDemo` e callback
  `onGoogleSucesso`.
- `PerfilScreen.kt` — modo completar perfil.
- `README.md` — config Firebase real, comportamento demo, checklist manual.

**Criados**
- `data/GoogleSignInModels.kt` — `GoogleSignInResult`, `GoogleCredentialResult`,
  `perfilIncompleto(Usuario?)` (nome distinto do método por uid do repositório).
- `data/GoogleSignInRules.kt` — funções puras `webClientIdValido(String?)` e
  `mensagemConflitoConta(Exception)`.
- `data/GoogleSignInHelper.kt` — leitura do web client id via `getIdentifier`,
  `GetCredentialRequest`, cancelamento/retry, `clearCredentialState()`.
- `ui/components/GoogleSignInButton.kt`
- `test/.../GoogleSignInRulesTest.kt` (unitário)
- `androidTest/.../GoogleButtonDemoTest.kt` (instrumentado)

**Não alterados**: `google-services.example.json`, `BackendSelector.kt`,
`firestore.rules`, `storage.rules`, screenshots e artefatos já presentes no
working tree (`build/reports/...`, `docs/superpowers/plans/...`, `scripts/__pycache__/`).

## Regressões e riscos

- **Build com JSON dummy**: mitigado por `getIdentifier` + validação; sem isso,
  `R.string.default_web_client_id` não existe e o build falha.
- **Fluxo real não verificável localmente**: sem JSON real, SHA-1 e provider ativo,
  o seletor não abre; validação fica unitária/instrumentada + checklist manual.
- **Perfil existente sobrescrito**: mitigado por `doc.exists()` e `update()` local.
- **Travamento no onboarding**: username gerado não é revalidado na conclusão.
- **Burlar onboarding por reinício/back/troca de conta**: Firestore é reconsultado;
  hint por uid nunca autoriza mapa.
- **`LoginFlowTest.kt:46-52` está desatualizado** (espera rótulos inexistentes);
  problema pré-existente, fora de escopo — será reportado, não corrigido aqui.
- **Dependências transitivas novas** (`core 1.15.0`, `kotlin-stdlib 2.1.20`,
  `coroutines 1.9.0`): monitorar no build; se houver conflito real, aplicar fallback
  justificado.

## Testes (separação explícita)

- **Unitários (JVM, sem Android/Firebase)**: validação do client id, regra de conflito,
  `precisaCompletarPerfil`, geração limitada de username, mapeamento de erro.
- **Instrumentados (AVD, modo demo)**: botão presente nas duas telas, toque mostra
  aviso sem abrir seletor, loading/cancelamento e logout **demo** (navegação para
  `welcome`). Não cobrem limpeza do Credential Manager — isso depende de Firebase
  real e fica na validação manual (ou teste unitário isolado do helper, se viável).
- **Validação manual (depende de Firebase real)**: instalar JSON com OAuth Web,
  registrar SHA-1 debug/release, ativar provider Google, exercitar seletor de conta,
  conflito de e-mail, onboarding e logout. Checklist no README.

## Plano

O plano detalhado com TDD, critérios de aceitação e commits isolados está em
`docs/superpowers/plans/2026-11-24-google-signin.md`.

## O que fica para o executor

- SHA-1 e Web client id reais devem ser fornecidos pelo dono do projeto Firebase.
- Nenhum merge automático; PR com `Closes #N` e aprovação explícita.
