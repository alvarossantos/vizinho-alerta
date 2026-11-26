# Indicador de movimento na câmera com giroscópio — desenho curto

Data: 2026-11-26. Base: `main` @ `7ab5980`.

## Intenção

Cobrir o quinto recurso do enunciado com um uso real e demonstrável de `TYPE_GYROSCOPE` dentro de uma funcionalidade existente: **Nova Ocorrência → câmera**. A auditoria prévia mostrou zero APIs de sensor no app; `android:screenOrientation="fullSensor"` apenas delega a rotação ao sistema e não conta como leitura.

## Objetivos

- Mostrar `Segure o celular firme` na prévia da câmera quando houver rotação.
- Ocultar o aviso após estabilidade, sem piscar.
- Manter o botão de captura habilitado em todos os estados.
- Preservar prévia e captura em aparelhos sem giroscópio.
- Registrar o listener somente com a prévia visível e o app em primeiro plano, removendo-o ao fechar ou ao sair de `STARTED`.

## Não-objetivos

- Bloquear foto, criar tela nova ou adicionar dependência.
- Usar `TYPE_ROTATION_VECTOR` ou qualquer sensor derivado.
- Pedir permissão de sensor ou ler sensor em segundo plano.

## Anti-objetivos (falhas a evitar)

- Aviso oscilando por amostras limitrofe.
- Listener vazando após fechar a prévia ou sair de primeiro plano.
- Captura degradada quando `getDefaultSensor(TYPE_GYROSCOPE)` retornar `null`.

## Restrições

- `minSdk = 24`; Compose + CameraX já em `ReportarScreen.kt:204-249`.
- Padrão do repo: dispositivo em `app/src/main/java/com/vizinhoalerta/service/`, UI em `ui/`.
- SDK, Studio e `~/.gradle` foram removidos na limpeza anterior; reinstalação exige aprovação de sudo. `JAVA_HOME` está apontando para JDK 17 inexistente (JDK 27 ativo).
- Validação em aparelho físico depende de dispositivo conectado via `adb`; sem dispositivo, a evidência será estática + testes, e isso deve ser declarado.

## Abordagens

1. **Escolhida — `GiroscopioService` (object) + `DisposableEffect` na tela.** Segue `CameraService`/`AudioService`/`LocalizacaoService`. O serviço encapsula seleção do sensor, `SensorEventListener`, limiar e histerese; a tela controla register/unregister e desenha o overlay. Custo: um arquivo novo; ganho: regra isolada e testável por teste unitário puro.
2. **Listener inline em `ReportarScreen`.** Menor diff, porém mistura sensor em tela de 387 linhas e dificulta teste isolado.
3. **ViewModel/StateFlow.** Descartado: estado persistente para aviso que só existe com a prévia aberta é YAGNI.

## Decisões

- Amostragem: `SensorManager.SENSOR_DELAY_UI`.
- Limiar: `0,15 rad/s` em qualquer dos três eixos; aviso aparece imediatamente na primeira amostra acima dele.
- Histerese: esconder após `500 ms` contínuos abaixo do limiar; se voltar a girar antes disso, o intervalo recomeça do zero.
- Estado do aviso é limpo ao fechar a prévia e ao sair de `STARTED`, junto com `unregisterListener`.
- Sem sensor disponível: observação desabilitada, nenhum aviso, captura inalterada.
- Aviso é informativo: `FloatingActionButton` de captura permanece habilitado.
- TAG de Logcat para diagnóstico: `GiroscopioService`.

## Testes e aceite

- Unitário puro: acima do limiar → aviso; parada por menos de 500 ms → aviso permanece; 500 ms contínuos → esconde; novo giro antes dos 500 ms → contagem reinicia; ausência de sensor → desabilitado.
- Instrumentado/manual em aparelho: **Nova Ocorrência → Câmera**, girar → aviso visível; parar → aviso oculto; foto tirada nos dois estados; repetir em aparelho sem giroscópio (quando disponível).
- Filtro de Logcat sugerido: `adb logcat -s GiroscopioService:D`.

## Aprovação

Usuário aprovou a abordagem 1, os parâmetros de amostragem/limiar/histerese e o critério de aceite em 2026-11-26, antes da escrita deste registro.
