# Linha demo abaixo do Google no login — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mover a linha `Demo: admin@demo.com / Demo@123` no login para abaixo do link `Não tem conta? Cadastrar`, igualando a distância entre os botões à do cadastro.

**Architecture:** As duas telas usam `Column` com `Arrangement.spacedBy(12.dp)`. Hoje o login insere `if (modoDemo) { Spacer(8.dp); Text(...) }` entre `PrimaryButton("ENTRAR")` e `GoogleSignInButton`. A correção desloca esse bloco para depois do `TextButton("Não tem conta? Cadastrar")`; o cadastro permanece intacto. Um teste instrumentado compara as posições reais dos nós na hierarquia Compose.

**Tech Stack:** Kotlin, Jetpack Compose, Compose UI Test, JUnit4, Gradle com wrapper fora do worktree.

**Spec:** `docs/superpowers/specs/2026-11-24-login-demo-abaixo-google-design.md`

## Global Constraints

- Modificar **apenas** `app/src/main/java/com/vizinhoalerta/ui/login/LoginScreen.kt` (código) e `app/src/androidTest/java/com/vizinhoalerta/ui/LoginDemoLayoutTest.kt` (teste), além de docs/plano.
- **Não** alterar `CadastroScreen.kt`, `GoogleSignInButton.kt`, `MainActivity.kt`, constantes, navegação ou dependências.
- Texto exato preservado: `Demo: admin@demo.com / Demo@123`, `fontSize = 12.sp`, guarda `if (modoDemo)`.
- `Spacer(Modifier.height(8.dp))` do bloco demo preservado dentro do bloco movido.
- Um commit por task; PR #18 permanece `OPEN`; **sem merge sem aprovação explícita**.
- Gradle: `sh /home/alvaro/vizinho-alerta/gradlew -p /home/alvaro/.local/share/opencode/worktree/12a924/feature/17-google-signin <tarefas> --no-daemon`.
- Emulador: `/home/alvaro/.local/share/android-sdk/emulator/emulator -avd test_emulator -no-window -no-audio -no-snapshot -gpu swiftshader_indirect -port 5554`. Aguardar `sys.boot_completed=1` antes de `connectedDebugAndroidTest`.

## Review Focus

- `modoDemo = false`: linha demo não deve existir — ⚠️ o teste instrumentado roda no build demo; coberto pela guarda `if (modoDemo)` preservada, sem teste dedicado.
- Cadastro inalterado: a distância `Criar minha conta → Continuar com Google` serve de referência no teste.
- Texto exato da linha demo: o teste localiza o nó por `onNodeWithText` com a string literal.
- Distância entre botões no login: o teste compara `googleLoginTop - entrarBottom` com `googleCadastroTop - criarBottom`.
- Ordem no login: o teste exige `demoTop > googleLoginTop`.

---

### Task 1: Teste instrumentado de layout (RED)

**Files:**
- Create: `app/src/androidTest/java/com/vizinhoalerta/ui/LoginDemoLayoutTest.kt`
- Commit também: `docs/superpowers/specs/2026-11-24-login-demo-abaixo-google-design.md`, `docs/superpowers/plans/2026-11-24-login-demo-abaixo-google.md`

**Interfaces:**
- Consumes: `MainActivity` (destino inicial `welcome` no demo), rótulos existentes `Entrar`, `Cadastrar-se`, `ENTRAR`, `Criar minha conta`, `Continuar com Google`, `Demo: admin@demo.com / Demo@123`.
- Produces: teste `linhaDemoAbaixoDoGoogle_eDistanciaIgualAoCadastro`.

- [ ] **Step 1: Escrever o teste**

```kotlin
package com.vizinhoalerta.ui

import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vizinhoalerta.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginDemoLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun linhaDemoAbaixoDoGoogle_eDistanciaIgualAoCadastro() {
        composeRule.onNodeWithText("Entrar").performClick()
        composeRule.waitForIdle()

        val entrarBottom = composeRule.onNodeWithText("ENTRAR").getBoundsInRoot().bottom
        val googleLoginTop = composeRule.onNodeWithText("Continuar com Google").getBoundsInRoot().top
        val demoTop = composeRule.onNodeWithText("Demo: admin@demo.com / Demo@123").getBoundsInRoot().top

        assert(demoTop > googleLoginTop) {
            "linha demo deve ficar abaixo do botao Continuar com Google"
        }

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Cadastrar-se").performClick()
        composeRule.waitForIdle()

        val criarBottom = composeRule.onNodeWithText("Criar minha conta").getBoundsInRoot().bottom
        val googleCadastroTop = composeRule.onNodeWithText("Continuar com Google").getBoundsInRoot().top

        val gapLogin = googleLoginTop - entrarBottom
        val gapCadastro = googleCadastroTop - criarBottom
        assert(gapLogin == gapCadastro) {
            "distancia ENTRAR->Google no login ($gapLogin) deve igualar Criar minha conta->Google no cadastro ($gapCadastro)"
        }
    }
}
```

- [ ] **Step 2: Rodar e confirmar RED**

Iniciar o emulador (comando em Global Constraints), aguardar boot, então:

```bash
sh /home/alvaro/vizinho-alerta/gradlew -p /home/alvaro/.local/share/opencode/worktree/12a924/feature/17-google-signin connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vizinhoalerta.ui.LoginDemoLayoutTest --no-daemon
```

Expected: **FAIL** na asserção `linha demo deve ficar abaixo do botao Continuar com Google` (hoje a linha fica acima). Se falhar por outro motivo (nó não encontrado, navegação), corrigir o teste antes de prosseguir — não é RED válido.

- [ ] **Step 3: Commit do teste e dos docs**

```bash
git add app/src/androidTest/java/com/vizinhoalerta/ui/LoginDemoLayoutTest.kt docs/superpowers/specs/2026-11-24-login-demo-abaixo-google-design.md docs/superpowers/plans/2026-11-24-login-demo-abaixo-google.md
git commit -m "test: posicao da linha demo no login"
```

---

### Task 2: Mover o bloco demo (GREEN)

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/ui/login/LoginScreen.kt:113-126`

**Interfaces:**
- Consumes: teste da Task 1.
- Produces: nenhuma interface nova.

- [ ] **Step 1: Mover o bloco**

Remover de entre `PrimaryButton("ENTRAR")` e `GoogleSignInButton`:

```kotlin
if (modoDemo) {
    Spacer(Modifier.height(8.dp))
    Text("Demo: admin@demo.com / Demo@123", fontSize = 12.sp)
}
```

Reinserir o mesmo bloco **logo após** `TextButton(onClick = onCadastrarClick) { Text("Não tem conta? Cadastrar") }`, antes do fechamento da `Column`. Nenhuma outra linha muda.

- [ ] **Step 2: Rodar o teste e confirmar GREEN**

```bash
sh /home/alvaro/vizinho-alerta/gradlew -p /home/alvaro/.local/share/opencode/worktree/12a924/feature/17-google-signin connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vizinhoalerta.ui.LoginDemoLayoutTest --no-daemon
```

Expected: **PASS** (1 teste, 0 falhas).

- [ ] **Step 3: Verificação unitária e de diff**

```bash
sh /home/alvaro/vizinho-alerta/gradlew -p /home/alvaro/.local/share/opencode/worktree/12a924/feature/17-google-signin assembleDebug testDebugUnitTest --no-daemon
git diff --check
```

Expected: BUILD SUCCESSFUL, 54 testes verdes, `git diff --check` limpo.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/ui/login/LoginScreen.kt
git commit -m "fix: linha demo abaixo do link de cadastro no login"
```

- [ ] **Step 5: Atualizar PR #18**

Fazer push da branch, recapturar `docs/screenshots/11-login-google.png` no emulador (welcome → `Entrar` → `screencap`), substituir o arquivo, commitar `docs: atualiza captura do login` e fazer push novamente. Atualizar o corpo do PR apenas se necessário. **Sem merge.**

## Self-Review

- Spec coverage: posição da linha (Task 2), distância entre botões (Task 1), cadastro intacto (Task 1 compara com cadastro), texto/guarda preservados (Task 2 reinsere o bloco literal).
- Proporção: plano curto, sem transcrição de código além do teste que fixa as asserções.
- ⚠️ Fora de cobertura: `modoDemo = false` e aparência em outros tamanhos de tela — nenhum teste dedicated, conforme Review Focus.
