# Ajuste de layout — linha demo no login

## Intenção

Na tela de login, a linha `Demo: admin@demo.com / Demo@123` hoje fica entre o botão **ENTRAR** e o **Continuar com Google**, com um `Spacer(8.dp)`. O usuário quer essa linha abaixo do link `Não tem conta? Cadastrar` e os botões do login na mesma distância do cadastro.

## Abordagens

1. **Mover o bloco existente (escolhida).** Deslocar `if (modoDemo) { Spacer(8.dp); Text(...) }` de depois do `PrimaryButton` para depois do `TextButton`. Remove o único elemento que afastava **ENTRAR** de **Google**; cadastro não é tocado. Quatro linhas, sem abstração nova.
2. Reduzir/remover o `Spacer` e manter a linha onde está. Rejeitada: o usuário pediu a linha em outra posição.
3. Extrair um componente/spacing compartilhado entre login e cadastro. Rejeitada: as duas `Column` já usam `Arrangement.spacedBy(12.dp)`; seria abstração sem ganho (YAGNI) e mexeria no cadastro.

## Decisões

- `LoginScreen.kt`: bloco demo vai após `TextButton(onClick = onCadastrarClick)`.
- `CadastroScreen.kt`: inalterado.
- Texto, `fontSize = 12.sp`, `modoDemo` e o `Spacer(8.dp)` preservados.
- Sem mudança de comportamento, navegação, constantes ou dependências.

## Verificação

- `assembleDebug testDebugUnitTest --no-daemon`: BUILD SUCCESSFUL, suíte verde.
- `git diff --check` limpo; commit único na branch `feature/17-google-signin`.
- Opcional: recapturar `docs/screenshots/11-login-google.png` no emulador e atualizar o README/PR se a captura ficar desatualizada.

## Fora de escopo

- `LoginFlowTest` pré-existente com rótulos desatualizados.
- Qualquer alteração no cadastro, no botão Google ou no fluxo de autenticação.
