# Setup Firebase (passo a passo)

> Não é possível criar o projeto Firebase automaticamente sem seu login Google.
> Siga exatamente esta ordem:

## 1. Criar projeto no Firebase
- [ ] Console: https://console.firebase.google.com → Adicionar projeto
- [ ] Nome sugerido: `vizinho-alerta`
- [ ] Desativar Google Analytics (opcional)

## 2. Registrar o app Android
- [ ] Firebase → Visão geral → ícone Android
- [ ] Package name: `com.vizinhoalerta`
- [ ] Baixar `google-services.json` → colocar em `app/google-services.json`
- [ ] **Nunca commitar** esse arquivo (já está no `.gitignore`)

## 3. Ativar serviços
- [ ] Authentication → Método: e-mail/senha
- [ ] Firestore Database → Banco novo → coleções `usuarios` e `ocorrencias`
- [ ] Storage → pastas `fotos/` e `audios/`

## 4. Aplicar regras
- [ ] Firestore → Regras → colar conteúdo de `firestore.rules` → Publicar
- [ ] Storage → Regras → colar conteúdo de `storage.rules` → Publicar

## 5. Testar
- [ ] Abrir projeto no Android Studio → Gradle Sync
- [ ] Rodar em dispositivo físico
- [ ] Cadastrar 1 usuário → criar 1 ocorrência → conferir no Firestore
