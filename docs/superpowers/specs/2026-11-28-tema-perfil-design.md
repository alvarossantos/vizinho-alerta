# Design: seletor de tema no Perfil

Issue #22. Documento curto: mudança pequena, escopo localizado.

## Intenção

Hoje o tema segue somente o Android. Usuário quer escolher **Seguir o sistema**, **Claro** ou **Escuro** em **Perfil → Tema**, com persistência no dispositivo e aplicação imediata, sem recriar a Activity.

## Objetivos

- Seletor de três opções em Perfil, padrão **Seguir o sistema**.
- Preferência salva em SharedPreferences e preservada entre aberturas.
- Mesmo valor de tema efetivo decide esquema de cores do Compose e aparência da barra de status.
- Troca recomponde na hora; usuário continua na mesma tela.
- Em **Seguir o sistema**, mudanças do Android são refletidas.

## Não objetivos / anti-objetivos

- Não adicionar dependência nova (sem AppCompat, sem DataStore).
- Não recriar Activity nem trocar `ComponentActivity` por `AppCompatActivity`.
- Não criar tela de configurações separada; a opção fica em Perfil.
- Não alterar permissões nem dados de conta.

## Restrições

- `MainActivity` é `ComponentActivity` com `activity-compose:1.9.0`, `core-ktx:1.13.1`.
- `VizinhoAlertaTheme` hoje lê `LocalConfiguration.current.uiMode` diretamente.
- SharedPreferences já é usado em `AuthRepository`; segue o mesmo padrão, sem nova lib.
- Único recurso `values-night` é `themes.xml` (cor da barra de status); não há outros qualificadores night em `res/`.

## Abordagens

| Abordagem | Trade-off |
|---|---|
| Compose puro + SharedPreferences + `WindowCompat` | Nenhuma dependência; troca sem recriar Activity; barra de status precisa ser ajustada explicitamente. **Escolhida.** |
| `AppCompatDelegate.setDefaultNightMode` + `AppCompatActivity` | Barra automática via `values-night`, mas adiciona dependência e recria a Activity a cada troca. |

## Decisões

1. `PreferenciaTema` com `SISTEMA`, `CLARO`, `ESCURO`; padrão `SISTEMA` quando não há chave salva.
2. Repositório mínimo com `ler()` / `salvar()` em SharedPreferences (`tema_prefs`, chave `preferencia_tema`).
3. Função pura `temaEfetivo(preferencia, modoNoturnoSistema): Boolean` — testável sem Android.
4. `VizinhoAlertaTheme` recebe a preferência, resolve o booleano e escolhe `LightColorScheme`/`DarkColorScheme`. **Não** sobrescrever `LocalConfiguration` de início.
5. Barra de status: `SideEffect`/`DisposableEffect` no tema aplica cor e `isAppearanceLightStatusBars` com `WindowCompat` a partir do mesmo booleano. Isso cobre explicitamente o único recurso `values-night` relevante; se outro recurso night aparecer, tratar caso a caso.
6. `PerfilScreen` recebe a preferência e um callback; `AlertDialog` com as três opções, sem sair da tela.
7. Preferência fornecida desde `MainActivity` e lembrada com `remember`/estado para recompor ao salvar.

## Aceite

- Primeira instalação segue o Android; escolha persiste ao reabrir.
- Trocar em Perfil → Tema mantém o usuário em Perfil e atualiza contraste dos ícones da barra de status.
- Em **Seguir o sistema**, alternar o tema do Android reflete no app.
- Testes unitários da resolução e da persistência; build e testes verdes antes do PR.
