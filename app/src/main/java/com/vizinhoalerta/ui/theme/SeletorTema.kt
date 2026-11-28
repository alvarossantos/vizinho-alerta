package com.vizinhoalerta.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private fun rotuloTema(preferencia: PreferenciaTema): String = when (preferencia) {
    PreferenciaTema.SISTEMA -> "Seguir o sistema"
    PreferenciaTema.CLARO -> "Claro"
    PreferenciaTema.ESCURO -> "Escuro"
}

@Composable
fun SeletorTema(
    preferencia: PreferenciaTema,
    onSelecionar: (PreferenciaTema) -> Unit,
    modifier: Modifier = Modifier
) {
    var aberto by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { aberto = true }, modifier = modifier) {
        Text("Tema (${rotuloTema(preferencia)})")
    }

    if (aberto) {
        AlertDialog(
            onDismissRequest = { aberto = false },
            title = { Text("Tema") },
            text = {
                Column {
                    PreferenciaTema.entries.forEach { opcao ->
                        TextButton(onClick = {
                            onSelecionar(opcao)
                            aberto = false
                        }) {
                            Text(rotuloTema(opcao))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { aberto = false }) { Text("Cancelar") }
            }
        )
    }
}
