# UI em nível de referência + foto de perfil Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aproximar o app dos mockups (tema laranja/marinho, pills, chips, badges, bottom nav) e permitir que o vizinho tenha foto de perfil visível em perfil, detalhe, lista e admin, sem quebrar demo/autoria/release.

**Architecture:** Tema centralizado em `ui/theme` + componentes pequenos em `ui/components`; `Usuario.fotoUrl` é a única fonte da foto, preenchida pelo `AuthRepository` (demo: arquivo local + SharedPreferences; produção: Firebase Storage + Firestore/Auth). Busca/filtro do mapa ficam em estado hoisted no `NavHost` e alimentam mapa e lista.

**Tech Stack:** Kotlin, Jetpack Compose + Material3, Coil 2.6.0 (já instalado), Navigation Compose, Firebase Auth/Firestore/Storage, MapLibre (inalterado), JUnit4.

**Spec:** `docs/superpowers/specs/2026-11-23-ui-avatar-perfil-design.md`

## Global Constraints

- Branch nova: `feature/2-ui-avatar-perfil`, criada a partir de `origin/main` (`b7bd616`). Repo: `/home/alvaro/vizinho-alerta`.
- Nenhuma dependência nova no Gradle. Coil, FileProvider (`androidx.core`) e CameraX já estão disponíveis.
- Não alterar `exigirPermissaoDemo`, `atualizarLocalizacao`, `resolver` nem o guard `checkReleaseGoogleServices`.
- Release com `google-services.json` DUMMY deve continuar falhando em `checkReleaseGoogleServices`.
- Textos de login/cadastro usados nos testes AVD permanecem: "ENTRAR", "Demo: admin@demo.com / Demo@123", validações atuais de senha.
- Não comitar `build/reports/problems/problems-report.html`, `docs/superpowers/plans/2026-11-23-login-cadastro-design.md` nem `scripts/__pycache__/`.
- Todo PR: Issue com label `enhancement`, `Closes #N`, sem merge sem aprovação explícita.

## Review Focus

1. **Usuário sem foto** — `Avatar` deve mostrar inicial do nome sem crash; teste: AVD Task 3 (perfil/admin sem foto) + revisão do composable.
2. **Imagem inválida ou > 2 MB** — deve ser rejeitada antes de upload; teste: `FotoPerfilTest` em Task 2 (`validarImagemPerfil`).
3. **Demo sem uid ou sem Context** — `salvarFotoPerfil` deve falhar sem gravar arquivo; teste: `FotoPerfilTest` em Task 2 (`mesclarFotoDemo`/fail-closed) + revisão do ramo demo.
4. **Filtro sem resultado** — mapa e lista devem mostrar estado vazio, não tela em branco; teste: `FiltroOcorrenciasTest` em Task 4 + AVD.
5. **Câmera negada/ausente** — galeria continua funcionando e erro aparece em pt-BR; teste: AVD Task 2 (galeria) + revisão dos launchers.

## Preflight (controller, antes da Task 1)

```bash
cd /home/alvaro/vizinho-alerta
git fetch origin main
git checkout -b feature/2-ui-avatar-perfil origin/main
gh issue create --title '[enhancement] UI em nível de referência e foto de perfil do vizinho' \
  --label enhancement \
  --body 'Alinhar a interface ao padrão visual das referências (laranja/marinho, pills, chips, badges, bottom nav) e permitir foto de perfil do vizinho, exibida em perfil, detalhe da ocorrência, lista e painel admin. Fora de escopo: comentários, carrossel, conquistas, pins customizados.'
```

Anotar o número da Issue para o PR final. Design e plano desta pasta entram no primeiro commit da Task 1.

---

### Task 1: Tema, componentes compartilhados e bottom nav

**Files:**
- Create: `app/src/main/java/com/vizinhoalerta/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/vizinhoalerta/ui/components/PrimaryButton.kt`
- Create: `app/src/main/java/com/vizinhoalerta/ui/components/StatusBadge.kt`
- Create: `app/src/main/java/com/vizinhoalerta/ui/components/Avatar.kt`
- Create: `app/src/main/java/com/vizinhoalerta/ui/components/BottomNav.kt`
- Modify: `app/src/main/java/com/vizinhoalerta/MainActivity.kt:64-115`

**Interfaces:**
- Consumes: `MaterialTheme` do Compose; Coil `AsyncImage`.
- Produces:
  - `@Composable fun VizinhoAlertaTheme(content: @Composable () -> Unit)`
  - `@Composable fun PrimaryButton(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, carregando: Boolean = false)`
  - `@Composable fun StatusBadge(texto: String, cor: androidx.compose.ui.graphics.Color)`
  - `@Composable fun Avatar(nome: String, fotoUrl: String? = null, tamanho: androidx.compose.ui.unit.Dp = 32.dp, editavel: Boolean = false, onEditarClick: () -> Unit = {})`
  - `@Composable fun BottomNav(rotaAtual: String?, onMapa: () -> Unit, onAlertas: () -> Unit, onNova: () -> Unit, onPerfil: () -> Unit)`
  - Tokens: `primary=#FF7A1A`, `onPrimary=#FFFFFF`, `secondary=#12203A`, `onSecondary=#FFFFFF`, `surface=#FFFFFF`, `background=#F6F7F9`, `error=#C62828`, `tertiary=#2E7D32`; shapes `extraLarge=28.dp`, botão `CircleShape`.

- [ ] **Step 1: Criar tema e componentes com as assinaturas acima**

`Avatar`: círculo com `clip(CircleShape)`; se `fotoUrl` em branco/nulo, caixa com a primeira letra de `nome` em `onPrimary` sobre `primary`; quando `editavel`, ícone de câmera sobreposto chamando `onEditarClick`.

- [ ] **Step 2: Integrar em `MainActivity.kt`**

Trocar ambos os `AppTheme { ... }` por `VizinhoAlertaTheme { ... }` e remover o `AppTheme` privado. Envolver `NavHost` em `Scaffold` com `bottomBar`, passando a rota corrente via `navController.currentBackStackEntryAsState()`. Mostrar `BottomNav` somente quando a rota não for `welcome`, `login` nem `cadastro`; conectar `onAlertas` a `lista_ocorrencias`, `onNova` a `reportar`, `onPerfil` a `perfil`, `onMapa` a `mapa` com `launchSingleTop`. Passar `Modifier.padding(innerPadding)` ao `NavHost`.

- [ ] **Step 3: Verificar build e navegação no AVD**

Run: `./gradlew assembleDebug --no-daemon && /home/alvaro/.local/share/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: `BUILD SUCCESSFUL`; instalação `Success`.

Run: abrir app → welcome (sem bottom nav) → login demo → mapa (com bottom nav) → tocar Perfil e Alertas.
Expected: bottom nav visível nas rotas autenticadas; ausente em welcome/login/cadastro; nenhuma tela em branco.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-11-23-ui-avatar-perfil-design.md docs/superpowers/plans/2026-11-23-ui-avatar-perfil.md app/src/main/java/com/vizinhoalerta/ui/theme app/src/main/java/com/vizinhoalerta/ui/components app/src/main/java/com/vizinhoalerta/MainActivity.kt
git commit -m "feat: tema laranja e bottom nav com componentes de UI"
```

---

### Task 2: Foto de perfil — dados, captação e persistência

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/model/Usuario.kt:3-15`
- Create: `app/src/main/java/com/vizinhoalerta/data/FotoPerfilValidador.kt`
- Modify: `app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt:37-56,248-304,337-357`
- Modify: `app/src/main/java/com/vizinhoalerta/MainActivity.kt:103-107`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/file_paths.xml`
- Modify: `storage.rules:1-22`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt:20-60,80-103`
- Test: `app/src/test/java/com/vizinhoalerta/data/FotoPerfilTest.kt`

**Interfaces:**
- Consumes: `Avatar(..., editavel = true, onEditarClick = ...)` da Task 1; `Context` injetado no repository.
- Produces:
  - `Usuario.fotoUrl: String = ""`
  - `internal fun validarImagemPerfil(mime: String?, sizeBytes: Int): String?` — `null` quando válido (JPG/PNG, `<= 2 * 1024 * 1024`).
  - `internal fun caminhoFotoPerfilDemo(uid: String): String` — retorna `perfil/<uid>.jpg`.
  - `internal fun mesclarFotoDemo(usuario: Usuario, fotoUrl: String?): Usuario`
  - `suspend fun AuthRepository.salvarFotoPerfil(uri: Uri): Result<String>`
  - `AuthRepository(context: Context? = null)` (parâmetro novo, opcional; defaults antigos intactos).

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
class FotoPerfilTest {
    @Test fun `aceita jpeg e png ate 2MB`() {
        assertNull(validarImagemPerfil("image/jpeg", 2 * 1024 * 1024))
        assertNull(validarImagemPerfil("image/png", 1024))
    }
    @Test fun `rejeita tipo diferente de jpg ou png`() {
        assertEquals("Use JPG ou PNG.", validarImagemPerfil("image/gif", 1024))
        assertEquals("Use JPG ou PNG.", validarImagemPerfil(null, 1024))
    }
    @Test fun `rejeita acima de 2MB`() {
        assertEquals("A foto deve ter ate 2 MB.", validarImagemPerfil("image/png", 2 * 1024 * 1024 + 1))
    }
    @Test fun `caminho demo e estavel por uid`() {
        assertEquals("perfil/demo-morador.jpg", caminhoFotoPerfilDemo("demo-morador"))
    }
    @Test fun `mescla foto sem perder dados do usuario`() {
        val u = Usuario(id = "u1", nome = "Ana", cidade = "Cassia")
        assertEquals("/data/ana.jpg", mesclarFotoDemo(u, "/data/ana.jpg").fotoUrl)
        assertEquals("Ana", mesclarFotoDemo(u, "/data/ana.jpg").nome)
        assertEquals("", mesclarFotoDemo(u, null).fotoUrl)
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha de compilação**

Run: `./gradlew testDebugUnitTest --tests '*FotoPerfilTest*' --no-daemon`
Expected: FAIL — funções/`fotoUrl` ainda não existem.

- [ ] **Step 3: Implementar modelo, validador e repository**

`Usuario`: acrescentar `fotoUrl` ao `data class`. `AuthRepository`: guardar `context?.applicationContext`; em `salvarFotoPerfil(uri)`: exigir `currentUid != null` e `context != null` (senão `Result.failure`), abrir `contentResolver.openInputStream(uri)`, ler bytes, validar com `validarImagemPerfil(mime, bytes.size)`.

Ramo demo: escrever bytes em `context.filesDir/caminhoFotoPerfilDemo(uid)` (criar diretório), salvar caminho absoluto em `SharedPreferences("perfil_fotos")` sob a chave `uid`, atualizar `DemoData.usuarios` com `copy(fotoUrl = caminho)`, retornar o caminho. `DemoData.reset()` continua limpo — as fotos ficam só no arquivo/prefs.

Ramo produção: upload `perfil/<uid>.jpg` via `storage!!.reference.child(...).putBytes(bytes, StorageMetadata.Builder().setContentType(mime).build()).await()`, `downloadUrl`, `auth!!.currentUser!!.updateProfile(userProfileChangeRequest { photoUri = Uri.parse(url) }).await()`, `firestore!!.collection("usuarios").document(uid).update("fotoUrl", url).await()`, retornar `url`.

`getDadosUsuario` e `listarUsuarios` (demo) devem passar por `mesclarFotoDemo(usuario, prefs.getString(uid, null))`.

- [ ] **Step 4: Configurar FileProvider e regra de Storage**

`AndroidManifest.xml`: provider `android:name="androidx.core.content.FileProvider"`, `android:authorities="${applicationId}.fileprovider"`, `exported=false`, `grantUriPermissions=true`, meta-data apontando `@xml/file_paths`. `file_paths.xml`: `<cache-path name="fotos_perfil" path="perfil_camera/" />`. `storage.rules`: `match /perfil/{uid}/{file}` com `allow read: if request.auth != null`, `allow write: if request.auth != null && request.auth.uid == uid && request.resource.size < 2 * 1024 * 1024 && request.resource.contentType.matches('image/(jpeg|png)')`.

- [ ] **Step 5: Ligar captação no Perfil**

Em `PerfilScreen`, registrar `ActivityResultContracts.GetContent()` e `ActivityResultContracts.TakePicture()` (Uri em `cacheDir/perfil_camera/<uid>.jpg` via `FileProvider.getUriForFile`). `Avatar(editavel = true)` dispara escolha de origem; ao receber Uri: limpar erro, marcar `carregando`, chamar `salvarFotoPerfil`, recarregar `usuario`, mostrar "Foto atualizada." ou `it.localizedMessage ?: "Não foi possível salvar a foto."` em pt-BR. Não apagar foto anterior em caso de falha.

- [ ] **Step 6: Rodar testes e build**

Run: `./gradlew testDebugUnitTest --tests '*FotoPerfilTest*' --no-daemon`
Expected: PASS (5 testes).

Run: `./gradlew assembleDebug --no-daemon`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Verificar no AVD**

```bash
ADB=/home/alvaro/.local/share/android-sdk/platform-tools/adb
$ADB push /home/alvaro/vizinho-alerta/app/src/main/res/mipmap-hdpi/ic_launcher.png /sdcard/Pictures/foto-perfil.png 2>/dev/null || true
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
```

Login `morador@demo.com` / `Demo@123` → Perfil → tocar badge de câmera → escolher galeria → selecionar a imagem.
Expected: avatar circular substitui a inicial; sair e entrar novamente mantém a foto. Sem câmera no AVD, testar câmera por revisão dos launchers.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/model/Usuario.kt app/src/main/java/com/vizinhoalerta/data/FotoPerfilValidador.kt app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt app/src/main/java/com/vizinhoalerta/MainActivity.kt app/src/main/AndroidManifest.xml app/src/main/res/xml/file_paths.xml storage.rules app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt app/src/test/java/com/vizinhoalerta/data/FotoPerfilTest.kt
git commit -m "feat: foto de perfil com galeria, camera e persistencia demo"
```

---

### Task 3: Aplicar tema e avatar em Welcome, Login, Cadastro, Perfil e Admin

**Files:**
- Modify: `app/src/main/java/com/vizinhoalerta/ui/welcome/WelcomeScreen.kt`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/login/LoginScreen.kt:36-98`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/cadastro/CadastroScreen.kt`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt:61-159`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/admin/AdminUsuariosTab.kt:43-58`

**Interfaces:**
- Consumes: `PrimaryButton`, `StatusBadge`, `Avatar` (Task 1); `Usuario.fotoUrl` (Task 2).
- Produces: nenhuma assinatura pública nova.

- [ ] **Step 1: Restilar Welcome/Login/Cadastro**

`WelcomeScreen`: hero `secondary` no topo com título "Seu bairro mais seguro, juntos." (palavra "juntos" em `primary`) e card de alerta decorativo; botões em `PrimaryButton` pill. `LoginScreen`: manter `modoDemo` e hint "Demo: admin@demo.com / Demo@123"; transformar "ENTRAR" em `PrimaryButton` e adicionar toggle de visibilidade da senha (`Icons.Default.Visibility`/`VisibilityOff`) sem remover validações nem textos de erro. `CadastroScreen`: mesmo padrão de fields/card, botão "Criar minha conta" via `PrimaryButton`.

- [ ] **Step 2: Restilar Perfil e Admin**

`PerfilScreen`: `Avatar(nome = usuario?.nome ?: "Usuário", fotoUrl = usuario?.fotoUrl, tamanho = 96.dp, editavel = true, onEditarClick = ...)` substituindo `Icons.Default.Person`; "Editar dados", "Painel Admin" e "Sair" em botões pill (`PrimaryButton` para ações primárias, `OutlinedButton` com `CircleShape` para secundárias). `AdminUsuariosTab`: `Avatar(... tamanho = 40.dp)` à esquerda de nome/e-mail/função no card.

- [ ] **Step 3: Verificar build e textos no AVD**

Run: `./gradlew assembleDebug --no-daemon && /home/alvaro/.local/share/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: `BUILD SUCCESSFUL`, `Success`.

Run: `python3 /tmp/opencode/ui_dump.py` em welcome, login, perfil e admin.
Expected: textos "Entrar", "Cadastrar-se", "ENTRAR", hint demo, "Perfil", "Painel Admin" (admin) e "Sair" presentes; avatar em vez de ícone genérico no perfil/admin.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/ui/welcome app/src/main/java/com/vizinhoalerta/ui/login app/src/main/java/com/vizinhoalerta/ui/cadastro app/src/main/java/com/vizinhoalerta/ui/perfil app/src/main/java/com/vizinhoalerta/ui/admin
git commit -m "feat: aplica identidade visual e avatar nas telas de entrada e perfil"
```

---

### Task 4: Mapa com busca/filtro, lista e detalhe com avatar do autor

**Files:**
- Create: `app/src/main/java/com/vizinhoalerta/ui/mapa/FiltroOcorrencias.kt`
- Test: `app/src/test/java/com/vizinhoalerta/ui/mapa/FiltroOcorrenciasTest.kt`
- Modify: `app/src/main/java/com/vizinhoalerta/MainActivity.kt:103-184`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/mapa/MapaScreen.kt:24-115`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/lista/OcorrenciasListScreen.kt:21-94`
- Modify: `app/src/main/java/com/vizinhoalerta/ui/detalhes/DetalhesScreen.kt:55-120`

**Interfaces:**
- Consumes: `Avatar`, `StatusBadge`, `PrimaryButton` (Task 1); `AuthRepository.getDadosUsuario(uid)` (já existente).
- Produces:
  - `enum class CategoriaFiltro { TODAS, SEGURANCA, INFRA, SUSPEITA }`
  - `data class FiltroOcorrencias(val busca: String = "", val categoria: CategoriaFiltro = CategoriaFiltro.TODAS)`
  - `fun FiltroOcorrencias.match(o: Ocorrencia): Boolean`
  - `MapaScreen(..., filtro: FiltroOcorrencias, onFiltroChange: (FiltroOcorrencias) -> Unit)`
  - `OcorrenciasListScreen(..., filtro: FiltroOcorrencias, authRepository: AuthRepository)`

- [ ] **Step 1: Escrever testes do filtro**

```kotlin
class FiltroOcorrenciasTest {
    private val iluminacao = Ocorrencia(id="1", tipo="Iluminação danificada", descricao="Poste apagado na Av. Presidente Kubitschek.", bairro="Centro", cidade="Cássia")
    private val suspeita = Ocorrencia(id="2", tipo="Suspeita", descricao="Movimento suspeito", bairro="Centro", cidade="Cássia")

    @Test fun `filtro padrao aceita todas`() {
        assertTrue(FiltroOcorrencias().match(iluminacao))
        assertTrue(FiltroOcorrencias().match(suspeita))
    }
    @Test fun `busca ignora caixa e procura descricao`() {
        assertTrue(FiltroOcorrencias(busca="kubitschek").match(iluminacao))
        assertFalse(FiltroOcorrencias(busca="KUBITSCHEK").match(suspeita))
    }
    @Test fun `categoria infra so aceita iluminacao e outra`() {
        assertTrue(FiltroOcorrencias(categoria=CategoriaFiltro.INFRA).match(iluminacao))
        assertFalse(FiltroOcorrencias(categoria=CategoriaFiltro.INFRA).match(suspeita))
    }
    @Test fun `categoria seguranca so aceita tipos de seguranca`() {
        assertTrue(FiltroOcorrencias(categoria=CategoriaFiltro.SEGURANCA).match(Ocorrencia(tipo="Briga")))
        assertFalse(FiltroOcorrencias(categoria=CategoriaFiltro.SEGURANCA).match(iluminacao))
    }
}
```

Mapeamento: `SEGURANCA` = Suspeita/Briga/Vandalismo/Animal perigoso; `INFRA` = Iluminação danificada/Outra; `SUSPEITA` = Suspeita. Busca compara `tipo`, `bairro`, `cidade`, `descricao` com `contains(..., ignoreCase = true)`.

- [ ] **Step 2: Rodar e confirmar falha**

Run: `./gradlew testDebugUnitTest --tests '*FiltroOcorrenciasTest*' --no-daemon`
Expected: FAIL — classe ainda não existe.

- [ ] **Step 3: Implementar filtro e hoist de estado**

`FiltroOcorrencias.kt` com as assinaturas acima. Em `MainActivity`, `var filtro by remember { mutableStateOf(FiltroOcorrencias()) }`; passar `filtro`/`onFiltroChange` para `MapaScreen` e `filtro`/`authRepository` para `OcorrenciasListScreen`.

- [ ] **Step 4: Aplicar em Mapa e Lista**

`MapaScreen`: campo `OutlinedTextField` de busca ("Buscar endereço ou ocorrência") e chips `Todas/Segurança/Infra/Suspeita` acima do mapa, alterando `filtro`; chamar `MapaBackend.Content(ocorrencias.filter(filtro::match), userLocation, centralizar, onOcorrenciaClick)`. Remover o `IconButton` de logout da top bar (agora só no Perfil). Manter FAB "Reportar".

`OcorrenciasListScreen`: `val visiveis = ocorrencias.filter(filtro::match)`; manter estado vazio "Nenhuma ocorrência registrada." quando vazio; para cada card, carregar `authRepository.getDadosUsuario(item.userId)?.fotoUrl` em `LaunchedEffect(item.userId)` com cache `remember { mutableStateMapOf<String, String?>() }`; exibir `Avatar(nome = item.usuarioNome.ifBlank { "?" }, fotoUrl = cache[item.userId])`.

- [ ] **Step 5: Detalhes com autor e badge**

Em `DetalhesScreen`, substituir o cabeçalho simples por bloco com `secondary` contendo título, `StatusBadge` para `ABERTO`/`RESOLVIDO`, e ao lado de `usuarioNome` o `Avatar` do autor (busca via `authRepository.getDadosUsuario(o.userId)`). Não alterar botões `MARCAR COMO RESOLVIDO`, `EXCLUIR`, `ALTERAR LOCALIZAÇÃO`, `SALVAR LOCALIZAÇÃO`, `CANCELAR` nem o listener do mapa.

- [ ] **Step 6: Rodar testes, build e AVD**

Run: `./gradlew testDebugUnitTest --tests '*FiltroOcorrenciasTest*' --no-daemon && ./gradlew testDebugUnitTest assembleDebug --no-daemon`
Expected: 4 testes do filtro PASS; suíte completa PASS; `BUILD SUCCESSFUL`.

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`; login `admin@demo.com` / `Demo@123` → mapa → chip "Infra"; depois Lista.
Expected: só ocorrências de iluminação aparecem; avatar do autor aparece; ao limpar filtro, lista volta ao normal; nenhuma tela em branco.

- [ ] **Step 7: Verificar autoria intacta**

Run: abrir "Iluminação danificada" como admin; tocar "ALTERAR LOCALIZAÇÃO", tocar no mapa, "SALVAR LOCALIZAÇÃO"; sair e reabrir.
Expected: ações presentes, modo de edição encerra e posição persiste (como no branch anterior). Depois login `morador@demo.com`: mesma ocorrência sem ações de gestão.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/vizinhoalerta/ui/mapa app/src/test/java/com/vizinhoalerta/ui/mapa app/src/main/java/com/vizinhoalerta/MainActivity.kt app/src/main/java/com/vizinhoalerta/ui/lista app/src/main/java/com/vizinhoalerta/ui/detalhes
git commit -m "feat: busca, filtros e avatar do autor em mapa, lista e detalhes"
```

---

### Task 5: Verificação final, review e PR

**Files:**
- Modify: `.superpowers/sdd/2026-11-23-ui-avatar-perfil/progress.md` (criar ledger com preflight, decisões e relatórios)

**Interfaces:**
- Consumes: todos os commits das Tasks 1–4.
- Produces: PR aberto, não mesclado.

- [ ] **Step 1: Suíte completa e guards**

```bash
cd /home/alvaro/vizinho-alerta
./gradlew testDebugUnitTest assembleDebug --no-daemon
./gradlew compileReleaseKotlin --no-daemon
git diff --check main..HEAD
git diff --stat main..HEAD
```

Expected: debug PASS com todos os testes; release FAIL em `checkReleaseGoogleServices` (DUMMY); `git diff --check` sem saída.

- [ ] **Step 2: Contar testes e conferir diff**

```bash
python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
t=f=e=0
for p in glob.glob('app/build/test-results/testDebugUnitTest/*.xml'):
    r=ET.parse(p).getroot(); t+=int(r.get('tests',0)); f+=int(r.get('failures',0)); e+=int(r.get('errors',0))
print(f'tests={t} failures={f} errors={e}')
PY
```

Expected: `failures=0 errors=0`, sem arquivos fora de escopo no `git status`.

- [ ] **Step 3: Review do branch**

Gerar pacote com `scripts/review-package` da skill `subagent-driven-development` e despachar reviewer no modelo mais capaz. Corrigir findings Important em uma única fix wave, re-revisar e só então publicar.

- [ ] **Step 4: Push e PR**

```bash
git push -u origin feature/2-ui-avatar-perfil
gh pr create --title 'feat: UI em nível de referência e foto de perfil do vizinho' \
  --body "$(printf 'Closes #N\n\nResumo das mudanças.\n')" --label enhancement
```

Expected: PR `OPEN`, label `enhancement`, `Closes #N` linkado. Não mesclar.

## Adendo — Task 4 revisada (2026-11-24)

A Task 4 original (busca/chips acima do mapa) é substituída pelo overlay flutuante descrito no adendo do spec. O filtro TDD (`CategoriaFiltro`, `FiltroOcorrencias`, 4 testes) é mantido como base de lógica; muda apenas a apresentação no `MapaScreen`. O cartão flutuante usa `Ocorrencia` real — sem mapper `OcorrenciaUi`. Verificação de autoria (Step 7 original) permanece obrigatória.
