# Seletor de tema no Perfil Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir escolher tema no Perfil (Seguir o sistema / Claro / Escuro), com persistência e aplicação imediata sem recriar a Activity.

**Architecture:** Preferência salva em SharedPreferences e lida em `MainActivity`; `VizinhoAlertaTheme` resolve o tema efetivo com função pura e ajusta cores do Compose e da barra de status via `WindowCompat`. Um composable pequeno em Perfil abre o seletor.

**Tech Stack:** Kotlin, Jetpack Compose Material3, `activity-compose:1.9.0`, `core-ktx:1.13.1`, SharedPreferences.

**Spec:** `docs/superpowers/specs/2026-11-28-tema-perfil-design.md`

## Global Constraints

- Nenhuma dependência nova (sem AppCompat, sem DataStore, sem Robolectric).
- Não sobrescrever `LocalConfiguration`; tema efetivo vem de `temaEfetivo(preferencia, modoNoturnoSistema)`.
- SharedPreferences: arquivo `tema_prefs`, chave `preferencia_tema`, padrão `SISTEMA`.
- Barra de status: claro `#FFFBE8` com ícones escuros; escuro `#1C1B1F` com ícones claros.
- `ComponentActivity` permanece; troca de tema não pode chamar `recreate()` nem navegar.

## Review Focus

- Preferência ausente/inválida → `SISTEMA`; teste: `PreferenciaTemaTest.preferencia_padrao_quando_ausente` e `RepositorioTemaTest.valor_invalido_volta_para_sistema`.
- `SISTEMA` acompanha o Android; `CLARO`/`ESCURO` ignoram o Android; teste: `PreferenciaTemaTest` (4 casos).
- Escolha manual com sistema invertido mantém o tema escolhido; teste: `PreferenciaTemaTest.override_ignora_mudanca_do_sistema`.
- Contraste dos ícones da barra de status acompanha o tema manual; teste: aceite em aparelho (Perfil → Tema, com Android em claro e em escuro), observando ícones claros/escuros.
- Trocar tema mantém rota atual e não recria Activity; teste: aceite em aparelho (permanece em Perfil, sem flash de loading).
- Preferência sobrevive ao fechamento; teste: `RepositorioTemaTest.salvar_e_ler` + aceite ao reabrir o app.

---

### Task 1: Modelo e persistência da preferência

**Files:**
- Create: `app/src/main/java/com/vizinhoalerta/ui/theme/PreferenciaTema.kt`
- Test: `app/src/test/java/com/vizinhoalerta/ui/theme/PreferenciaTemaTest.kt`
- Test: `app/src/androidTest/java/com/vizinhoalerta/ui/theme/RepositorioTemaTest.kt`

**Interfaces:**
- Consumes: nada.
- Produces: `enum class PreferenciaTema { SISTEMA, CLARO, ESCURO }`; `fun temaEfetivo(preferencia: PreferenciaTema, modoNoturnoSistema: Boolean): Boolean`; `object RepositorioTema { fun ler(context: Context): PreferenciaTema; fun salvar(context: Context, preferencia: PreferenciaTema) }`.

- [ ] **Step 1: Escrever testes unitários falhantes**

`PreferenciaTemaTest.kt`: `sistema_claro`, `sistema_escuro`, `claro_ignora_sistema`, `escuro_ignora_sistema`, `override_ignora_mudanca_do_sistema` — afirmando `temaEfetivo(...)` respectivamente `false`, `true`, `false`, `true`, `true` com `CLARO` e sistema escuro.

- [ ] **Step 2: Rodar e confirmar falha**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.vizinhoalerta.ui.theme.PreferenciaTemaTest' --console=plain`
Expected: FAIL (símbolo não resolvido).

- [ ] **Step 3: Implementar modelo, função pura e repositório**

`PreferenciaTema.kt`: enum, `temaEfetivo` (`SISTEMA` → `modoNoturnoSistema`; `ESCURO` → `true`; `CLARO` → `false`) e `RepositorioTema` com `getSharedPreferences("tema_prefs", MODE_PRIVATE)`, lendo `preferencia_tema` via `nameOrNull` com fallback `SISTEMA`, salvando com `apply()`.

- [ ] **Step 4: Rodar testes unitários**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.vizinhoalerta.ui.theme.PreferenciaTemaTest' --console=plain`
Expected: PASS (5 testes).

- [ ] **Step 5: Escrever teste instrumentado de persistência**

`RepositorioTemaTest.kt`: `salvar_e_ler` (salvar `ESCURO`, ler de novo = `ESCURO`), `preferencia_padrao_quando_ausente` (sem chave = `SISTEMA`), `valor_invalido_volta_para_sistema` (gravar `"luz"`, ler = `SISTEMA`). Usar `ApplicationProvider` do androidx.test e limpar o arquivo no `@Before`.

- [ ] **Step 6: Rodar no aparelho e commitar**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vizinhoalerta.ui.theme.RepositorioTemaTest --console=plain`
Expected: BUILD SUCCESSFUL (3 testes).
Commit: `git commit -m "feat: preferencia de tema com persistencia"`

### Task 2: Tema efetivo e barra de status

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/ui/theme/Theme.kt`

**Interfaces:**
- Consumes: `PreferenciaTema`, `temaEfetivo` (Task 1).
- Produces: `@Composable fun VizinhoAlertaTheme(preferencia: PreferenciaTema = PreferenciaTema.SISTEMA, content: @Composable () -> Unit)`.

- [ ] **Step 1: Trocar assinatura e resolução**

Manter leitura de `uiMode`; calcular `val escuro = temaEfetivo(preferencia, isNightMode)`; usar `escuro` para escolher esquema. Não introduzir `CompositionLocalProvider` para `LocalConfiguration`.

- [ ] **Step 2: Ajustar barra de status no mesmo valor**

Dentro de `SideEffect`, com `(LocalContext.current as? Activity)?.window`: definir `statusBarColor` com `androidx.compose.ui.graphics.Color(0xFFFFFBE8).toArgb()` quando claro e `Color(0xFF1C1B1F).toArgb()` quando escuro; `WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = !escuro`.

- [ ] **Step 3: Verificar build**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain`
Expected: BUILD SUCCESSFUL, 60+ testes unitários verdes.
Commit: `git commit -m "feat: aplica tema escolhido e barra de status"`

### Task 3: Seletor em Perfil

**Files:**
- Create: `app/src/main/java/com/vizinhoalerta/ui/theme/SeletorTema.kt`
- Test: `app/src/androidTest/java/com/vizinhoalerta/ui/theme/SeletorTemaTest.kt`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt`

**Interfaces:**
- Consumes: `PreferenciaTema` (Task 1).
- Produces: `@Composable fun SeletorTema(preferencia: PreferenciaTema, onSelecionar: (PreferenciaTema) -> Unit)`; `PerfilScreen(..., preferenciaTema: PreferenciaTema, onPreferenciaTemaChange: (PreferenciaTema) -> Unit)`.

- [ ] **Step 1: Escrever teste instrumentado falhante**

`SeletorTemaTest.kt`: `seletor_mostra_tres_opcoes_e_confirma` — com `PreferenciaTema.SISTEMA`, clicar em "Tema", assertar textos "Seguir o sistema", "Claro", "Escuro"; clicar em "Escuro"; assertar callback recebeu `ESCURO` e nenhum texto de opção permanece. Usar `createComposeRule()`.

- [ ] **Step 2: Rodar e confirmar falha**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vizinhoalerta.ui.theme.SeletorTemaTest --console=plain`
Expected: FAIL (composable não existe).

- [ ] **Step 3: Implementar `SeletorTema`**

`OutlinedButton` com texto `Tema` (e valor atual entre parênteses) abrindo `AlertDialog` com três `TextButton` alinhados por rótulo: "Seguir o sistema", "Claro", "Escuro". Cada um chama `onSelecionar` e fecha o diálogo.

- [ ] **Step 4: Integrar em PerfilScreen**

Adicionar dois parâmetros à assinatura e renderizar `SeletorTema(preferenciaTema, onPreferenciaTemaChange)` depois de "Editar dados" e antes de "Painel Admin"/"Sair". Nenhuma chamada de navegação.

- [ ] **Step 5: Rodar teste, build e commitar**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vizinhoalerta.ui.theme.SeletorTemaTest --console=plain`
Expected: BUILD SUCCESSFUL (1 teste).
Commit: `git commit -m "feat: adiciona opcao Tema no Perfil"`

### Task 4: Ligação no MainActivity e aceite

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/MainActivity.kt`

**Interfaces:**
- Consumes: `RepositorioTema` (Task 1), callback de `PerfilScreen` (Task 3), `VizinhoAlertaTheme` (Task 2).
- Produces: estado de preferência compartilhado entre os dois `setContent` e a navegação.

- [ ] **Step 1: State inicial**

Em `MainActivity`, campo `private var preferenciaTema = mutableStateOf(PreferenciaTema.SISTEMA)`; em `onCreate`, antes do primeiro `setContent`, atribuir `RepositorioTema.ler(this)`. Passar `preferencia = preferenciaTema.value` nos dois `VizinhoAlertaTheme`.

- [ ] **Step 2: Propagar callback**

Adicionar parâmetros `preferenciaTema` e `onPreferenciaTemaChange` a `VizinhoAlertaApp`, passá-los ao `PerfilScreen`; no callback, `RepositorioTema.salvar(context, it)` e atualizar o estado antes de recompor. Nenhuma navegação ou `recreate()`.

- [ ] **Step 3: Verificar suite e build**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Aceite no aparelho (USB)**

Instalar APK; em Perfil → Tema: escolher Escuro com Android em claro (fundo escuro, ícones claros da barra, permanece em Perfil); escolher Claro (ícones escuros); fechar e reabrir — escolha preservada; escolher Seguir o sistema e alternar o tema do Android — app acompanha.

- [ ] **Step 5: Commit**

`git commit -m "feat: persiste e aplica preferencia de tema"`

## Review Focus — testes por item

- `temaEfetivo` com sistema claro/escuro e overrides → Task 1, Step 1.
- Preferência ausente/inválida → Task 1, Step 5.
- Contraste da barra e permanência na tela → Task 4, Step 4.
- Persistência ao reabrir → Task 1, Step 5 + Task 4, Step 4.
