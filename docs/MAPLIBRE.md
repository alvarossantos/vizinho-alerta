# MapLibre (OpenStreetMap) - NÃO Precisa de API Key!

O app usa **MapLibre SDK** para exibir mapas de OpenStreetMap. Diferente do Google Maps, **não é necessário criar uma API Key**.

## Configuração automática

O projeto já está configurado para usar MapLibre. Não é preciso fazer mais nada.

## Dependência

```kotlin
implementation("org.maplibre.gl:android-sdk:13.7.1")
implementation("com.google.maps.android:maps-compose:2.13.0")
```

## Recursos de mapa

- **Cores dos marcadores:**
  - 🟢 Verde = ocorrência resolvida
  - 🔴 Vermelho = ocorrência aberta
  - 🔵 Azul = sua localização atual (Blue Dot)

- **Navegação:**
  - Botão FAB "Reportar" → abre formulário
  - Botão "Local Atual" → recente para sua posição
  - Clique em marker → detalhes da ocorrência

## Sobre o OpenStreetMap

- Mapas gratuitos e de código aberto
- Dados atualizados pela comunidade
- Sem limites de uso, sem cobrança
- Ideal para apps de cidades e problemas urbanos

## Personalização

Se quiser trocar o estilo do mapa:
- Edite `app/src/main/java/com/vizinhoalerta/ui/mapa/MapStyle.kt`
- Exemplos: `StreetsStyle`, `SatelliteStyle`, `DarkStyle`

## Troubleshooting

Se o mapa não carregar:
1. Verifique conexão com internet
2. Limpe a cache do app
3. Verifique se `INTERNET` permission está no Manifest