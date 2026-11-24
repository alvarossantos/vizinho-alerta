package com.vizinhoalerta.data

internal const val MSG_GOOGLE_NAO_CONFIGURADO =
    "Login com Google exige Firebase configurado (google-services.json com OAuth Web)."

private const val MSG_CONFLITO_CONTAS =
    "Este e-mail já possui conta com senha. Entre com sua senha para continuar."

private val IDS_FICTICIOS = listOf("DUMMY", "PLACEHOLDER", "YOUR_", "example")
private val WEB_CLIENT_ID_REGEX = Regex("""^\d+-[A-Za-z0-9._-]+\.apps\.googleusercontent\.com$""")

internal fun webClientIdValido(id: String?): Boolean {
    if (id.isNullOrBlank()) return false
    if (IDS_FICTICIOS.any { id.contains(it, ignoreCase = true) }) return false
    if (id.startsWith("000000000000")) return false
    return WEB_CLIENT_ID_REGEX.matches(id)
}

internal fun mensagemConflitoConta(errorCode: String?, message: String?): String? =
    if (errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ||
        message?.contains("account-exists-with-different-credential") == true
    ) {
        MSG_CONFLITO_CONTAS
    } else {
        null
    }
