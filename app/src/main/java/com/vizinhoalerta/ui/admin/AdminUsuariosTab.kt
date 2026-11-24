package com.vizinhoalerta.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.model.Usuario
import com.vizinhoalerta.ui.components.Avatar

@Composable
fun AdminUsuariosTab(authRepository: AuthRepository) {
    var usuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { authRepository.listarUsuarios() }
            .onSuccess { usuarios = it }
            .onFailure { erro = it.localizedMessage }
        carregando = false
    }

    when {
        carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        erro != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(erro!!, color = MaterialTheme.colorScheme.error)
        }
        usuarios.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum usuário.")
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(usuarios) { usuario ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(
                            nome = usuario.nome,
                            fotoUrl = usuario.fotoUrl?.takeIf { it.isNotBlank() },
                            tamanho = 40.dp
                        )
                        Column {
                            Text(usuario.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(usuario.email, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Função: ${if (usuario.isAdmin) "admin" else "user"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}