# Perfil editável, painel admin e modo protótipo com fallback

Data: 2026-11-23

## Intenção

O testador precisa avaliar o app sem configurar Firebase. Ao mesmo tempo, o usuário comum deve conseguir atualizar seus dados e o admin deve ver todos os usuários (nome/e-mail) e todas as ocorrências, em qualquer situação. As capturas do README devem mostrar esse estado.

## Objetivos

- Perfil com formulário de edição dos próprios dados.
- Painel admin com duas áreas: ocorrências (todas as situações) e usuários (nome/e-mail).
- App utilizável sem Firebase: tenta Firebase primeiro e cai para dados locais se o backend não responder.
- Capturas atualizadas: perfil admin, painel admin de ocorrências e lista de usuários.

## Não-objetivos

- Nova dependência (Room, DataStore, DI).
- Persistência dos dados do modo demo entre execuções.
- Release com `google-services.json` fictício **nem fallback demo em release**.
- Reescrita das telas de login/cadastro; no máximo uma dica de credenciais demo.

## Restrições

- Debug aponta para emuladores em `10.0.2.2:9099/8081/9199` (`VizinhoAlertaDebugApp.kt:13-16`).
- `AuthRepository.atualizarPerfil` já existe (`AuthRepository.kt:174-214`), mas não é chamado pela UI.
- `OcorrenciaRepository.listar()` já não filtra (`OcorrenciaRepository.kt:95-108`), mas engole erros e retorna lista vazia.
- Regras atuais: `usuarios` só dono lê/escreve (`firestore.rules:4-6`); admin por claim (`firestore.rules:28-35`); app trata admin por `role` (`Usuario.kt:14`).
- Sem.backend, login falha com erro de rede não traduzido e as telas ficam vazias sem aviso.

## Abordagens

1. **Demo local fixo no debug** — simples, mas ignora quem tem emuladores e exigiria alternar manualmente.
2. **Exigir emuladores Firebase** — reusa quase tudo, mas o testador precisa configurar Firebase; contraria a ressalva.
3. **Firebase com fallback automático (escolhida)** — uma sonda no startup decide o backend; quem tem emuladores continua usando Firebase, quem não tem cai para dados locais sem saber. Custo: uma indireção pequena nos repositórios e uma sonda com timeout.

Escolheu-se a 3. É a única que atende simultaneamente quem tem e quem não tem Firebase, sem duplicar telas.

## Como ficará

### 1. Seleção do backend

- Novo `data/BackendSelector.kt`: `enum class BackendMode { FIREBASE, DEMO }` e `suspend fun detectar(): BackendMode`.
- **O fallback só existe em debug**: se `!BuildConfig.DEBUG`, retorna `FIREBASE` sem sondar. Release nunca aceita login demo, independente de conectividade.
- Sonda (debug): `withTimeout(4_000) { firestore.collection("ocorrencias").limit(1).get().await() }`.
- `PERMISSION_DENIED` conta como **FIREBASE** (backend respondeu; sem login é esperado). `UNAVAILABLE`, `DEADLINE_EXCEEDED`, timeout ou qualquer falha de conexão contam como **DEMO**.
- `MainActivity` mostra um indicador enquanto a sonda roda, cria os repositórios com `demo = (modo == DEMO)` e só então monta o `NavHost`.
- Em release, se o Firebase falhar, a tela mostra erro de conexão em português; nunca cai para dados fictícios.

### 2. Dados demo

- Novo `data/DemoData.kt`: listas mutáveis em memória com 2 usuários (`admin@demo.com` / `morador@demo.com`, senha `Demo@123`) e 5 ocorrências em Cássia/Passos cobrindo `registrado`, `em_verificacao`, `confirmado`, `resolvido`, `rejeitado`.
- `AuthRepository` e `OcorrenciaRepository` ganham `demo: Boolean = false`. Quando `demo`, leem/escrevem essas listas; quando não, seguem com Firebase. Nenhuma assinatura de tela muda.
- `currentUser` continua privado para Firebase; passam a existir `currentUid: String?` e `currentNome: String`, usados por `MainActivity`, `PerfilScreen`, `ReportarScreen` e `MapaScreen`.
- Login demo aceita qualquer e-mail com senha de 6+ caracteres; `admin@demo.com` vira admin, os demais viram morador. `LoginScreen` mostra a dica das credenciais apenas no modo demo.
- Cadastro demo cria o usuário na lista em memória com `role = user`, usando a **mesma política de senha** do Firebase (`validarSenhaSegura`, 8+ caracteres), e ele passa a aparecer na aba Usuários do admin.

### 3. Identidade do admin

- `AuthRepository.isAdmin(): Boolean`: no demo lê `role`; no Firebase usa claim `admin` **ou** `role` do próprio documento.
- Regras Firestore ajustadas: `isAdmin()` = claim ou `get(usuarios/$(uid)).data.role == 'admin'`; `usuarios` passa a permitir `list` para admin, `create` só com `role == 'user'`, `update` do próprio usuário sem alterar `role`, e Admin continua podendo atualizar ocorrências em qualquer situação.
- Rota `admin` protegida: se `isAdmin()` for falso, a tela mostra "Acesso negado" com botão de voltar.

### 4. Perfil

- `PerfilScreen` passa a ter botão **Editar dados** que alterna para campos: nome, username, telefone, estado, cidade, bairro.
- Salvar chama `atualizarPerfil` existente; mostra erro traduzido e mensagem de sucesso; recarrega o usuário.
- Botão **Painel Admin** continua visível apenas para admin (agora usando `isAdmin()`).

### 5. Painel admin

- `AdminScreen` ganha abas **Ocorrências** e **Usuários** (`TabRow`, sem pager/new dep).
- aba Ocorrências: usa `listar()` sem filtro, mostra as 5 situações, adiciona ação **Rejeitado** (`situacao = 'rejeitado'`, `status = 'resolvido'`, pois situação terminal não deve continuar com botão de resolver), mensagem de lista vazia e mensagem de erro quando houver falha. `listar()` passa a devolver `Result<List<Ocorrencia>>`.
- aba Usuários: `AuthRepository.listarUsuarios()` mostra nome e e-mail (e role), com estado vazio/erro.

### 6. Capturas e README

- Com emuladores parados (modo demo ativo), entrar como `admin@demo.com`, capturar:
  - `docs/screenshots/01-perfil.png` — perfil admin com campos editáveis e botão do painel;
  - `docs/screenshots/02-admin.png` — aba Ocorrências com situações variadas;
  - `docs/screenshots/08-admin-usuarios.png` — aba Usuários com nome/e-mail.
- README: galeria ganha a nova imagem e uma linha explicando as credenciais demo.

### 7. Verificação

- `./gradlew assembleDebug testDebugUnitTest` (todos os testes existentes + novos testes puros de `DemoData`/admin).
- `compileReleaseKotlin` com chave temporária, restaurando a dummy depois.
- Capturas validadas por dimensão 1080x2280, contagem de cores e OCR.
- `git diff` sem chave real; commit e push após verificação.

## Decisões do construtor (minhas, registrar no plano)

- Timeout da sonda: 4 s; `PERMISSION_DENIED` = backend vivo; fallback **somente debug**.
- Login demo: senha mínima 6; cadastro demo: senha mínima 8 (mesma validação do Firebase). Credenciais fixas apenas para conveniência do testador.
- 5 ocorrências demo, uma por situação, para a captura mostrar todas.
- Abas em vez de rotas novas para usuários, evitando mais navegação.
- Rejeitado fecha a ocorrência (`status = resolvido`).
