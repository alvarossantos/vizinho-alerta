# Vizinho Alerta

App Android (Kotlin) de segurança comunitária para reportar ocorrências com foto, áudio e GPS, usando câmera, microfone, GPS e internet.

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
- Cadastro/login
- Mapa de ocorrências em tempo real
- Nova ocorrência: foto + áudio + GPS + categoria
- Feed de ocorrências próximas
- Detalhes: foto, áudio, distância e status (aberto/resolvido)

## Como rodar
1. Abrir projeto no Android Studio
2. Configurar `google-services.json` do Firebase
3. Adicionar chave do Google Maps no `AndroidManifest.xml`
4. Executar em emulador ou dispositivo físico

## Stack
Kotlin, CameraX, Maps SDK, FusedLocation, Firebase (Auth, Firestore, Storage)

## Entrega
Trabalho individual — disciplina de programação mobile (prazo 05/12/2026).
