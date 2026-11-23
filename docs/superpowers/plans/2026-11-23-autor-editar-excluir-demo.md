# Autor editar/excluir e reposicionar no demo — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir que autor ou admin altere situação/status, exclua e reposicione uma ocorrência no mapa, com o mesmo critério em demo e produção.

**Architecture:** O guard fica na camada de dados: `OcorrenciaRepository` recebe `AuthRepository?` e, em demo, toda mutação passa por `exigirPermissaoDemo(id)`. `DetalhesScreen` usa o mesmo critério para mostrar as ações e um modo de edição com `addOnMapClickListener`. Em produção, `firestore.rules` impõe autoria e agora também permite `lat`/`lng`.

**Tech Stack:** Kotlin, Jetpack Compose, MapLibre Android SDK 13.7.1, JUnit4, Gradle (`./gradlew`).

**Spec:** `docs/superpowers/specs/2026-11-23-autor-editar-excluir-demo-design.md`

## Global Constraints

- Não alterar fluxo de criação, `userId` da ocorrência, autenticação, cadastro, perfil ou navegação fora de `detalhes/{ocorrenciaId}`.
- Não habilitar demo em release; guard de build permanece.
- Fail-closed: sem `authRepository` injetado, demo nega mutação.
- Coordenadas válidas: `-90 <= lat <= 90`, `-180 <= lng <= 180`; validar no repositório e nas regras.
- Mensagens de erro em pt-BR, padrão existente: `"Ocorrência não encontrada"`.
- Não tocar em `build/reports/problems/problems-report.html` (modificação preexistente).
- Issue: #1. Branch: `fix/1-autor-editar-excluir-demo`. PR deve conter `Closes #1`.

## Review Focus

- `userId` vazio / ninguém logado → negar. Teste: `sem login o repositorio nega`.
- `authRepository == null` em demo → negar. Teste: `sem authRepository o repositorio nega`.
- Id inexistente → falhar com "Ocorrência não encontrada". Teste: `id inexistente continua falhando`.
- Admin altera/exclui/reposiciona ocorrência alheia → permitir. Teste: `admin altera exclui e reposiciona`.
- Autor altera/exclui/reposiciona própria ocorrência → permitir. Teste: `autor altera exclui e reposiciona`.
- Coordenada fora da faixa → negar sem gravar. Teste: `localizacao fora da faixa e negada`.
- Usuário comum não vê botões em ocorrência alheia → verificação manual no AVD (Task 2).

---

### Task 1: Guard de autoria e reposicionamento no repositório + regras

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/data/OcorrenciaRepository.kt:17-21,117-255`
- Modify: `app/src/main/java/com/vizinhoalerta/MainActivity.kt:106`
- Modify: `firestore.rules:44-60`
- Test: `app/src/test/java/com/vizinhoalerta/data/OcorrenciaRepositoryDemoTest.kt`

**Interfaces:**
- Consumes: `AuthRepository(demo = true)`, `authRepository.currentUid`, `DemoData.usuarios` (`Usuario.isAdmin`), `DemoData.ocorrencias`.
- Produces: `OcorrenciaRepository(demo, firestore, storage, authRepository = null)`; `atualizarLocalizacao(id: String, lat: Double, lng: Double): Result<Unit>`; `exigirPermissaoDemo` privado; `resolver(id, userId)` mantém assinatura por compatibilidade e usa uid autenticado.

- [ ] **Step 1: Escrever os testes que falham**

Em `OcorrenciaRepositoryDemoTest.kt`, manter `DemoData.reset()` no `setUp` e adicionar:

```kotlin
private fun login(email: String, pass: String): AuthRepository =
    AuthRepository(demo = true).also { assertTrue(runBlocking { it.login(email, pass) }.isSuccess) }

@Test fun `usuario comum nao altera ocorrencia alheia`() = runBlocking {
    val auth = login("morador@demo.com", "Demo@123")
    val repo = OcorrenciaRepository(demo = true, authRepository = auth)
    val antes = repo.listar().getOrThrow().first { it.id == "cassia_1" }
    assertTrue(repo.atualizarSituacaoComStatus("cassia_1", "rejeitado", "resolvido").isFailure)
    assertTrue(repo.deletar("cassia_1").isFailure)
    assertTrue(repo.atualizarLocalizacao("cassia_1", -20.0, -46.0).isFailure)
    assertEquals(antes, repo.listar().getOrThrow().first { it.id == "cassia_1" })
}

@Test fun `autor altera exclui e reposiciona`() = runBlocking {
    val auth = login("morador@demo.com", "Demo@123")
    val repo = OcorrenciaRepository(demo = true, authRepository = auth)
    assertTrue(repo.inserir(Ocorrencia(tipo = "Suspeita", userId = auth.currentUid!!)).isSuccess)
    val id = repo.listar().getOrThrow().last().id
    assertTrue(repo.atualizarSituacaoComStatus(id, "rejeitado", "resolvido").isSuccess)
    assertTrue(repo.atualizarLocalizacao(id, -20.5, -46.9).isSuccess)
    val o = repo.obterPorId(id)!!
    assertEquals(-20.5, o.lat, 0.0)
    assertEquals(-46.9, o.lng, 0.0)
    assertTrue(repo.deletarComMidia(id).isSuccess)
    assertFalse(repo.listar().getOrThrow().any { it.id == id })
}

@Test fun `admin altera exclui e reposiciona`() = runBlocking {
    val auth = login("admin@demo.com", "Demo@123")
    val repo = OcorrenciaRepository(demo = true, authRepository = auth)
    assertTrue(repo.atualizarSituacaoComStatus("cassia_1", "rejeitado", "resolvido").isSuccess)
    assertTrue(repo.atualizarLocalizacao("cassia_1", -20.5, -46.9).isSuccess)
    assertTrue(repo.deletarComMidia("cassia_1").isSuccess)
    assertFalse(repo.listar().getOrThrow().any { it.id == "cassia_1" })
}

@Test fun `localizacao fora da faixa e negada`() = runBlocking {
    val auth = login("admin@demo.com", "Demo@123")
    val repo = OcorrenciaRepository(demo = true, authRepository = auth)
    val antes = repo.obterPorId("cassia_1")!!
    assertTrue(repo.atualizarLocalizacao("cassia_1", 91.0, -46.9).isFailure)
    assertTrue(repo.atualizarLocalizacao("cassia_1", -20.0, -181.0).isFailure)
    assertEquals(antes.lat, repo.obterPorId("cassia_1")!!.lat, 0.0)
}

@Test fun `sem authRepository o repositorio nega`() = runBlocking {
    val repo = OcorrenciaRepository(demo = true)
    assertTrue(repo.atualizarSituacao("cassia_1", "rejeitado").isFailure)
    assertTrue(repo.resolver("cassia_1", "demo-admin").isFailure)
    assertTrue(repo.atualizarLocalizacao("cassia_1", -20.0, -46.0).isFailure)
    assertTrue(repo.deletar("cassia_1").isFailure)
}

@Test fun `sem login o repositorio nega`() = runBlocking {
    val repo = OcorrenciaRepository(demo = true, authRepository = AuthRepository(demo = true))
    assertTrue(repo.deletar("cassia_1").isFailure)
}

@Test fun `id inexistente continua falhando`() = runBlocking {
    val auth = login("admin@demo.com", "Demo@123")
    val repo = OcorrenciaRepository(demo = true, authRepository = auth)
    val r = repo.atualizarSituacaoComStatus("nao_existe", "rejeitado", "resolvido")
    assertTrue(r.isFailure)
    assertEquals("Ocorrência não encontrada", r.exceptionOrNull()?.message)
}
```

Ajustar o teste existente `atualizar situacao persiste no demo` para logar como admin. Adicionar imports `assertFalse` e `com.vizinhoalerta.model.Ocorrencia`.

- [ ] **Step 2: Rodar e confirmar que falha**

Run: `cd /home/alvaro/vizinho-alerta && ./gradlew testDebugUnitTest --tests com.vizinhoalerta.data.OcorrenciaRepositoryDemoTest --no-daemon`
Expected: FAIL — construtor não aceita `authRepository`, `atualizarLocalizacao` não existe.

- [ ] **Step 3: Implementar repositório e regras**

Em `OcorrenciaRepository.kt`: adicionar `private val authRepository: AuthRepository? = null` ao construtor; helper privado `exigirPermissaoDemo(id: String): Result<Unit>` que localiza em `DemoData.ocorrencias` (falha com "Ocorrência não encontrada"), obtém `uid = authRepository?.currentUid`, calcula `admin = DemoData.usuarios.firstOrNull { it.id == uid }?.isAdmin == true` e falha com "Você não tem permissão para alterar esta ocorrência." quando `uid == null || (o.userId != uid && !admin)`. Chamá-lo em `atualizarSituacao`, `atualizarSituacaoComStatus`, `resolver`, `deletar`, `deletarComMidia` e `atualizarLocalizacao` quando `demo == true`. Em `resolver`, manter `userId` por compatibilidade sem autorizar com ele. `atualizarLocalizacao` valida a faixa antes de mutar; em produção atualiza `lat`/`lng` via Firestore.

Em `MainActivity.kt:106`, trocar `OcorrenciaRepository(demo = demo)` por `OcorrenciaRepository(demo = demo, authRepository = authRepository)` — sem isso o guard nunca recebe o uid no app.

Em `firestore.rules`: nas duas cláusulas `allow update` de `/ocorrencias/{id}`, trocar `hasOnly(['situacao', 'status'])` por `hasOnly(['situacao', 'status', 'lat', 'lng'])` e acrescentar validação `lat`/`lng` `is number` e faixa. `allow delete` permanece autor ou admin.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `cd /home/alvaro/vizinho-alerta && ./gradlew testDebugUnitTest --tests com.vizinhoalerta.data.OcorrenciaRepositoryDemoTest --no-daemon`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
cd /home/alvaro/vizinho-alerta
git add docs/superpowers/specs/2026-11-23-autor-editar-excluir-demo-design.md \
        docs/superpowers/plans/2026-11-23-autor-editar-excluir-demo.md \
        app/src/main/java/com/vizinhoalerta/data/OcorrenciaRepository.kt \
        app/src/main/java/com/vizinhoalerta/MainActivity.kt \
        app/src/test/java/com/vizinhoalerta/data/OcorrenciaRepositoryDemoTest.kt \
        firestore.rules
git commit -m "fix: autor e admin alteram, excluem e reposicionam no demo"
```

---

### Task 2: Ações e modo de edição na tela de detalhes

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/ui/detalhes/DetalhesScreen.kt:45-61,266-356`
- Modify: `app/src/main/java/com/vizinhoalerta/MainActivity.kt:197-207`

**Interfaces:**
- Consumes: `authRepository.currentUid`, `authRepository.isAdmin()`, `ocorrenciaRepository.atualizarLocalizacao` e `deletarComMidia` (Task 1), `onVoltar`, `MapLibreMap.addOnMapClickListener`.
- Produces: `DetalhesScreen(ocorrenciaId, ocorrenciaRepository, authRepository, onVoltar)`; estados `podeGerenciar`, `editandoLocalizacao`, `posicaoSelecionada`.

- [ ] **Step 1: Permissão e estado de edição**

Em `DetalhesScreen`, adicionar parâmetro `authRepository: AuthRepository`; estados `podeGerenciar`, `editandoLocalizacao` e `posicaoSelecionada: LatLng?`. No `LaunchedEffect(ocorrenciaId)`, após carregar, atribuir `podeGerenciar = o?.userId == authRepository.currentUid || authRepository.isAdmin()`. Usar `pos = posicaoSelecionada ?: LatLng(o.lat, o.lng)` no mapa.

- [ ] **Step 2: Tocar no mapa move o marcador**

No bloco `AndroidView` do mapa de detalhes, registrar uma única vez `map.addOnMapClickListener(MapLibreMap.OnMapClickListener { point -> if (editandoLocalizacaoAtual) posicaoSelecionada = point; true })` usando `rememberUpdatedState` para não capturar estado obsoleto. O bloco de atualização do `AndroidView` limpa o mapa, adiciona o marcador em `pos` e centraliza a câmera. Registrar `removeOnMapClickListener` no `onDispose`.

- [ ] **Step 3: Botões condicionais**

Mostrar "MARCAR COMO RESOLVIDO" e "EXCLUIR" somente quando `podeGerenciar`; "EXCLUIR" chama `deletarComMidia` e `onVoltar()` no sucesso. Acrescentar "ALTERAR LOCALIZAÇÃO" quando `podeGerenciar && !editandoLocalizacao`. No modo de edição, mostrar "SALVAR LOCALIZAÇÃO" (chama `atualizarLocalizacao`, atualiza `ocorrencia` e `posicaoSelecionada = null`, sai do modo) e "CANCELAR" (sai do modo sem salvar).

- [ ] **Step 4: Passar authRepository no roteamento**

Em `MainActivity`, no `composable("detalhes/{ocorrenciaId}")`, passar `authRepository = authRepository` para `DetalhesScreen`.

- [ ] **Step 5: Compilar e verificar no AVD**

Run: `cd /home/alvaro/vizinho-alerta && ./gradlew assembleDebug --no-daemon` → `BUILD SUCCESSFUL`.
No AVD demo: com `testador@demo.com`, abrir "Iluminação danificada" (dono `demo-admin`) e confirmar ausência das ações; entrar com `admin@demo.com` / `Demo@123`, tocar no mapa, salvar e confirmar que o marcador e os dados mudam; criar ocorrência com a conta comum e confirmar que as ações aparecem nela.

- [ ] **Step 6: Commit**

```bash
cd /home/alvaro/vizinho-alerta
git add app/src/main/java/com/vizinhoalerta/ui/detalhes/DetalhesScreen.kt \
        app/src/main/java/com/vizinhoalerta/MainActivity.kt
git commit -m "feat: edita localizacao no mapa com permissao de autor ou admin"
```

---

### Task 3: Verificação final e PR

**Files:** nenhum código novo.

- [ ] **Step 1: Suíte completa e build**

Run: `cd /home/alvaro/vizinho-alerta && ./gradlew testDebugUnitTest assembleDebug --no-daemon`
Expected: todos os testes passam e `BUILD SUCCESSFUL`.

- [ ] **Step 2: Guard de release continua ativo**

Run: `cd /home/alvaro/vizinho-alerta && ./gradlew compileReleaseKotlin --no-daemon`
Expected: falha com `google-services.json` DUMMY, como antes.

- [ ] **Step 3: Revisar o diff**

Run: `cd /home/alvaro/vizinho-alerta && git diff --check d82b6e9..HEAD && git diff --stat d82b6e9..HEAD`
Expected: nenhum erro de whitespace; apenas arquivos das Tasks 1 e 2.

- [ ] **Step 4: Publicar e abrir PR**

```bash
cd /home/alvaro/vizinho-alerta
git push -u origin fix/1-autor-editar-excluir-demo
gh pr create --title 'fix: autor e admin gerenciam e reposicionam ocorrências no demo' \
  --body 'Closes #1

Alinha o demo a `firestore.rules`: autor ouadmin alteram situação/status, excluem e reposicionam ocorrências. `DetalhesScreen` ganha modo de edição no mapa. Testes cobrem autor, não-autor, admin, fail-closed e faixa de coordenadas.' \
  --label bug
```
Expected: PR aberto com `Closes #1`.

- [ ] **Step 5: Não mesclar sem aprovação explícita**
