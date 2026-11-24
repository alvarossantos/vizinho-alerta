# Ícone padrão de usuário no Avatar — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir a inicial do nome por uma silhueta de usuário branca, desenhada no tema do app, em todo avatar sem foto.

**Architecture:** Um vector drawable próprio (`ic_usuario_padrao.xml`) fornece apenas a silhueta; o círculo de fundo continua sendo aplicado pelo `Box` do `Avatar` via `MaterialTheme.colorScheme.primary`, então o light/dark scheme é preservado sem duplicar cor. O `Avatar` é o único ponto de mudança de código e já é consumido por Perfil, Detalhe, Lista e Admin.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Android vector drawable, Coil (já existente), AVD `emulator-5554` para verificação visual.

**Spec:** `docs/superpowers/specs/2026-11-23-icone-usuario-padrao-design.md`

## Global Constraints

- Nenhuma dependência Gradle nova.
- Nome é obrigatório no cadastro — não existe avatar sem nome; a silhueta substitui a letra em **todo** caso sem `fotoUrl`.
- Fundo do avatar: `MaterialTheme.colorScheme.primary` ( `#F97316` no light ). Silhueta: branco literal `#FFFFFF` (igual a `onPrimary` nos dois esquemas).
- Não alterar: `AsyncImage`, selo de câmera (`editavel`), tamanhos atuais (28–40 dp), `Usuario`, `AuthRepository`, cadastro, Storage, bottom nav, autoria, guard de release.
- Verificação visual obrigatória no AVD antes do PR.

## Review Focus

- **Avatar com foto:** a silhueta nunca pode aparecer sobre a foto — coberto pelo Step 4 (AVD, Perfil com foto).
- **Perfil editável:** silhueta + selo de câmera sobrepostos, sem perder o clique — coberto pelo Step 4 (AVD, Perfil).
- **Tamanhos pequenos (28–32 dp):** silhueta legível em Detalhe e Lista — coberto pelo Step 4 (AVD, Detalhe e Lista).
- **Admin (40 dp):** silhueta correta para admin e morador — coberto pelo Step 4 (AVD, aba Usuários).
- **Dark mode:** fundo acompanha o tema sem círculo duplicado — coberto pelo Step 3 (revisão do diff: drawable só com silhueta, `Box` com o background).

---

### Task 0: Issue e branch (preflight)

- [ ] **Step 1: Criar Issue `[enhancement] Ícone padrão de usuário no avatar`** com label `enhancement` via `gh issue create`, registrando o número retornado.
- [ ] **Step 2: Criar a branch** `enhancement/N-icone-usuario-padrao` a partir de `main` (N = número da Issue), com `git switch -c`.

---

### Task 1: Silhueta padrão no Avatar

**Files:**
- Create: `app/src/main/res/drawable/ic_usuario_padrao.xml`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/components/Avatar.kt:51-64`
- Test: `./gradlew testDebugUnitTest assembleDebug --no-daemon` + verificação visual no AVD

**Interfaces:**
- Consumes: `Avatar(nome, fotoUrl, tamanho, editavel, onEditarClick)` existente; tokens de `ui.theme` (`Laranja`, `#FFFFFF`).
- Produces: nenhuma assinatura pública muda. `R.drawable.ic_usuario_padrao` passa a existir.

- [ ] **Step 1: Criar o drawable vetorial**

Crie `app/src/main/res/drawable/ic_usuario_padrao.xml` com viewport 24×24, **sem fundo circular** (o `Box` já aplica o círculo), dois paths em `#FFFFFFFF`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M12,5.4a3.6,3.6 0 1,1 0,7.2a3.6,3.6 0 1,1 0,-7.2Z" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M5.3,20.3c0,-3.7 3,-6.1 6.7,-6.1s6.7,2.4 6.7,6.1v0.7H5.3Z" />
</vector>
```

Cabeça centrada em (12, 9.0) com raio 3.6; ombros de x=5.3 a x=18.7. Proporções próprias — não copiar o path do `Icons.Filled.Person`.

- [ ] **Step 2: Verificar que o recurso compila**

Run: `./gradlew assembleDebug --no-daemon`
Expected: `BUILD SUCCESSFUL` (valida o XML do vetor via `aapt2`).

- [ ] **Step 3: Trocar a inicial pela silhueta no fallback do `Avatar`**

Em `Avatar.kt`, no ramo `else` (onde hoje está a `Text` da inicial), substitua o `Text` por:

```kotlin
Icon(
    painter = painterResource(R.drawable.ic_usuario_padrao),
    contentDescription = "Sem foto de perfil",
    tint = Color.Unspecified,
    modifier = Modifier.fillMaxSize(0.62f)
)
```

Adicione os imports `androidx.compose.ui.res.painterResource`, `com.vizinhoalerta.R` e mantenha `androidx.compose.ui.graphics.Color`. Remova `androidx.compose.material3.Text`, `androidx.compose.ui.text.font.FontWeight` e `androidx.compose.ui.unit.sp` se ficarem sem uso. Mantenha intactos: o `Box` com `background(MaterialTheme.colorScheme.primary, CircleShape)`, o `AsyncImage`, o `inicial` (pode ficar calculado, sem uso) e o bloco `editavel`.

- [ ] **Step 4: Rodar suíte e build**

Run: `./gradlew testDebugUnitTest assembleDebug --no-daemon`
Expected: `BUILD SUCCESSFUL`, 44 testes / 0 falhas (baseline do PR #4).

- [ ] **Step 5: Verificar no AVD**

Reinstale o APK e confera, com login `admin@demo.com` / `Demo@123`:
1. Perfil: avatar "A" virou silhueta; "Editar foto" com câmera ainda clicável.
2. Alertas (Lista): silhueta nos 5 cards.
3. Detalhes: silhueta no header do autor.
4. Painel Admin → Usuários: silhueta em Admin Demo e Morador Demo.
5. Com uma foto salva via "Editar foto", a foto substitui a silhueta no Perfil.

Registre o resultado em `.superpowers/sdd/2026-11-23-icone-usuario-padrao/progress.md`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/drawable/ic_usuario_padrao.xml \
        app/src/main/java/com/vizinhoalerta/ui/components/Avatar.kt
git commit -m "feat: silhueta padrao no avatar sem foto"
```

Não comite `build/reports/problems/problems-report.html`, `scripts/__pycache__/`, planos/specs antigos nem artefatos do ledger.

### Task 2: Verificação final e PR

**Files:**
- Modify: `.superpowers/sdd/2026-11-23-icone-usuario-padrao/progress.md`

**Interfaces:**
- Consumes: commit da Task 1.
- Produces: PR aberto, não mesclado.

- [ ] **Step 1: Conferir diff e testes**

```bash
git diff --check origin/main..HEAD
python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
t=f=e=0
for p in glob.glob('app/build/test-results/testDebugUnitTest/*.xml'):
    r=ET.parse(p).getroot(); t+=int(r.get('tests',0)); f+=int(r.get('failures',0)); e+=int(r.get('errors',0))
print(f'tests={t} failures={f} errors={e}')
PY
```

Expected: diff sem whitespace errors; `tests=44 failures=0 errors=0`.

- [ ] **Step 2: Criar Issue, branch e PR**

Criar Issue `[enhancement] Ícone padrão de usuário no avatar` com label `enhancement`. Já estamos em branch própria; comitar o spec e o plano aprovados nela. Push e `gh pr create` com `Closes #N`, sem mesclar.

- [ ] **Step 3: Revisão do branch**

Gerar pacote com `scripts/review-package` da skill `subagent-driven-development` e despachar reviewer. Corrigir findings Important em uma fix wave antes de publicar o PR.
