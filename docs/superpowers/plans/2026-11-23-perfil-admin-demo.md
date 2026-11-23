# Perfil editável, admin e fallback demo — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir editar o próprio perfil, dar ao admin listas de usuários e de todas as ocorrências, e fazer o debug cair automaticamente para dados locais quando não há Firebase.

**Architecture:** Uma sonda no startup escolhe `FIREBASE` ou `DEMO` (somente debug); `AuthRepository` e `OcorrenciaRepository` recebem `demo: Boolean` e leem/escrevem `DemoData` em memória nesse caso. As telas continuam recebendo os mesmos repositórios; `AdminScreen` ganha abas e `PerfilScreen` ganha formulário.

**Tech Stack:** Kotlin, Jetpack Compose, Firebase Auth/Firestore, coroutines + `kotlinx-coroutines-play-services` (já instalado), JUnit JVM. Testes usam `runBlocking`; nenhuma dependência nova.

**Spec:** `docs/superpowers/specs/2026-11-23-perfil-admin-demo-design.md`

## Global Constraints

- Fallback demo **somente debug**; release nunca aceita login fictício.
- Timeout da sonda: **4 s**; `PERMISSION_DENIED` = Firebase vivo; qualquer falha de conexão = demo.
- Login demo: senha mínima **6**; cadastro demo: senha mínima **8** com a validação atual (`AuthRepository.validarSenhaSegura`).
- Admin demo: `admin@demo.com`; morador demo: `morador@demo.com`; senha `Demo@123`.
- 5 ocorrências demo, uma por situação: `registrado`, `em_verificacao`, `confirmado`, `resolvido`, `rejeitado`.
- Ação Rejeitado: `situacao = "rejeitado"`, `status = "resolvido"`.
- Nenhuma dependência nova; release com `google-services.json` dummy continua bloqueado.
- Comandos de verificação usam `./gradlew ... --no-daemon`; Java 17 para Gradle.

## Review Focus

- **Sem Firebase o app precisa funcionar de ponta a ponta** — `AuthRepositoryDemoTest.login admin`, `DemoDataTest`, `BackendSelectorTest`.
- **Release não pode cair para demo** — `BackendSelectorTest.detectar debug=false com probe que falha`.
- **Usuário comum não pode se promover a admin alterando `role`** — verificação no emulador Firestore na Task 8.
- **Admin deve ver as 5 situações, inclusive rejeitado, e a ação Rejeitado fecha** — `DemoDataTest` + verificação por OCR na Task 9.
- **Username duplicado não pode ser aceito no perfil** — `AuthRepositoryDemoTest.atualizarPerfil rejeita username em uso`.

---

### Task 1: Fixture de dados demo

**Files:**
- Create: `app/src/main/java/com/vizinhoalerta/data/DemoData.kt`
- Test: `app/src/test/java/com/vizinhoalerta/data/DemoDataTest.kt`

**Interfaces:**
- Produces: `object DemoData` com `const val ADMIN_EMAIL = "admin@demo.com"`, `const val SENHA_DEMO = "Demo@123"`, `val usuarios: MutableList<Usuario>`, `val ocorrencias: MutableList<Ocorrencia>`, `fun reset()`.

- [ ] **Step 1: Escrever teste falhando**

```kotlin
class DemoDataTest {
    @Before fun setUp() { DemoData.reset() }

    @Test fun `fixture tem admin e morador`() {
        assertEquals(2, DemoData.usuarios.size)
        assertTrue(DemoData.usuarios.first { it.email == DemoData.ADMIN_EMAIL }.isAdmin)
        assertFalse(DemoData.usuarios.first { it.email == "morador@demo.com" }.isAdmin)
    }

    @Test fun `fixture cobre todas as situacoes em Cassia e Passos`() {
        assertEquals(5, DemoData.ocorrencias.size)
        assertEquals(
            setOf("registrado", "em_verificacao", "confirmado", "resolvido", "rejeitado"),
            DemoData.ocorrencias.map { it.situacao }.toSet()
        )
        assertTrue(DemoData.ocorrencias.all { it.lat < -19.0 && it.lat > -21.0 })
        assertTrue(DemoData.ocorrencias.all { it.cidade in setOf("Cássia", "Passos") })
    }

    @Test fun `reset restaura a fixture`() {
        DemoData.usuarios.clear()
        DemoData.reset()
        assertEquals(2, DemoData.usuarios.size)
    }
}
```

- [ ] **Step 2: Rodar teste e confirmar falha**

Run: `./gradlew testDebugUnitTest --tests '*DemoDataTest' --no-daemon`
Expected: FAIL (símbolo `DemoData` não existe).

- [ ] **Step 3: Implementar `DemoData`**

`reset()` limpa e repovoa as duas listas. Usuários: ids `demo-admin`/`demo-morador`, nomes "Admin Demo"/"Morador Demo", `username` `@admin`/`@morador`, `cidade = "Cássia"`, `bairro = "Centro"`, `role` `admin`/`user`. Ocorrências: ids `cassia_1..3` e `passos_1..2`, coordenadas de Cássia (`-20.5825,-46.9219`) e Passos (`-20.7160,-46.6150`), uma situação por item, `userId = "demo-admin"` e `usuarioNome = "Admin Demo"`.

- [ ] **Step 4: Rodar teste e confirmar passagem**

Run: `./gradlew testDebugUnitTest --tests '*DemoDataTest' --no-daemon`
Expected: PASS (3 testes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/data/DemoData.kt app/src/test/java/com/vizinhoalerta/data/DemoDataTest.kt
git commit -m "test: fixture demo com usuarios e todas as situacoes"
```

### Task 2: Seletor de backend

**Files:**
- Create: `app/src/main/java/com/vizinhoalerta/data/BackendSelector.kt`
- Test: `app/src/test/java/com/vizinhoalerta/data/BackendSelectorTest.kt`

**Interfaces:**
- Produces: `object BackendSelector { enum class Mode { FIREBASE, DEMO }; suspend fun detectar(debug: Boolean = BuildConfig.DEBUG, probe: suspend () -> Unit = { ... }): Mode }`.

- [ ] **Step 1: Escrever teste falhando**

```kotlin
class BackendSelectorTest {
    @Test fun `release nunca usa demo`() = runBlocking {
        assertEquals(BackendSelector.Mode.FIREBASE,
            BackendSelector.detectar(debug = false) { error("sem rede") })
    }
    @Test fun `debug com backend vivo usa firebase`() = runBlocking {
        assertEquals(BackendSelector.Mode.FIREBASE, BackendSelector.detectar(debug = true) { })
    }
    @Test fun `debug sem backend usa demo`() = runBlocking {
        assertEquals(BackendSelector.Mode.DEMO,
            BackendSelector.detectar(debug = true) { error("unavailable") })
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `./gradlew testDebugUnitTest --tests '*BackendSelectorTest' --no-daemon`
Expected: FAIL.

- [ ] **Step 3: Implementar `BackendSelector.detectar`**

Se `!debug`, retorne `FIREBASE` sem chamar `probe`. Caso contrário, `withTimeout(4_000) { probe() }`; sucesso ou `FirebaseFirestoreException` com `code == PERMISSION_DENIED` → `FIREBASE`; qualquer outra exceção (inclusive `TimeoutCancellationException`) → `DEMO`. O `probe` padrão executa `FirebaseFirestore.getInstance().collection("ocorrencias").limit(1).get().await()`.

- [ ] **Step 4: Rodar e confirmar passagem**

Run: `./gradlew testDebugUnitTest --tests '*BackendSelectorTest' --no-daemon`
Expected: PASS (3 testes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/data/BackendSelector.kt app/src/test/java/com/vizinhoalerta/data/BackendSelectorTest.kt
git commit -m "feat: sonda backend com fallback apenas no debug"
```

### Task 3: AuthRepository em modo demo

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt:31-49,67-83,85-142,174-214,231-238`
- Test: `app/src/test/java/com/vizinhoalerta/data/AuthRepositoryDemoTest.kt`

**Interfaces:**
- Consumes: `DemoData`.
- Produces: `AuthRepository(demo: Boolean = false, auth: FirebaseAuth? = ..., firestore: FirebaseFirestore? = ...)`; `val currentUid: String?`; `val currentNome: String`; `suspend fun isAdmin(): Boolean`; `suspend fun listarUsuarios(): List<Usuario>`; `login`/`cadastrar` passam a devolver `Result<Unit>`; `atualizarPerfil` mantém a assinatura atual.

- [ ] **Step 1: Escrever testes falhando**

```kotlin
class AuthRepositoryDemoTest {
    private lateinit var repo: AuthRepository
    @Before fun setUp() { DemoData.reset(); repo = AuthRepository(demo = true) }

    @Test fun `login admin demo`() = runBlocking {
        assertTrue(repo.login(DemoData.ADMIN_EMAIL, DemoData.SENHA_DEMO).isSuccess)
        assertEquals("demo-admin", repo.currentUid)
        assertTrue(repo.isAdmin())
    }

    @Test fun `cadastro demo valida senha e cria usuario comum`() = runBlocking {
        assertTrue(repo.cadastrar("Novo", "novo@demo.com", "fraca", cidade="Cássia", bairro="Centro").isFailure)
        assertTrue(repo.cadastrar("Novo", "novo@demo.com", "Demo@123", cidade="Cássia", bairro="Centro").isSuccess)
        assertTrue(repo.listarUsuarios().any { it.email == "novo@demo.com" && !it.isAdmin })
    }

    @Test fun `atualizar perfil demo persiste e bloqueia username duplicado`() = runBlocking {
        repo.login(DemoData.ADMIN_EMAIL, DemoData.SENHA_DEMO)
        assertTrue(repo.atualizarPerfil("Admin Novo", "@morador", "", "", "Passos", "Centro").isFailure)
        assertTrue(repo.atualizarPerfil("Admin Novo", "@admin2", "", "", "Passos", "Centro").isSuccess)
        assertEquals("Passos", repo.getDadosUsuario("demo-admin")?.cidade)
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `./gradlew testDebugUnitTest --tests '*AuthRepositoryDemoTest' --no-daemon`
Expected: FAIL.

- [ ] **Step 3: Implementar branch demo**

`demo = true`: `login` aceita e-mail não vazio e senha com 6+; administrador quando o e-mail normalizado é `ADMIN_EMAIL`, senão usa/cria morador em `DemoData.usuarios`. `cadastrar` usa `validarSenhaSegura`, rejeita e-mail duplicado e acrescenta usuário com `role = "user"`. `currentUid`/`currentNome` leem a sessão demo. `isAdmin()` lê `role` do usuário demo. `listarUsuarios()` devolve cópia da lista demo. `atualizarPerfil` aplica a normalização de username existente, verifica duplicidade em `DemoData.usuarios` (ignorando o próprio uid) e atualiza o objeto. `getDadosUsuario` busca na lista demo. No branch Firebase, `auth!!`/`firestore!!` preservam o comportamento atual e `isAdmin()` usa claim `admin` ou `role` do documento; `listarUsuarios()` faz query ordenada por `nome` com `doc.toObject(...).copy(id = doc.id)`. Troque os tipos de retorno de `login`/`cadastrar` para `Result<Unit>` (as telas só usam `onSuccess`/`onFailure`).

- [ ] **Step 4: Rodar e confirmar passagem**

Run: `./gradlew testDebugUnitTest --tests '*AuthRepositoryDemoTest' --no-daemon`
Expected: PASS (3 testes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt app/src/test/java/com/vizinhoalerta/data/AuthRepositoryDemoTest.kt
git commit -m "feat: autenticacao e perfil em modo demo"
```

### Task 4: OcorrenciaRepository em modo demo

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/data/OcorrenciaRepository.kt:16-18,77-108,178-186`
- Test: `app/src/test/java/com/vizinhoalerta/data/OcorrenciaRepositoryDemoTest.kt`

**Interfaces:**
- Consumes: `DemoData`.
- Produces: `OcorrenciaRepository(demo: Boolean = false, firestore: FirebaseFirestore? = ...)`; `suspend fun listar(): Result<List<Ocorrencia>>`.

- [ ] **Step 1: Escrever teste falhando**

```kotlin
class OcorrenciaRepositoryDemoTest {
    private lateinit var repo: OcorrenciaRepository
    @Before fun setUp() { DemoData.reset(); repo = OcorrenciaRepository(demo = true) }

    @Test fun `listar devolve as cinco situacoes`() = runBlocking {
        val lista = repo.listar().getOrThrow()
        assertEquals(5, lista.size)
        assertTrue(lista.any { it.situacao == "rejeitado" })
    }

    @Test fun `atualizar situacao persiste no demo`() = runBlocking {
        assertTrue(repo.atualizarSituacaoComStatus("cassia_1", "rejeitado", "resolvido").isSuccess)
        assertEquals("rejeitado", repo.listar().getOrThrow().first { it.id == "cassia_1" }.situacao)
        assertEquals("resolvido", repo.listar().getOrThrow().first { it.id == "cassia_1" }.status)
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha**

Run: `./gradlew testDebugUnitTest --tests '*OcorrenciaRepositoryDemoTest' --no-daemon`
Expected: FAIL.

- [ ] **Step 3: Implementar branch demo**

Construtor com `demo` e `firestore` anulável. `observarOcorrencias()` e `listar()` usam `DemoData.ocorrencias` quando demo; `listar()` devolve `Result.success(cópia)` no demo e `Result.failure(e)` no Firebase (em vez de engolir a exceção). `obterPorId`, `inserir`, `atualizarSituacao`, `atualizarSituacaoComStatus` e `deletarComMidia` leem/escrevem a lista demo nesse modo.

- [ ] **Step 4: Rodar e confirmar passagem**

Run: `./gradlew testDebugUnitTest --tests '*OcorrenciaRepositoryDemoTest' --no-daemon`
Expected: PASS (2 testes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/data/OcorrenciaRepository.kt app/src/test/java/com/vizinhoalerta/data/OcorrenciaRepositoryDemoTest.kt
git commit -m "feat: ocorrencias em modo demo com erro propagado"
```

### Task 5: Ligações de backend e chamadores de sessão

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/MainActivity.kt:45-91`, `app/src/main/java/com/vizinhoalerta/ui/login/LoginScreen.kt:72-94`, `app/src/main/java/com/vizinhoalerta/ui/mapa/MapaScreen.kt:78`, `app/src/main/java/com/vizinhoalerta/ui/reportar/ReportarScreen.kt:90,361-362`

**Interfaces:**
- Consumes: `BackendSelector.detectar`, `AuthRepository(demo=)`, `OcorrenciaRepository(demo=)`, `AuthRepository.currentUid/currentNome`.

- [ ] **Step 1: Implementar sonda no `MainActivity`**

`VizinhoAlertaApp` recebe apenas `modo: BackendSelector.Mode`; `MainActivity.onCreate` roda `lifecycleScope.launch { val modo = BackendSelector.detectar(); setContent { ... } }`, exibindo um `CircularProgressIndicator` antes do resultado. Crie os repositórios com `demo = (modo == DEMO)` dentro do composable usando `remember(modo)`. Em release, se a sonda falhar, permaneça em `FIREBASE` e exiba "Não foi possível conectar ao Firebase." na tela.

- [ ] **Step 2: Ajustar chamadores**

Substitua `currentUser?.uid` por `currentUid` e `currentUser?.displayName` por `currentNome` nos quatro arquivos listados. `MapaScreen` usa `currentUid != null`.

- [ ] **Step 3: Dica de credenciais no login**

Em `LoginScreen`, aceite `modoDemo: Boolean = false` e mostre `Text("Demo: admin@demo.com / Demo@123")` somente quando true; passe o valor de `VizinhoAlertaApp`.

- [ ] **Step 4: Verificar e commitar**

Run: `./gradlew assembleDebug testDebugUnitTest --no-daemon`
Expected: BUILD SUCCESSFUL; 12 testes anteriores + 8 novos passam.

```bash
git add app/src/main/java/com/vizinhoalerta/MainActivity.kt app/src/main/java/com/vizinhoalerta/ui/login/LoginScreen.kt app/src/main/java/com/vizinhoalerta/ui/mapa/MapaScreen.kt app/src/main/java/com/vizinhoalerta/ui/reportar/ReportarScreen.kt
git commit -m "feat: seleciona backend no startup e expoe sessao demo"
```

### Task 6: Perfil editável

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt:18-90`

**Interfaces:**
- Consumes: `AuthRepository.currentUid`, `atualizarPerfil`, `isAdmin`, `getDadosUsuario`.

- [ ] **Step 1: Implementar formulário**

`PerfilScreen` carrega `usuario` e `ehAdmin = authRepository.isAdmin()`. Botão **Editar dados** alterna para seis `OutlinedTextField` (nome, username, telefone, estado, cidade, bairro) pré-preenchidos. **Salvar** chama `atualizarPerfil(...)`, mostra erro em `Text` vermelho ou mensagem "Dados atualizados." e recarrega o usuário. Botão **Painel Admin** passa a depender de `ehAdmin`.

- [ ] **Step 2: Verificar e commitar**

Run: `./gradlew assembleDebug testDebugUnitTest --no-daemon`
Expected: BUILD SUCCESSFUL, sem novas falhas.

```bash
git add app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt
git commit -m "feat: edicao de dados no perfil"
```

### Task 7: Painel admin com abas

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/ui/admin/AdminScreen.kt:30-179`
- Create: `app/src/main/java/com/vizinhoalerta/ui/admin/AdminUsuariosTab.kt`

**Interfaces:**
- Consumes: `OcorrenciaRepository.listar(): Result<List<Ocorrencia>>`, `AuthRepository.listarUsuarios()`, `AuthRepository.isAdmin()`.

- [ ] **Step 1: Proteger a rota admin**

Em `MainActivity`, dentro de `composable("admin")`, chame `authRepository.isAdmin()` em `LaunchedEffect`; se falso, mostre "Acesso negado" e botão **Voltar** em vez de `AdminScreen`.

- [ ] **Step 2: Implementar abas e lista de usuários**

`AdminScreen` passa a receber `authRepository: AuthRepository` e usa `TabRow` com abas **Ocorrências** e **Usuários**. `AdminUsuariosTab(authRepository)` carrega `listarUsuarios()`, mostra `nome`, `email` e `role` em `Card`s, com estados de carregamento, lista vazia e erro.

- [ ] **Step 3: Ajustar aba de ocorrências**

Use `listar().getOrThrow()` dentro de `runCatching`, atribuindo `erro` em caso de falha. Inclua mensagem "Nenhuma ocorrência." quando vazia. Adicione `TextButton("Rejeitado")` chamando `onAtualizar(ocorrencia, "rejeitado", "resolvido")`.

- [ ] **Step 4: Verificar e commitar**

Run: `./gradlew assembleDebug testDebugUnitTest --no-daemon`
Expected: BUILD SUCCESSFUL.

```bash
git add app/src/main/java/com/vizinhoalerta/MainActivity.kt app/src/main/java/com/vizinhoalerta/ui/admin/AdminScreen.kt app/src/main/java/com/vizinhoalerta/ui/admin/AdminUsuariosTab.kt
git commit -m "feat: admin com abas de ocorrencias e usuarios"
```

### Task 8: Regras Firestore para admin por role

**Files:**
- Modify: `firestore.rules:1-43`

**Interfaces:**
- Consumes: campo `role` em `usuarios/{uid}`.

- [ ] **Step 1: Atualizar regras**

Defina `isAdminClaim()`, `isAdminRole()` (exige `exists(/databases/$(database)/documents/usuarios/$(request.auth.uid))` **e** `get(...).data.role == 'admin'`) e `isAdmin() = isAdminClaim() || isAdminRole()`. Em `usuarios/{uid}`: `get` só próprio ou admin, `list` só admin, `create` exige `request.auth.uid == uid && request.resource.data.role == 'user'`, `update` do próprio usuário exige `request.resource.data.role == resource.data.role`, `write` admin liberado. Substitua `request.auth.token.admin == true` por `isAdmin()` em `ocorrencias` e `moderacao`.

- [ ] **Step 2: Verificar no emulador**

Com emuladores rodando, use REST/Firestore para confirmar: morador consegue ler o próprio documento; morador não consegue `list` em `usuarios`; morador não consegue alterar `role`; usuário com `role = "admin"` consegue `list` em `usuarios` e atualizar `situacao` de ocorrência de terceiro para `confirmado`.

Run: `curl -s -o /dev/null -w '%{http_code}\n' ...` para cada cenário.
Expected: 200 para leitura própria e ações admin; 403 para negações.

- [ ] **Step 3: Commit**

```bash
git add firestore.rules
git commit -m "fix: regras permitem admin por role sem auto-promocao"
```

### Task 9: README, capturas e verificação final

**Files:**
- Modify: `README.md` (na seção "google-services.json (protótipo)" e galeria)
- Modify: `docs/screenshots/01-perfil.png`, `docs/screenshots/02-admin.png`
- Create: `docs/screenshots/08-admin-usuarios.png`

**Interfaces:**
- Consumes: build debug do modo demo.

- [ ] **Step 1: Aviso no README**

Acrescente aviso em destaque: sem `app/google-services.json` real (ou sem emuladores), o app debug entra automaticamente no **modo demo**: login `admin@demo.com` / `Demo@123`, dados em memória; ao fechar o app, tudo volta ao padrão do repositório. Release exige Firebase real e nunca usa demo.

- [ ] **Step 2: Capturas no modo demo**

Com emuladores parados, instale o debug, entre como `admin@demo.com`, abra Perfil → Editar dados (deixe os campos preenchidos visíveis), capture `01-perfil.png`; abra Painel Admin → aba Ocorrências, capture `02-admin.png`; aba Usuários, capture `08-admin-usuarios.png`. Valide 1080x2280, cores e OCR contendo "Cássia", "Passos", "admin@demo.com".

- [ ] **Step 3: Verificação final**

Run: `./gradlew assembleDebug testDebugUnitTest --no-daemon`
Expected: EXIT 0, todos os testes passam.

Run: `./gradlew compileReleaseKotlin --no-daemon` com chave temporária não-dummy, restaurando `app/google-services.json` logo depois.
Expected: EXIT 0; hash do arquivo restaurado igual ao original.

Run: `git diff | grep -iE 'AIza|secret|token'`
Expected: nenhum segredo.

- [ ] **Step 4: Commit e push**

```bash
git add README.md docs/screenshots/01-perfil.png docs/screenshots/02-admin.png docs/screenshots/08-admin-usuarios.png
git commit -m "docs: aviso modo demo e capturas perfil/admin"
git push origin main
```
