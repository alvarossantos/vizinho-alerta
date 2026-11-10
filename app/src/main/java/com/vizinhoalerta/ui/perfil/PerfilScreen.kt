package com.vizinhoalerta.ui.perfil

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vizinhoalerta.data.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    authRepository: AuthRepository,
    onLogoutClick: () -> Unit,
    onVoltar: () -> Unit
) {
    val usuario = remember { authRepository.getDadosUsuario(authRepository.currentUser?.uid ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = usuario?.nome ?: "Usuário",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = usuario?.email ?: "Email não disponível",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${usuario?.bairro}, ${usuario?.cidade}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = {
                    onLogoutClick()
                    onVoltar()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sair", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}