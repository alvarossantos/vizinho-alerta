# Google Maps API (chave)

## 1. Criar chave
- [ ] Google Cloud Console: https://console.cloud.google.com
- [ ] Criar projeto (pode ser o mesmo do Firebase) → APIs e serviços → Credenciais
- [ ] Criar chave de API → Ativar "Maps SDK for Android"

## 2. Colocar no app
- [ ] Abrir `app/src/main/AndroidManifest.xml`
- [ ] Substituir `YOUR_GOOGLE_MAPS_API_KEY_HERE` pela chave criada

## 3. Restrições recomendadas
- [ ] Restringir chave para Android (`com.vizinhoalerta`)
- [ ] Restringir para API "Maps SDK for Android"
