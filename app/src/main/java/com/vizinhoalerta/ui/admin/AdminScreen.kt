package com.vizinhoalerta.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.model.Ocorrencia
import com.vizinhoalerta.model.Ocorrencia.Companion.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    ocorrenciaRepository: OcorrenciaRepository,
    onVoltar: () -> Unit
) {
    var ocorrencias by remember { mutableStateOf<List<Ocorrencia>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            ocorrencias = ocorrenciaRepository.listar() // Lista síncrona para admin
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel Admin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ocorrencias) { ocorrencia ->
                AdminOcorrenciaRow(
                    ocorrencia = ocorrencia,
                    onAtualizar = { ocorr ->
                        ocorrenciaRepository.atualizarSituacao(ocorr.id, ocorr.situacao)
                    },
                    onExcluir = {
                        ocorrenciaRepository.deletar(ocorrencia.id)
                        // Recarrega lista - simplificação
                    }
                )
            }
        }
    }
}

@Composable
fun AdminOcorrenciaRow(
    ocorrencia: Ocorrencia,
    onAtualizar: (Ocorrencia) -> Unit,
    onExcluir: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ocorrencia.tipo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Chip(
                    label = { Text(ocorrencia.situacao) },
                    colors = ChipDefaults.chipColors(
                        containerColor = when (ocorrencia.situacao) {
                            SITUACAO_REGISTRADO -> Color(0xFFFFEB3B)
                            SITUACAO_EM_VERIFICACAO -> Color(0xFFFFC107)
                            SITUACAO_CONFIRMADO -> Color(0xFF4CAF50)
                            SITUACAO_RESOLVIDO -> Color(0xFF8BC34A)
                            SITUACAO_REJEITADO -> Color(0xFFF44336)
                            else -> Color(0xFFBDBDBD)
                        },
                        labelColor = Color.Black
                    )
                )
            }

            Text("${ocorrencia.bairro}, ${ocorrencia.cidade}", style = MaterialTheme.typography.bodyMedium)
            Text(ocorrencia.descricao, style = MaterialTheme.typography.bodySmall, maxLines = 2)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Alterar situação:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAtualizar(ocorrencia.copy(situacao = SITUACAO_REGISTRADO)) }) {
                    Text("Registrado", modifier = Modifier.alpha(0.7f))
                }
                TextButton(onClick = { onAtualizar(ocorrencia.copy(situacao = SITUACAO_EM_VERIFICACAO)) }) {
                    Text("Verif.", modifier = Modifier.alpha(0.7f))
                }
                TextButton(onClick = { onAtualizar(ocorrencia.copy(situacao = SITUACAO_CONFIRMADO)) }) {
                    Text("Confirmado", modifier = Modifier.alpha(0.7f))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAtualizar(ocorrencia.copy(situacao = SITUACAO_RESOLVIDO, status = "resolvido")) }) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resolver")
                }
                TextButton(onClick = { onAtualizar(ocorrencia.copy(situacao = SITUACAO_REJEITADO)) }) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rejeitar", color = Color.Red)
                }
            }
        }
    }
}