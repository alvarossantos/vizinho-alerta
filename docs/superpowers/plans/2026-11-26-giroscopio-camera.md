# Indicador de movimento na câmera (giroscópio) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fazer o app ler `TYPE_GYROSCOPE` na prévia da câmera de Nova Ocorrência e exibir "Segure o celular firme" enquanto houver rotação, mantendo a captura sempre disponível.

**Architecture:** `EstadoMovimento` é uma classe pura (sem Android) que decide o aviso a partir de amostras dos três eixos e de um relógio monotonic, com histerese de 500 ms. `GiroscopioService` é o wrapper Android: seleciona o sensor, registra `SensorEventListener`, observationa o `Lifecycle` e devolve um dispose. `ReportarScreen` usa `DisposableEffect` para ativar/desativar a observação e desenha o texto sobre a prévia CameraX existente.

**Tech Stack:** Kotlin, Jetpack Compose, CameraX, `SensorManager`/`SensorEventListener`, JUnit 4, Compose UI Test, Gradle 8.7 / AGP 8.5.1.

**Spec:** `docs/superpowers/specs/2026-11-26-giroscopio-camera-design.md`

## Global Constraints

- `minSdk = 24`, `targetSdk = 34`, `compileSdk = 34`.
- Sensores não exigem permissão; `CAMERA`/`RECORD_AUDIO`/GPS já são pedidos em `MainActivity.kt`.
- Amostragem: `SensorManager.SENSOR_DELAY_UI`.
- Limiar: `0,15 rad/s` em qualquer eixo (`abs(x) >= 0,15f || abs(y) >= ... || abs(z) >= ...`).
- Histerese: esconder após `500 ms` contínuos abaixo do limiar; novo giro reinicia a contagem.
- Aviso é informativo: botão de captura permanece habilitado sempre.
- Sem `TYPE_GYROSCOPE`: nenhum aviso, prévia e captura inalteradas.
- TAG de Logcat: `GiroscopioService`.
- Toda mudança entra por Issue + branch + PR; nada de push direto na `main`.
- Texto exato do overlay: `Segure o celular firme`.

## Review Focus

- Sensor ausente (`getDefaultSensor` retorna `null`): deve seguir sem aviso e sem exceção — Task 3, passo 3 (verificação de código) e Task 5, passo 4 (conferência em aparelho sem sensor, se disponível).
- Amostras oscilando perto de `0,15 rad/s`: histerese impede aviso piscando — Task 2, passo 1, teste `giro antes de 500ms reinicia intervalo`.
- Saída de `STARTED` ou fechamento da prévia com aviso ligado: listener removido e estado limpo — Task 2, passo 1, teste `reiniciar limpa estado`; Task 5, passo 2 (dispose).
- `SensorEvent.values` é reutilizado pelo sistema: não guardar a referência — Task 2, passo 3 (cópia dos componentes em `atualizar`).
- Captura durante o aviso: nenhuma alteração em `CameraService.tirarFoto` ou no FAB — Task 4, passo 4 (inspeção) e Task 5, passo 4 (foto nos dois estados).

---

### Task 1: Restaurar toolchain Android e abrir Issue/branch

**Files:**
- Modificar: `local.properties` (somente se o caminho do SDK mudar; pretende-se manter `sdk.dir=/home/alvaro/.local/share/android-sdk`)
- GitHub: nova Issue; git: nova branch a partir de `main`

**Interfaces:**
- Consumes: repo em `/home/alvaro/vizinho-alerta`, `main` @ `7ab5980`.
- Produces: branch `feature/<numero>-giroscopio-camera`, JDK 17 via `JAVA_HOME`, SDK com `platforms;android-34` e `build-tools;34.0.0`, `adb` funcional.

- [ ] **Step 1: Abrir a Issue**

```bash
cd /home/alvaro/vizinho-alerta
gh issue create --title "[feature] indicador de movimento na camera com giroscopio" \
  --body "Ler TYPE_GYROSCOPE na previa de Nova Ocorrencia e exibir 'Segure o celular firme' ao girar. Aviso informativo; captura permanece disponivel; sem sensor, fluxo inalterado. Cobre o quinto recurso do enunciado." \
  --label enhancement
```
Anotar o número retornado.

- [ ] **Step 2: Criar branch a partir da main atualizada**

```bash
git fetch origin && git switch main && git merge --ff-only origin/main
git switch -c feature/<numero>-giroscopio-camera
```
Expected: `git branch --show-current` retorna o novo nome.

- [ ] **Step 3: Instalar toolchain (REQUER APROVAÇÃO EXPLÍCITA DE SUDO)**

Propor ao usuário e só executar após aprovação:
```bash
sudo pacman -Syu android-tools jdk17-openjdk
yay -S android-sdk
```
Referência consultada: https://wiki.archlinux.org/title/Android (SDK e JDK no Arch). AGP 8.5.1 exige JDK 17; `JAVA_HOME` já aponta para `/usr/lib/jvm/java-17-openjdk`.

- [ ] **Step 4: Confirmar caminho do sdkmanager antes de usar (readback)**

```bash
ls -l /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager
```
Se o caminho diferir, parar e usar o caminho real retornado.

- [ ] **Step 5: Instalar platform/build-tools no SDK esperado pelo `local.properties`**

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
yes | /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/home/alvaro/.local/share/android-sdk --licenses
/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root=/home/alvaro/.local/share/android-sdk "platforms;android-34" "build-tools;34.0.0" "platform-tools"
```

- [ ] **Step 6: Verificar baseline e dispositivo**

```bash
./gradlew --version | grep -i 'JVM'
adb version | head -1
adb devices -l
./gradlew :app:testDebugUnitTest
```
Expected: JVM 17, `adb` presente, suíte unitária verde antes de qualquer mudança. Se `adb devices` não listar dispositivo, registrar que a validação física ficará pendente.

- [ ] **Step 7: Commit do documento de desenho e plano nesta branch**

```bash
git add docs/superpowers/specs/2026-11-26-giroscopio-camera-design.md docs/superpowers/plans/2026-11-26-giroscopio-camera.md
git commit -m "docs: desenho e plano do indicador de giroscopio na camera"
```

---

### Task 2: Regra pura de detecção e histerese

**Files:**
- Create: `app/src/main/java/com/vizinhoalerta/service/EstadoMovimento.kt`
- Test: `app/src/test/java/com/vizinhoalerta/service/EstadoMovimentoTest.kt`

**Interfaces:**
- Consumes: nada.
- Produces: `class EstadoMovimento` com `fun atualizar(valores: FloatArray, agoraMs: Long): Boolean` (retorna `true` quando o aviso deve estar visível) e `fun reiniciar()`; companion com `const val LIMIAR_RAD_S = 0.15f` e `const val INTERVALO_ESTABILIDADE_MS = 500L`.

- [ ] **Step 1: Escrever os testes que falham**

Em `EstadoMovimentoTest.kt`, com `org.junit.Assert.assertTrue/assertFalse` e `@Test`, nomes e valores exatos:
- `acima do limiar em qualquer eixo liga aviso`: `atualizar(floatArrayOf(0f, 0.2f, 0f), 1000)` → `true`.
- `limiar exato conta como movimento`: `atualizar(floatArrayOf(0.15f, 0f, 0f), 1000)` → `true`.
- `abaixo do limiar antes de 500ms mantem aviso`: `1000` com `0.2f` → `true`; `1400` com zeros → `true`.
- `500ms contiguos abaixo do limiar escondem aviso`: `1000` com `0.2f` → `true`; `1500` com zeros → `false`.
- `giro antes de 500ms reinicia intervalo`: `1000` com `0.2f` → `true`; `1300` zeros → `true`; `1400` com `0.2f` → `true`; `1899` zeros → `true`; `1900` zeros → `false`.
- `reiniciar limpa estado`: `1000` com `0.2f` → `true`; `reiniciar()`; `5000` zeros → `false`.

- [ ] **Step 2: Rodar e confirmar que falha**

Run: `./gradlew :app:testDebugUnitTest --tests '*EstadoMovimentoTest*'`
Expected: FAIL (classe inexistente).

- [ ] **Step 3: Implementar `EstadoMovimento`**

Sem depender de classes Android. Guardar apenas `ultimoMovimentoMs: Long?` e `avisoVisivel: Boolean`; em `atualizar`, ler os três componentes de `valores` imediatamente (não reter o `FloatArray`), ligar o aviso se qualquer `abs(eixo) >= LIMIAR_RAD_S`, e desligar quando `avisoVisivel && agoraMs - ultimoMovimentoMs >= INTERVALO_ESTABILIDADE_MS`. `reiniciar()` zera os dois campos.

- [ ] **Step 4: Rodar testes e confirmar verde**

Run: `./gradlew :app:testDebugUnitTest --tests '*EstadoMovimentoTest*'`
Expected: PASS (6 testes).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/service/EstadoMovimento.kt app/src/test/java/com/vizinhoalerta/service/EstadoMovimentoTest.kt
git commit -m "test: histerese do indicador de movimento com giroscopio"
```

---

### Task 3: Wrapper Android `GiroscopioService`

**Files:**
- Create: `app/src/main/java/com/vizinhoalerta/service/GiroscopioService.kt`

**Interfaces:**
- Consumes: `EstadoMovimento.atualizar(valores, agoraMs)` e `EstadoMovimento.reiniciar()` da Task 2; constantes `LIMIAR_RAD_S` e `INTERVALO_ESTABILIDADE_MS`.
- Produces: `object GiroscopioService { const val TAG = "GiroscopioService"; fun observar(context: Context, lifecycle: Lifecycle, onMovimento: (Boolean) -> Unit): () -> Unit }`. O lambda retornado remove observer, desregistra listener e reinicia o estado; pode ser chamado mais de uma vez.

- [ ] **Step 1: Escrever o esqueleto com assinatura e constantes exatas**

Criar o arquivo com `TAG`, imports de `android.content.Context`, `android.hardware.Sensor`, `SensorEventListener`, `SensorManager`, `android.os.SystemClock`, `android.util.Log`, `androidx.lifecycle.Lifecycle`, `Lifecycle.Event`, `LifecycleEventObserver`.

- [ ] **Step 2: Rodar compilação para verificar assinatura**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL com o object vazio além da assinatura (ainda sem corpo).

- [ ] **Step 3: Implementar `observar`**

Obter `SensorManager` via `context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager` e `getDefaultSensor(Sensor.TYPE_GYROSCOPE)`; se faltar, logar `Log.d(TAG, "giroscopio indisponivel")` e retornar dispose no-op (caminho do Review Focus 1). Instanciar `EstadoMovimento`; no `onSensorChanged`, chamar `atualizar(event.values, SystemClock.elapsedRealtime())` e repassar o `Boolean` a `onMovimento`. Registrar com `SENSOR_DELAY_UI` somente se `lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)`; adicionar `LifecycleEventObserver` que registra em `ON_START` e remove em `ON_STOP`. O dispose remove o observer, desregistra e chama `estado.reiniciar()`. Logar em `D` a disponibilidade e cada transição do aviso.

- [ ] **Step 4: Verificar compilação e ausência de permissão nova**

```bash
./gradlew :app:compileDebugKotlin
git diff -- app/src/main/AndroidManifest.xml
```
Expected: BUILD SUCCESSFUL; `git diff` do manifest vazio.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/service/GiroscopioService.kt
git commit -m "feat: servico de observacao do giroscopio com lifecycle"
```

---

### Task 4: Overlay informativo na prévia com teste instrumentado

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/ui/reportar/ReportarScreen.kt` (adicionar `@Composable internal fun AvisoMovimento(movimentoDetectado: Boolean, modifier: Modifier = Modifier)` e o `Text` com `Segure o celular firme`)
- Test: `app/src/androidTest/java/com/vizinhoalerta/ui/reportar/AvisoMovimentoTest.kt`

**Interfaces:**
- Consumes: nada da Task 3 além do texto exato do spec.
- Produces: `AvisoMovimento(Boolean, Modifier)`; `ReportarScreen` passa a chamar esse composable dentro do `Box` da prévia (`ReportarScreen.kt:206-248`).

- [ ] **Step 1: Escrever teste instrumentado que falha**

```kotlin
@get:Rule val composeTestRule = createComposeRule()

@Test fun aviso_aparece_quando_há_movimento() {
    composeTestRule.setContent { AvisoMovimento(true) }
    composeTestRule.onNodeWithText("Segure o celular firme").assertIsDisplayed()
}

@Test fun aviso_desaparece_quando_estável() {
    composeTestRule.setContent { AvisoMovimento(false) }
    composeTestRule.onNodeWithText("Segure o celular firme").assertDoesNotExist()
}
```

- [ ] **Step 2: Rodar e confirmar que falha**

Run: `./gradlew :app:connectedDebugAndroidTest --tests '*AvisoMovimentoTest*'`
Expected: FAIL (referência inexistente). Se não houver dispositivo, registrar como pendente e prosseguir com verificação de compilação.

- [ ] **Step 3: Implementar `AvisoMovimento`**

`if (!movimentoDetectado) return`; `Text` com o texto exato, `Modifier.align(Alignment.TopCenter).background(...).padding(...)` e cor de contraste. Não envolver em nenhum `Button` nem alterar o FAB.

- [ ] **Step 4: Inserir o overlay no `Box` da prévia**

Chamar `AvisoMovimento(movimentoDetectado)` dentro do `Box` de `ReportarScreen.kt:206-248`, sem tocar em `imageCapture`, `FloatingActionButton` ou `CameraService.tirarFoto` (Review Focus 5).

- [ ] **Step 5: Rodar teste instrumentado e unitário**

```bash
./gradlew :app:connectedDebugAndroidTest --tests '*AvisoMovimentoTest*'
./gradlew :app:testDebugUnitTest
```
Expected: 2 testes instrumentados e suíte unitária verde.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/ui/reportar/ReportarScreen.kt app/src/androidTest/java/com/vizinhoalerta/ui/reportar/AvisoMovimentoTest.kt
git commit -m "feat: aviso de movimento sobre a previa da camera"
```

---

### Task 5: Ligar ciclo de vida, validar no aparelho e fechar PR

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/ui/reportar/ReportarScreen.kt`

**Interfaces:**
- Consumes: `GiroscopioService.observar(context, lifecycleOwner.lifecycle) { movimentoDetectado = it }` da Task 3; `AvisoMovimento` da Task 4.
- Produces: prévia com aviso ativo somente enquanto `mostrandoCamera` e `STARTED`.

- [ ] **Step 1: Adicionar estado e `DisposableEffect`**

`var movimentoDetectado by remember { mutableStateOf(false) }`. Com chaves `(mostrandoCamera, lifecycleOwner)`, registrar quando `mostrandoCamera`; `onDispose` chama o dispose retornado e define `movimentoDetectado = false` (Review Focus 3).

- [ ] **Step 2: Verificar que o dispose cobre fechar prévia e sair de `STARTED`**

Inspeção do código + execução do app: abrir câmera, girar, sair da tela com `back`, voltar e confirmar que o aviso não reaparece sem novo movimento.

- [ ] **Step 3: Rodar verificação completa**

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug
git diff --check
```
Expected: verde em tudo; `git diff --check` sem saída.

- [ ] **Step 4: Aceite em aparelho físico**

Com dispositivo conectado:
```bash
adb logcat -c
adb logcat -s GiroscopioService:D
```
Abrir **Nova Ocorrência → Câmera**, girar o aparelho (aviso aparece), mantê-lo parado por ~1 s (aviso some), tirar foto com aviso visível e com aviso oculto. Repetir em aparelho sem giroscópio, se disponível. Registrar no PR o modelo do aparelho e o resultado; se não houver aparelho, declarar explicitamente que a validação ficou estática/instrumentada. Ajustar o limiar `0,15f` somente se o aviso não responder a um giro manual moderado, alterando a constante e o teste juntos.

- [ ] **Step 5: Commit final e PR**

```bash
git add -A app/src/main/java/com/vizinhoalerta/ui/reportar/ReportarScreen.kt
git commit -m "feat: observa giroscopio apenas com a previa visivel"
git push -u origin feature/<numero>-giroscopio-camera
gh pr create --title "Indicador de movimento na câmera com giroscópio" --body "Closes #<numero>" --base main
```
Expected: PR criado com `Closes #<numero>`; merge apenas após revisão, como no PR #18.
