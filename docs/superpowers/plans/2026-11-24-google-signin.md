# Login/Cadastro com Google — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Botão "Continuar com Google" funcional em Login e Cadastro, com fluxo real via Credential Manager/Firebase e comportamento informativo no modo demo.

**Architecture:** Funções puras (`GoogleSignInModels`/`GoogleSignInRules`) decidem validação do Web Client ID, conflito de contas e necessidade de onboarding; um helper Android isolado fala com o Credential Manager; `AuthRepository` converte o ID token em sessão Firebase e preserva o perfil; `GoogleSignInButton` concentra a orquestração de clique/loading/erro; `MainActivity` usa o Firestore como fonte de verdade do destino inicial.

**Tech Stack:** Kotlin 2.1.0, Compose, Gradle 8.7/AGP 8.5.1, Firebase Auth BOM 33.1.2, AndroidX Credentials 1.6.0, googleid 1.2.1, JUnit4 + Compose Test.

**Spec:** `docs/superpowers/specs/2026-11-24-google-signin-design.md`

## Global Constraints

- `minSdk = 24`, `compileSdk = 34`, `targetSdk = 34`, JVM `17`; não alterar Gradle/AGP/Kotlin/compileSdk.
- Dependências exatas: `androidx.credentials:credentials:1.6.0`, `androidx.credentials:credentials-play-services-auth:1.6.0`, `com.google.android.libraries.identity.googleid:googleid:1.2.1`; remover `com.google.android.gms:play-services-auth:21.2.0` (sem consumidores).
- Se o build provar incompatibilidade real, usar `credentials:1.5.0` + `googleid:1.1.1` **com justificativa no commit**.
- Mensagem fixa de configuração: `"Login com Google exige Firebase configurado (google-services.json com OAuth Web)."`
- Conflito de e-mail: mensagem fixa `"Este e-mail já possui conta com senha. Entre com sua senha para continuar."`; **nunca** chamar `linkWithCredential`.
- Zero `Log`/`println`/` Timber` com ID token, credencial, e-mail ou resultado do seletor.
- Firestore é a fonte de verdade do onboarding; `SharedPreferences` é hint por uid e nunca autoriza o mapa.
- Não tocar em `build/reports/problems/problems-report.html`, `docs/superpowers/plans/2026-11-23-login-cadastro-design.md`, `scripts/__pycache__/`, screenshots, `google-services.example.json`, regras Firestore/Storage.
- Um commit por Task; PR com `Closes #N`; **sem merge sem aprovação explícita**.

## Review Focus

- **Web client id ausente/vazio/fictício (`DUMMY`, `YOUR_`, `example`, `000000000000`)** → aviso de configuração, nenhum seletor. Pinado em Task 1 (`webClientIdValido`) e Task 5 (botão demo).
- **E-mail Google já cadastrado com senha** → erro específico, sem linkagem automática. Pinado em Task 1 (`mensagemConflitoConta`) e Task 2 (`formatAuthException`).
- **Perfil existente com cidade/bairro preenchidos** → não sobrescrito; conclusão grava só cidade/bairro. Pinado em Task 1 (`perfilIncompleto`) e Task 4 (`atualizarLocalizacaoPerfil` preserva nome/username).
- **Firestore ilegível/`null` e username indisponível retornando `null`** → falha fechada no onboarding; sem loop infinito. Pinado em Task 1, Task 2 e Task 4.
- **Reinício/back/troca de conta durante onboarding** → destino recalculado do Firestore; hint nunca autoriza mapa. Pinado em Task 4 (teste com dois uids) e checklist manual em Task 8.

---

### Task 1: Modelos e regras puras

**Files:**
- Create: `app/src/main/java/com/vizinhoalerta/data/GoogleSignInModels.kt`
- Create: `app/src/main/java/com/vizinhoalerta/data/GoogleSignInRules.kt`
- Test: `app/src/test/java/com/vizinhoalerta/data/GoogleSignInRulesTest.kt`

**Interfaces:**
- Consumes: `com.vizinhoalerta.model.Usuario` (campos `cidade`, `bairro`).
- Produces: `GoogleSignInResult(uid: String, precisaCompletarPerfil: Boolean)`; `GoogleCredentialResult` (`Sucesso(idToken)`, `Cancelado`, `Erro(mensagem)`); `internal fun perfilIncompleto(usuario: Usuario?): Boolean`; `internal fun webClientIdValido(id: String?): Boolean`; `internal fun mensagemConflitoConta(errorCode: String?, message: String?): String?`; `internal const val MSG_GOOGLE_NAO_CONFIGURADO`.

- [ ] **Step 1: Escrever testes falhando em `GoogleSignInRulesTest.kt`**

```kotlin
@Test fun `web client id valido so aceita id real`() {
    assertTrue(webClientIdValido("123456789-abc.apps.googleusercontent.com"))
    assertFalse(webClientIdValido(null))
    assertFalse(webClientIdValido(""))
    assertFalse(webClientIdValido("   "))
    assertFalse(webClientIdValido("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"))
    assertFalse(webClientIdValido("123-DUMMY.apps.googleusercontent.com"))
    assertFalse(webClientIdValido("000000000000-abc.apps.googleusercontent.com"))
    assertFalse(webClientIdValido("123-abc.example.com"))
    assertFalse(webClientIdValido("123-abc.apps.googleusercontent.com.br"))
}

@Test fun `perfil nulo ou sem localizacao exige onboarding`() {
    assertTrue(perfilIncompleto(null))
    assertTrue(perfilIncompleto(Usuario(cidade = "", bairro = "Centro")))
    assertTrue(perfilIncompleto(Usuario(cidade = "Cássia", bairro = "  ")))
    assertFalse(perfilIncompleto(Usuario(cidade = "Cássia", bairro = "Centro")))
}

@Test fun `conflito de conta google tem mensagem especifica`() {
    assertEquals(
        "Este e-mail já possui conta com senha. Entre com sua senha para continuar.",
        mensagemConflitoConta("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", null)
    )
    assertEquals(
        "Este e-mail já possui conta com senha. Entre com sua senha para continuar.",
        mensagemConflitoConta(null, "account-exists-with-different-credential")
    )
    assertNull(mensagemConflitoConta("ERROR_EMAIL_ALREADY_IN_USE", null))
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `./gradlew testDebugUnitTest --tests '*GoogleSignInRulesTest' --no-daemon`
Expected: FAIL (unresolved references).

- [ ] **Step 3: Implementar os modelos e regras**

`webClientIdValido`: rejeita `null`/blank; rejeita ignoreCase contendo `DUMMY`, `PLACEHOLDER`, `YOUR_`, `example`; rejeita prefixo `000000000000`; exige regex exata `^\d+-[A-Za-z0-9._-]+\.apps\.googleusercontent\.com$`. `mensagemConflitoConta`: só retorna texto quando `errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL"` ou a mensagem contém `account-exists-with-different-credential`; senão `null`. `perfilIncompleto`: `null -> true`; cidade ou bairro em branco -> `true`. `GoogleCredentialResult` é `sealed interface` com `data object Cancelado`.

- [ ] **Step 4: Rodar e confirmar verde**

Run: `./gradlew testDebugUnitTest --tests '*GoogleSignInRulesTest' --no-daemon`
Expected: PASS (3 testes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/data/GoogleSignInModels.kt app/src/main/java/com/vizinhoalerta/data/GoogleSignInRules.kt app/src/test/java/com/vizinhoalerta/data/GoogleSignInRulesTest.kt
git commit -m "test: regras puras do login com Google"
```

### Task 2: Username com limite e mensagem de conflito

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt:18-36,145-190,226-255`
- Test: `app/src/test/java/com/vizinhoalerta/data/GoogleSignInRulesTest.kt`

**Interfaces:**
- Consumes: `mensagemConflitoConta` (Task 1), `AuthRepository.isUsernameDisponivel(username, currentUid)` existente.
- Produces: `internal suspend fun gerarUsernameUnico(base: String, disponivel: suspend (String) -> Boolean?): kotlin.Result<String>` em `AuthRepository.kt`.

- [ ] **Step 1: Adicionar testes falhando**

```kotlin
@Test fun `gerar username para quando consulta falha`() = runBlocking {
    val resultado = gerarUsernameUnico("@morador") { null }
    assertTrue(resultado.isFailure)
}

@Test fun `gerar username usa primeiro disponivel`() = runBlocking {
    val resultado = gerarUsernameUnico("@morador") { true }
    assertEquals("@morador", resultado.getOrNull())
}

@Test fun `formatAuthException prioriza conflito de conta`() {
    assertEquals(
        "Este e-mail já possui conta com senha. Entre com sua senha para continuar.",
        formatAuthException(FirebaseAuthUserCollisionException("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "conflito"))
    )
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `./gradlew testDebugUnitTest --tests '*GoogleSignInRulesTest' --no-daemon`
Expected: FAIL (unresolved reference / mensagem genérica).

- [ ] **Step 3: Implementar**

`gerarUsernameUnico`: até 10 tentativas com sufixo `100..999`, depois 2 com `System.currentTimeMillis()`; retorna `Result.success` no primeiro `true`, `Result.failure(Exception("Não foi possível gerar um username único. Tente novamente."))` se todas retornarem `false`/`null`. Substituir os loops em `cadastrar` (demo e produção) e `signInWithGoogle` por essa função, propagando falha. Em `formatAuthException`, inserir **antes** do branch `email-already-in-use`: `mensagemConflitoConta((e as? FirebaseAuthException)?.errorCode, message)?.let { return it }`.

- [ ] **Step 4: Rodar e confirmar verde**

Run: `./gradlew testDebugUnitTest --tests '*GoogleSignInRulesTest' --no-daemon`
Expected: PASS (6 testes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt app/src/test/java/com/vizinhoalerta/data/GoogleSignInRulesTest.kt
git commit -m "fix: limita geracao de username e mapeia conflito de conta"
```

### Task 3: Dependências e helper do Credential Manager

**Files:**
- Modify: `app/build.gradle.kts:131-132`
- Create: `app/src/main/java/com/vizinhoalerta/data/GoogleSignInHelper.kt`

**Interfaces:**
- Consumes: `webClientIdValido`, `MSG_GOOGLE_NAO_CONFIGURADO` (Task 1), `GoogleCredentialResult` (Task 1).
- Produces: `class GoogleSignInHelper(context: Context)` com `val disponivel: Boolean`, `suspend fun signIn(): GoogleCredentialResult`, `suspend fun clearCredentialState()`.

- [ ] **Step 1: Ajustar dependências**

Trocar a linha `// Google Sign-In` por três `implementation(...)` com as versões exatas de Global Constraints e remover `play-services-auth:21.2.0`.

- [ ] **Step 2: Implementar o helper**

`disponivel`: `context.resources.getIdentifier("default_web_client_id", "string", context.packageName)`; `0` → `null`; senão `context.getString(id)`; aplicar `webClientIdValido`. `signIn()`: se `!disponivel`, retornar `GoogleCredentialResult.Erro(MSG_GOOGLE_NAO_CONFIGURADO)`; montar `GetGoogleIdOption.Builder().setServerClientId(id).setFilterByAuthorizedAccounts(filter).build()` e `GetCredentialRequest`; `CredentialManager.create(context).getCredential(context, request)`; extrair `CustomCredential` com `type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL` via `GoogleIdTokenCredential.createFrom(credential.data).idToken`. Em `NoCredentialException`, repetir **uma vez** com `filter = false`; em `GetCredentialCancellationException`, retornar `Cancelado`; demais `GetCredentialException`/`Exception`, retornar `Erro` com texto amigável. `clearCredentialState()`: `try { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) } catch (e: Exception) { }` — best-effort, sem log. Nenhum `Log`/`println` no arquivo.

- [ ] **Step 3: Verificar build**

Run: `./gradlew assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL. Se falhar por incompatibilidade real de compileSdk/transitivas, aplicar fallback de Global Constraints e registrar motivo no commit.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts app/src/main/java/com/vizinhoalerta/data/GoogleSignInHelper.kt
git commit -m "feat: credencial manager para login com Google"
```

### Task 4: AuthRepository — resultado, hint e onboarding

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt:44-58,226-255,432-437`
- Test: `app/src/test/java/com/vizinhoalerta/data/AuthRepositoryDemoTest.kt`

**Interfaces:**
- Consumes: `GoogleSignInResult` e `perfilIncompleto(Usuario?)` (Task 1), `gerarUsernameUnico` (Task 2).
- Produces: `suspend fun signInWithGoogle(idToken: String): kotlin.Result<GoogleSignInResult>`; `suspend fun atualizarLocalizacaoPerfil(cidade: String, bairro: String): kotlin.Result<Unit>`; `suspend fun precisaCompletarPerfil(uid: String): Boolean`; `fun marcarOnboardingPendente(uid: String)`; `fun limparOnboardingPendente(uid: String)`.

- [ ] **Step 1: Adicionar testes falhando**

```kotlin
@Test fun `conclusao do onboarding altera apenas cidade e bairro`() = runBlocking {
    repo.login(DemoData.ADMIN_EMAIL, DemoData.SENHA_DEMO)
    val antes = repo.getDadosUsuario("demo-admin")!!
    assertTrue(repo.atualizarLocalizacaoPerfil("Passos", "Centro").isSuccess)
    val depois = repo.getDadosUsuario("demo-admin")!!
    assertEquals(antes.nome, depois.nome)
    assertEquals(antes.username, depois.username)
    assertEquals("Passos", depois.cidade)
    assertEquals("Centro", depois.bairro)
}

@Test fun `conclusao rejeita cidade ou bairro vazios`() = runBlocking {
    repo.login(DemoData.ADMIN_EMAIL, DemoData.SENHA_DEMO)
    assertTrue(repo.atualizarLocalizacaoPerfil("", "Centro").isFailure)
    assertTrue(repo.atualizarLocalizacaoPerfil("Cássia", " ").isFailure)
}

@Test fun `precisa completar perfil e recalculado por uid`() = runBlocking {
    repo.login(DemoData.ADMIN_EMAIL, DemoData.SENHA_DEMO)
    assertFalse(repo.precisaCompletarPerfil("demo-admin")) // perfil demo já tem cidade/bairro
    DemoData.usuarios.add(Usuario(id = "outro-uid", nome = "Outro", username = "@outro", email = "outro@demo.com"))
    assertTrue(repo.precisaCompletarPerfil("outro-uid")) // troca de conta
    assertTrue(repo.precisaCompletarPerfil("uid-inexistente")) // falha fechada
}
```

Importar `com.vizinhoalerta.model.Usuario` no teste.

- [ ] **Step 2: Rodar e confirmar falha**

Run: `./gradlew testDebugUnitTest --tests '*AuthRepositoryDemoTest' --no-daemon`
Expected: FAIL (métodos ausentes).

- [ ] **Step 3: Implementar**

Adicionar `onboardingPrefs = context?.getSharedPreferences("onboarding_google", Context.MODE_PRIVATE)` e helpers com chave `"pendente:$uid"` (no-op se `context == null`). `precisaCompletarPerfil(uid)`: `perfilIncompleto(getDadosUsuario(uid))` (Task 1); `null` → `true`; hint apenas marca estado pendente, nunca retorna `false`. `signInWithGoogle`: manter guarda demo; trocar retorno para `Result<GoogleSignInResult>`; após `signInWithCredential`, manter `doc.exists()` (nunca sobrescrever); usando `gerarUsernameUnico` para documento novo; calcular flag com `precisaCompletarPerfil(user.uid)` e retornar `GoogleSignInResult(user.uid, flag)`. `atualizarLocalizacaoPerfil`: validar não-blank; demo atualiza `DemoData` via `copy(cidade, bairro)`; produção usa `firestore.collection("usuarios").document(uid).update(mapOf("cidade" to cidade.trim(), "bairro" to bairro.trim()))` — **sem** revalidar username; ao sucesso, `limparOnboardingPendente(uid)`. `logout()` também limpa o hint do uid atual antes de `signOut()`/reset demo.

- [ ] **Step 4: Rodar e confirmar verde**

Run: `./gradlew testDebugUnitTest --tests '*AuthRepositoryDemoTest' --no-daemon`
Expected: PASS (incluindo os 3 novos testes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt app/src/test/java/com/vizinhoalerta/data/AuthRepositoryDemoTest.kt
git commit -m "feat: resultado do Google e onboarding baseado em Firestore"
```

### Task 5: Botão compartilhado, telas e teste demo

**Files:**
- Create: `app/src/main/java/com/vizinhoalerta/ui/components/GoogleSignInButton.kt`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/login/LoginScreen.kt:28-33,113`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/cadastro/CadastroScreen.kt:30-34,137`
- Test: `app/src/androidTest/java/com/vizinhoalerta/ui/GoogleButtonDemoTest.kt`

**Interfaces:**
- Consumes: `GoogleSignInHelper` (Task 3), `AuthRepository.signInWithGoogle` (Task 4), `MSG_GOOGLE_NAO_CONFIGURADO` (Task 1).
- Produces: `GoogleSignInButton(googleSignIn: GoogleSignInHelper, authRepository: AuthRepository, modoDemo: Boolean, onSucesso: (Boolean) -> Unit, onMensagem: (String) -> Unit, modifier: Modifier = Modifier)`; `LoginScreen(..., googleSignIn: GoogleSignInHelper, onGoogleSucesso: (Boolean) -> Unit)`; `CadastroScreen(..., modoDemo: Boolean = false, googleSignIn: GoogleSignInHelper, onGoogleSucesso: (Boolean) -> Unit)`.

- [ ] **Step 1: Escrever teste instrumentado falhando**

```kotlin
@Test fun botao_google_no_login_mostra_aviso_no_demo() {
    composeTestRule.onNodeWithText("Continuar com Google").assertIsDisplayed()
    composeTestRule.onNodeWithText("Continuar com Google").performClick()
    composeTestRule.onNodeWithText("Login com Google exige Firebase configurado (google-services.json com OAuth Web).").assertIsDisplayed()
}

@Test fun botao_google_no_cadastro_mostra_aviso_no_demo() {
    composeTestRule.onNodeWithText("Não tem conta? Cadastrar").performClick()
    composeTestRule.onNodeWithText("Continuar com Google").assertIsDisplayed()
    composeTestRule.onNodeWithText("Continuar com Google").performClick()
    composeTestRule.onNodeWithText("Login com Google exige Firebase configurado (google-services.json com OAuth Web).").assertIsDisplayed()
}
```

Usar `createAndroidComposeRule<MainActivity>()`; o AVD está em modo demo (chave `DUMMY`), então o seletor nunca abre.

- [ ] **Step 2: Rodar e confirmar falha**

Run: `./gradlew connectedAndroidTest --tests '*GoogleButtonDemoTest' --no-daemon`
Expected: FAIL (botão/aviso ausente). Se nenhum AVD estiver disponível, registrar a limitação — não inventar sucesso.

- [ ] **Step 3: Implementar o composable**

`OutlinedButton` com texto exato `"Continuar com Google"`; quando `carregando`, `CircularProgressIndicator(Modifier.size(20.dp))`. No clique: se `modoDemo || !googleSignIn.disponivel`, chamar `onMensagem(MSG_GOOGLE_NAO_CONFIGURADO)` e retornar; senão `scope.launch` com `when (googleSignIn.signIn())`: `Sucesso` → `authRepository.signInWithGoogle(idToken)` e `onSucesso(result.precisaCompletarPerfil)` no sucesso / `onMensagem` na falha; `Cancelado` → nada; `Erro` → `onMensagem`. `carregando` controla `enabled` e é sempre restaurado no `finally`. Sem log.

- [ ] **Step 4: Ligar nas telas**

`LoginScreen`: adicionar parâmetros e posicionar o botão depois do bloco `if (modoDemo)`; `onMensagem = { mensagemErro = it }`; `onSucesso = onGoogleSucesso`. `CadastroScreen`: adicionar `modoDemo`, `googleSignIn`, `onGoogleSucesso` e posicionar o botão antes do `TextButton` de login; mesma ligação de mensagem. O botão de e-mail/senha existente permanece intacto.

- [ ] **Step 5: Verificar build e teste**

```bash
./gradlew assembleDebug --no-daemon
./gradlew connectedAndroidTest --tests '*GoogleButtonDemoTest' --no-daemon
```

Expected: BUILD SUCCESSFUL; teste instrumentado PASS quando AVD disponível (caso contrário, registrar limitação e manter a verificação unitária).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/ui/components/GoogleSignInButton.kt app/src/main/java/com/vizinhoalerta/ui/login/LoginScreen.kt app/src/main/java/com/vizinhoalerta/ui/cadastro/CadastroScreen.kt app/src/androidTest/java/com/vizinhoalerta/ui/GoogleButtonDemoTest.kt
git commit -m "feat: botao Continuar com Google no login e cadastro"
```

### Task 6: MainActivity — destino, navegação e logout

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/MainActivity.kt:95-258`

**Interfaces:**
- Consumes: `GoogleSignInHelper` (Task 3), `AuthRepository.precisaCompletarPerfil/limparOnboardingPendente` (Task 4), callbacks das telas (Task 5), `PerfilScreen` (Task 7).
- Produces: estado `completarPerfil: Boolean` e callback `onPerfilConcluido` consumidos por `PerfilScreen`.

- [ ] **Step 1: Implementar o destino inicial assíncrono**

`val googleSignIn = remember(context) { GoogleSignInHelper(context) }`; `var startDestination by remember { mutableStateOf<String?>(null) }`; `LaunchedEffect(modo) { val uid = authRepository.currentUid; startDestination = when { uid == null -> "welcome"; authRepository.precisaCompletarPerfil(uid) -> "perfil"; else -> "mapa" } }`; enquanto `startDestination == null`, renderizar o `CircularProgressIndicator` atual dentro do `Box` (mesmo padrão de `MainActivity.onCreate`). Ao resolver `"perfil"`, iniciar `completarPerfil = true`.

- [ ] **Step 2: Ligar navegação e logout**

`var completarPerfil by remember { mutableStateOf(false) }`; `val scope = rememberCoroutineScope()`; `BackHandler(enabled = completarPerfil) { }`; incluir `"perfil"` em `rotasSemBottomNav` quando `completarPerfil`. Nas rotas `login` e `cadastro`, passar `googleSignIn`, `modoDemo = demo` (cadastro) e `onGoogleSucesso = { incompleto -> completarPerfil = incompleto; navController.navigate(if (incompleto) "perfil" else "mapa") { popUpTo("welcome") { inclusive = true } } }`. Na rota `perfil`, passar `completarPerfil` e `onPerfilConcluido = { completarPerfil = false; navController.navigate("mapa") { popUpTo("welcome") { inclusive = true } } }`. Unificar os dois `onLogoutClick` existentes em `fun sair()`: capturar uid, `authRepository.limparOnboardingPendente(uid)`, `authRepository.logout()`, `scope.launch { googleSignIn.clearCredentialState() }` (best-effort), `completarPerfil = false`, `navController.navigate("welcome") { popUpTo(0) { inclusive = true } }`.

- [ ] **Step 3: Verificar build e teste de fluxo demo**

Run: `./gradlew assembleDebug testDebugUnitTest --no-daemon`
Expected: BUILD SUCCESSFUL e testes verdes; `LoginFlowTest` permanece com os rótulos pré-existentes desatualizados (reportar, não corrigir).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/MainActivity.kt
git commit -m "feat: onboarding obrigatorio e navegacao do login Google"
```

### Task 7: PerfilScreen em modo conclusão

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt:28-68,178-205`

**Interfaces:**
- Consumes: `AuthRepository.atualizarLocalizacaoPerfil`, `marcarOnboardingPendente`, `limparOnboardingPendente` (Task 4); `onPerfilConcluido` (Task 6).
- Produces: parâmetros `completarPerfil: Boolean = false` e `onPerfilConcluido: () -> Unit = {}`.

- [ ] **Step 1: Implementar modo obrigatório**

Novos parâmetros com default (não quebram chamadores); `LaunchedEffect(completarPerfil) { if (completarPerfil) editando = true }`. No `Salvar`, quando `completarPerfil`: exigir `cidade`/`bairro` não vazios (erro `"Informe cidade e bairro."`), chamar `atualizarLocalizacaoPerfil`; no sucesso limpar hint do uid e `onPerfilConcluido()`; na falha manter `editando = true`, marcar hint e exibir erro. Fora do modo obrigatório, manter o fluxo `atualizarPerfil(...)` atual. Ocultar `onVoltar` quando `completarPerfil`; `onLogoutClick` permanece visível.

- [ ] **Step 2: Verificar build**

Run: `./gradlew assembleDebug --no-daemon`
Expected: BUILD SUCCESSFUL; back bloqueado por `BackHandler` da Task 6.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt
git commit -m "feat: conclusao obrigatoria do perfil apos login Google"
```

### Task 8: README e checklist manual

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: nenhum símbolo novo; documenta o comportamento entregue nas Tasks 1-7.

- [ ] **Step 1: Atualizar README**

Documentar: `google-services.json` real com `oauth_client` Web e `other_platform_oauth_client`; registrar SHA-1 debug/release; habilitar provider Google; gerar Web client id; comportamento do botão no demo (aviso, sem autenticação); checklist manual dependente de Firebase real: seletor de conta, conflito de e-mail já cadastrado com senha, onboarding de cidade/bairro, reinício/back/troca de conta durante onboarding e logout. Avisar que dados demo seguem em memória e que o `LoginFlowTest` pré-existente está desatualizado (fora de escopo).

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: config Firebase e checklist manual do login Google"
```

### Task 9: Verificação final e PR

**Files:** nenhum código; apenas Issue/PR.

- [ ] **Step 1: Rodar verificação completa**

```bash
./gradlew testDebugUnitTest assembleDebug --no-daemon
git diff --check
grep -RInE 'Log\.|println|Timber' app/src/main/java/com/vizinhoalerta/data/GoogleSignIn* app/src/main/java/com/vizinhoalerta/ui/components/GoogleSignInButton.kt || echo "sem logs"
git status --short
```

Expected: BUILD SUCCESSFUL; testes verdes; `git diff --check` limpo; grep sem saída; working tree sem os artefatos fora de escopo alterados.

- [ ] **Step 2: Abrir Issue e branch**

`gh issue create --title "[feature] Login e cadastro com conta Google" --label enhancement --body "..."`; branch `feature/<numero>-google-signin` a partir de `main`.

- [ ] **Step 3: Abrir PR**

```bash
gh pr create --base main --head feature/<numero>-google-signin --title "feat: login e cadastro com Google" --body "Closes #<numero> ..."
```

Expected: PR aberto; **aguardar aprovação explícita antes de qualquer merge**.
