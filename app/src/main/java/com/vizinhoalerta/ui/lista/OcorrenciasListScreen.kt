package com.vizinhoalerta.ui.lista

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.model.Ocorrencia
import com.vizinhoalerta.ui.mapa.FiltroOcorrencias
import com.vizinhoalerta.ui.components.Avatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcorrenciasListScreen(
    ocorrenciaRepository: OcorrenciaRepository,
    filtro: FiltroOcorrencias,
    authRepository: AuthRepository,
    onVoltar: () -> Unit,
    onOcorrenciaClick: (String) -> Unit
) {
    val ocorrencias by ocorrenciaRepository.observarOcorrencias()
        .collectAsState(initial = emptyList())

    val visiveis = ocorrencias.filter(filtro::match)

    val fotoCache = remember { mutableStateMapOf<String, String?>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ocorrências", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (visiveis.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma ocorrência registrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visiveis) { item ->
                    val isResolvido = item.status == "resolvido"
                    LaunchedEffect(item.userId) {
                        if (!fotoCache.contains(item.userId)) {
                            val usuario = authRepository.getDadosUsuario(item.userId)
                            fotoCache[item.userId] = usuario?.fotoUrl?.takeIf { it.isNotBlank() }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOcorrenciaClick(item.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Avatar(
                                        nome = item.usuarioNome.ifBlank { "?" },
                                        fotoUrl = fotoCache[item.userId],
                                        tamanho = 32.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = item.tipo,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isResolvido) "Resolvido" else "Aberto",
                                    color = if (isResolvido) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            Text(text = "${item.bairro}, ${item.cidade}", style = MaterialTheme.typography.bodyMedium)
                            if (item.descricao.isNotBlank()) {
                                Text(text = item.descricao, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}