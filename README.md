# Vizinho Alerta

App Android (Kotlin) de segurança comunitária para reportar ocorrências com foto, áudio, GPS e cidade, usando câmera, microfone, GPS e internet.

## Ideia do projeto
Moradores registram situações suspeitas ou problemas urbanos (rua escura, vandalismo, briga, animal perigoso, iluminação danificada) com evidências multimídia. As ocorrências aparecem em mapa compartilhado em tempo real, permitindo que vizinhos vejam o que acontece na região e marquem como resolvido.

## Recursos do equipamento usados
- **Câmera (CameraX):** foto da evidência
- **Microfone:** relato de voz de 10-15s
- **GPS (FusedLocation):**latitude/longitude automáticas
- **Internet (Firebase):** mapa/feed compartilhado via Auth, Firestore e Storage

## Estrutura
```
app/src/main/java/com/vizinhoalerta/
├── MainActivity.kt
├── model/
│   ├── Ocorrencia.kt
│   └── Usuario.kt
├── data/
│   └── OcorrenciaRepository.kt
├── ui/
│   ├── login/
│   ├── mapa/
│   ├── reportar/
│   └── detalhes/
└── service/
    ├── CameraService.kt
    ├── LocalizacaoService.kt
    └── AudioService.kt
```

## Funcionalidades MVP
- Cadastro/login com nome, email, cidade e bairro
- **Mapa com localização do usuário em azul** (Blue Dot) — todos veem onde estão
- Botão "Local Atual" → recente a câmera para a posição do usuário
- Clique no mapa → menu de confirmação da escolha do local
- Escolha local: clique no mapa no Reportar (confirmar) ou use GPS automático
- Nova ocorrência: foto + áudio + GPS + cidade + bairro customizado
- Feed de ocorrências em tempo real com marcadores coloridos
- Detalhes: foto, reprodução de áudio, mapa de localização, botão "Marcar como Resolvido"
- Ocorrências aparecem em mapa azul para todos (lado de baixo: verde = resolvido, vermelho = aberto)
- Usuário pode relatar em cidades ou bairros diferentes dos cadastrados na conta

## Como rodar (passo a passo)

### 1. Firebase (gratuito)
1. Acesse https://console.firebase.google.com
2. Crie um projeto chamado `vizinho-alerta`
3. Clique em **Android** → insira `com.vizinhoalerta` como package name
4. Baixe o `google-services.json` → cole em `app/`
5. Em **Authentication** → habilite método **E-mail/Senha**
6. Em **Firestore Database** → crie banco em modo **locked** (regras já no repo)
7. Em **Storage** → crie bucket padrão

### 2. Google Maps API (gratuito até US$ 200/mês)
**NÃO é obrigatório para teste local** — sem chave, o app funciona mas mapa ficará cinza.
1. Acesse https://console.cloud.google.com → **APIs & Services** → **Credenciais**
2. Criar chave de API → **Restringir chave**
   - **App Android** → `com.vizinhoalerta`
   - **APIs** → `Maps SDK for Android`
3. Copie a chave e cole em `app/src/main/AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="SUA_CHAVE_AQUI" />
   ```

### 3. Sincronizar o Gradle
Abra o projeto no Android Studio → **File** → **Sync Project with Gradle Files**

### 4. Executar
Compile e rode no emulador ou dispositivo físico.

Detalhes de Firebase (Auth, Firestore, Storage e regras) e da chave do Maps: `docs/SETUP_FIREBASE.md` e `docs/GOOGLE_MAPS.md`.

## Stack
Kotlin, CameraX, Maps SDK, FusedLocation, Firebase (Auth, Firestore, Storage)

## Entrega
Trabalho individual — disciplina de programação mobile (prazo 05/12/2026).
