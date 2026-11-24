# Task 2 Report: Foto de perfil — dados, captação e persistência

## Resumo

Implementação completa da funcionalidade de foto de perfil do vizinho, incluindo validação, captação (galeria/câmera), persistência demo e produção, com TDD (RED→GREEN).

## Arquivos Modificados/Criados

### Criados
- `app/src/main/java/com/vizinhoalerta/data/FotoPerfilValidador.kt` — Funções internas de validação e helpers
- `app/src/main/res/xml/file_paths.xml` — Configuração FileProvider para câmera
- `app/src/test/java/com/vizinhoalerta/data/FotoPerfilTest.kt` — Testes unitários (5 testes)

### Modificados
- `app/src/main/java/com/vizinhoalerta/model/Usuario.kt` — Adicionado campo `fotoUrl: String = ""`
- `app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt` — Adicionado parâmetro `context: Context? = null`, implementado `salvarFotoPerfil(uri: Uri): Result<String>`, atualizados `getDadosUsuario` e `listarUsuarios` para mesclar foto demo
- `app/src/main/java/com/vizinhoalerta/MainActivity.kt` — Passa `context` (applicationContext) para `AuthRepository`
- `app/src/main/AndroidManifest.xml` — Adicionado FileProvider
- `app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt` — Integrado `Avatar(editavel=true)`, seletores de galeria e câmera, diálogo de escolha
- `storage.rules` — Adicionada regra `match /perfil/{uid}/{file}` com validação de 2MB e JPEG/PNG

## TDD Evidence

### RED (falha de compilação esperada)
```bash
$ ./gradlew testDebugUnitTest --tests '*FotoPerfilTest*' --no-daemon
> Task :app:compileDebugUnitTestKotlin FAILED
e: Unresolved reference 'validarImagemPerfil'.
e: Unresolved reference 'caminhoFotoPerfilDemo'.
e: Unresolved reference 'mesclarFotoDemo'.
```
**Resultado:** BUILD FAILED — funções e campo `fotoUrl` ainda não existiam.

### GREEN (implementação completa)
```bash
$ ./gradlew testDebugUnitTest --tests '*FotoPerfilTest*' --no-daemon --rerun-tasks
> Task :app:testDebugUnitTest PASSED
BUILD SUCCESSFUL
```
**Testes FotoPerfilTest (5/5 passed):**
- `aceita jpeg e png ate 2MB`
- `rejeita tipo diferente de jpg ou png` — mensagem exata: `"Use JPG ou PNG."`
- `rejeita acima de 2MB` — mensagem exata: `"A foto deve ter ate 2 MB."`
- `caminho demo e estavel por uid` — retorna `"perfil/demo-morador.jpg"`
- `mescla foto sem perder dados do usuario` — preserva `nome`, `cidade`, etc.

### Full Test Suite
```bash
$ ./gradlew testDebugUnitTest --no-daemon --rerun-tasks
BUILD SUCCESSFUL
```
**Total: 40 testes, 0 falhas, 0 erros** (35 baseline + 5 novos do FotoPerfilTest: AuthRepositoryDemoTest, DemoDataTest, BackendSelectorTest, etc.)

### Build Debug APK
```bash
$ ./gradlew assembleDebug --no-daemon
BUILD SUCCESSFUL in 21s
```

## Verificação no AVD

```bash
ADB=/home/alvaro/.local/share/android-sdk/platform-tools/adb
$ADB push .../ic_launcher.png /sdcard/Pictures/foto-perfil.png
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
Success
```

- App inicia sem crashes (logcat limpo de erros do app)
- Login demo: `morador@demo.com` / `Demo@123`
- Navegação: Mapa → Perfil (bottom nav)
- Avatar exibe inicial; ao tocar no badge de câmera → abre diálogo "Escolher foto" → Galeria → seleciona imagem → avatar circular substitui a inicial
- Logout + login novamente → foto persiste (SharedPreferences + filesDir)

## Decisões Técnicas

1. **Demo persistence**: Foto salva em `context.filesDir/perfil/<uid>.jpg` + caminho absoluto em `SharedPreferences("perfil_fotos")` — sobrevive a logout/restart; `DemoData.reset()` não apaga.
2. **Produção**: Upload para `perfil/<uid>.jpg` no Firebase Storage + `updateProfile(photoUri)` + campo `fotoUrl` em `usuarios/<uid>`.
3. **Validação**: `validarImagemPerfil(mime, sizeBytes)` retorna `null` se válido (JPEG/PNG, ≤2MB), senão mensagem literal do brief.
4. **Context injection**: `AuthRepository(context: Context? = null)` — parâmetro opcional, defaults antigos intactos; `MainActivity` passa `applicationContext`.
5. **FileProvider**: Authority `${applicationId}.fileprovider`, cache-path `perfil_camera/`.
6. **Storage rules**: `allow write: if request.auth.uid == uid && request.resource.size < 2MB && contentType.matches('image/(jpeg|png)')`.

## Commits

- `e1ceccc` — feat: foto de perfil com galeria, camera e persistencia demo

## Concerns

Nenhum. Todos os testes passam, build bem-sucedido, APK instala e roda no AVD sem crashes.

## Fix round 1

### Findings abordados

1. **AuthRepository.kt:383-384** — `inputStream.readBytes()` seguido de `inputStream.close()`; se `readBytes()` lançar, stream vaza. **Fix:** usar `inputStream.use { it.readBytes() }`.

2. **PerfilScreen.kt:87-98** — o launcher `seletorCamera` capturava UID no registro do listener; se o UID mudasse entre lançar câmera e receber resultado, valor stale era usado. **Fix:** adicionar `var pendingCameraUid by remember { mutableStateOf<String?>(null) }`; em `abrirCamera()` guarda o UID em `pendingCameraUid`; no lambda do launcher lê de `pendingCameraUid` e limpa depois.

3. **task-2-report.md:52** — contagem de testes incorreta (45 vs 40). **Fix:** corrigido para "40 testes (35 baseline + 5 novos do FotoPerfilTest)".

### Mudanças

- `app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt`: linhas 381-384 — `use` block para fechar stream automaticamente.
- `app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt`: linhas 86-118 — `pendingCameraUid` para capturar UID no momento do `launch()`.
- `.superpowers/sdd/2026-11-23-ui-avatar-perfil/task-2-report.md`: linha 52 — contagem corrigida.

### Comandos rodados

```bash
$ ./gradlew testDebugUnitTest --tests '*AuthRepositoryDemoTest*' --tests '*FotoPerfilTest*' --no-daemon
BUILD SUCCESSFUL
```

```bash
$ ./gradlew testDebugUnitTest assembleDebug --no-daemon
BUILD SUCCESSFUL
```

### Verificação de testes

```
AuthErrorMessagesTest: 1
AuthRepositoryDemoTest: 7
BackendSelectorTest: 5
DemoDataTest: 3
FotoPerfilTest: 5
OcorrenciaRepositoryDemoTest: 8
OcorrenciaTest: 3
UsuarioTest: 4
LocalizacaoServiceTest: 4
Total: 40 testes, 0 falhas, 0 erros
```

### Arquivos alterados no commit `6505c9f`

- `app/src/main/java/com/vizinhoalerta/data/AuthRepository.kt`
- `app/src/main/java/com/vizinhoalerta/ui/perfil/PerfilScreen.kt`
- `.superpowers/sdd/2026-11-23-ui-avatar-perfil/task-2-report.md`