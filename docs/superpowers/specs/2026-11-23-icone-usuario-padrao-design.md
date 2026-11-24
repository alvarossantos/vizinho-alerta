# Ícone padrão de usuário no Avatar — design curto

**Data:** 2026-11-23
**Issue:** a criar (`enhancement`)

## Intenção

Substituir a inicial do nome por uma silhueta de usuário nas cores do tema em **todos** os avatares sem foto.

## Por quê

O fallback atual é um círculo laranja com a letra inicial (`Avatar.kt:35-63`). O usuário pediu um ícone próprio do app para essa função, coerente com a identidade visual dos mockups.

## Goals

- Criar `app/src/main/res/drawable/ic_usuario_padrao.xml`: vetor com a silhueta (cabeça + ombros) em branco, viewport 24×24.
- Em `Avatar.kt`, quando `fotoUrl` estiver nula ou em branco, trocar a `Text` da inicial por `Icon(painterResource(R.drawable.ic_usuario_padrao), contentDescription = "Sem foto de perfil")`.
- Atingir automaticamente Perfil (editável), Detalhe, Lista e Admin — todos consomem o mesmo composable.

## Non-goals

- Novo campo de dados; mudanças em `Usuario`, `AuthRepository`, Storage ou cadastro.
- Alterar o `AsyncImage`, o selo de câmera do modo editável ou os tamanhos atuais (28–40 dp).
- Nova dependência Gradle.

## Constraints

- O nome é obrigatório no cadastro; não existe avatar sem nome. A silhueta substitui a letra em **todo** caso sem foto, independentemente do nome.
- O `Box` de fallback já aplica `background(MaterialTheme.colorScheme.primary, CircleShape)`. O vetor conterá **apenas a silhueta branca**, sem duplicar o círculo — assim o fundo continua vindo do tema (`#F97316` no light) e acompanha o dark scheme.
- XML vetor não lê `MaterialTheme`; a silhueta usa branco literal (`#FFFFFF`), que já é `onPrimary` nos dois esquemas.

## Approaches

- **A (escolhida):** drawable vetorial próprio. Pró: asset versionado, editável, com a identidade do app. Contra: um arquivo XML novo.
- **B (descartada):** `Icons.Filled.Person` do Material. Pró: zero arquivo. Contra: silhueta genérica do Android, sem identidade própria — não atende "crie um ícone no tema do app".

## How decidido

- Fundo laranja permanece no `Box` do `Avatar`; o drawable só desenha a silhueta.
- `contentDescription = "Sem foto de perfil"` para acessibilidade.
- Nenhuma lógica nova de fallback: continua valendo `fotoUrl?.isNotBlank() == true`.

## O que fica para o builder

- Verificar no AVD que a silhueta aparece em Perfil, Lista, Detalhe e Admin, e que o selo de câmera continua sobre o avatar editável.
- Seguir o fluxo padrão: Issue `enhancement` → branch → commit → PR `Closes #N`, sem merge sem aprovação explícita.
