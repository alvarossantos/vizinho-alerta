package com.vizinhoalerta.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNav(
    rotaAtual: String?,
    onMapa: () -> Unit,
    onAlertas: () -> Unit,
    onNova: () -> Unit,
    onPerfil: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Mapa",
                    tint = if (rotaAtual == "mapa") primary else onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Mapa", fontSize = 12.sp) },
            selected = rotaAtual == "mapa",
            onClick = onMapa
        )
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Alertas",
                    tint = if (rotaAtual == "lista_ocorrencias") primary else onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Alertas", fontSize = 12.sp) },
            selected = rotaAtual == "lista_ocorrencias",
            onClick = onAlertas
        )
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nova ocorrência",
                    tint = if (rotaAtual == "reportar") primary else onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Nova", fontSize = 12.sp) },
            selected = rotaAtual == "reportar",
            onClick = onNova
        )
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = if (rotaAtual == "perfil") primary else onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Perfil", fontSize = 12.sp) },
            selected = rotaAtual == "perfil",
            onClick = onPerfil
        )
    }
}