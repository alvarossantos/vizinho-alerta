#!/usr/bin/env python3
"""Popula os emuladores locais com usuario de teste e ocorrencias simuladas.

Uso: python3 scripts/seed_emulador.py
Idempotente: usa IDs fixos e pode rodar novamente apos reiniciar os emuladores.
"""

import json
import time
import urllib.error
import urllib.request

AUTH = "http://127.0.0.1:9099"
FIRESTORE = "http://127.0.0.1:8081"
PROJECT = "vizinho-alerta"
EMAIL = "teste@exemplo.com"
SENHA = "Teste@123"

OCORRENCIAS = [
    ("cassia_1", "Iluminação danificada", "Poste apagado na Avenida Presidente Kubitschek, Centro.", -20.58250, -46.92190, "Cássia", "Centro"),
    ("cassia_2", "Suspeita", "Movimento suspeito próximo à praça central.", -20.58520, -46.92050, "Cássia", "Centro"),
    ("passos_1", "Vandalismo", "Pichação em patrimônio público no centro.", -20.71600, -46.61500, "Passos", "Centro"),
    ("passos_2", "Animal perigoso", "Cachorro solto circulando pela região.", -20.71800, -46.61700, "Passos", "Jardim"),
]


def call(url, method="GET", body=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    cabecalhos = {"Content-Type": "application/json"}
    cabecalhos.update(headers or {})
    req = urllib.request.Request(url, data=data, headers=cabecalhos, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as resposta:
            conteudo = resposta.read()
            return resposta.status, (json.loads(conteudo) if conteudo else {})
    except urllib.error.HTTPError as erro:
        return erro.code, json.loads(erro.read() or b"{}")


def autenticar():
    url = f"{AUTH}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake"
    corpo = {"email": EMAIL, "password": SENHA, "returnSecureToken": True}
    status, resposta = call(url, "POST", corpo)
    if status == 200:
        return resposta

    if resposta.get("error", {}).get("message") != "EMAIL_NOT_FOUND":
        raise SystemExit(f"Falha no login: {status} {resposta}")

    status, resposta = call(
        f"{AUTH}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake",
        "POST",
        corpo,
    )
    if status != 200:
        raise SystemExit(f"Falha ao criar usuario: {status} {resposta}")
    print("Usuario de teste criado.")
    return resposta


def main():
    usuario = autenticar()
    uid, token = usuario["localId"], usuario["idToken"]
    agora = int(time.time() * 1000)

    for indice, (doc, tipo, descricao, lat, lng, cidade, bairro) in enumerate(OCORRENCIAS):
        campos = {
            "userId": {"stringValue": uid},
            "usuarioNome": {"stringValue": "Alvaro Teste"},
            "tipo": {"stringValue": tipo},
            "descricao": {"stringValue": descricao},
            "lat": {"doubleValue": lat},
            "lng": {"doubleValue": lng},
            "situacao": {"stringValue": "registrado"},
            "cidade": {"stringValue": cidade},
            "bairro": {"stringValue": bairro},
            "status": {"stringValue": "aberto"},
            "createdAt": {"integerValue": str(agora - indice * 3600000)},
        }
        caminho = f"{FIRESTORE}/v1/projects/{PROJECT}/databases/(default)/documents/ocorrencias/{doc}"
        autenticacao = {"Authorization": f"Bearer {token}"}
        status, _ = call(caminho, headers=autenticacao)
        if status == 200:
            print(f"  {doc}: ja existe")
            continue
        if status != 404:
            raise SystemExit(f"Falha ao consultar {doc}: {status}")

        status, resposta = call(
            f"{FIRESTORE}/v1/projects/{PROJECT}/databases/(default)/documents/ocorrencias"
            f"?documentId={doc}",
            "POST",
            {"fields": campos},
            autenticacao,
        )
        if status != 200:
            raise SystemExit(f"Falha ao gravar {doc}: {status} {resposta}")
        print(f"  {doc}: {cidade} - {tipo} criada")

    print(f"Pronto. {len(OCORRENCIAS)} ocorrencias simuladas em {PROJECT}.")


if __name__ == "__main__":
    main()
