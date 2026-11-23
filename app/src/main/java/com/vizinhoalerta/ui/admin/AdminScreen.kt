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
import com.vizinhoalerta.model.Ocorrencia.Companion.SITUACAO_CONFIRMADO
import com.vizinhoalerta.model.Ocorrencia.Companion.SITUACAO_EM_VERIFICACAO
import com.vizinhoalerta.model.Ocorrencia.Companion.SITUACAO_REGISTRADO
import com.vizinhoalerta.model.Ocorrencia.Companion.SITUACAO_REJEITADO
import com.vizinhoalerta.model.Ocorrencia.Companion.SITUACAO_RESOLVIDO
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    ocorrenciaRepository: OcorrenciaRepository,
    onVoltar: () -> Unit
) {
    var ocorrencias by remember { mutableStateOf<List<Ocorrencia>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
            erro?.let { msg ->
                item {
                    Text(text = msg, color = MaterialTheme.colorScheme.error)
                }
            }
            items(ocorrencias) { ocorrencia ->
                AdminOcorrenciaRow(
                    ocorrencia = ocorrencia,
                    onAtualizar = { ocorr, situacao, status ->
                        scope.launch {
                            ocorrenciaRepository.atualizarSituacaoComStatus(ocorr.id, situacao, status)
                                .onSuccess {
                                    erro = null
                                    ocorrencias = ocorrencias.map {
                                        if (it.id == ocorr.id) it.copy(situacao = situacao, status = status) else it
                                    }
                                }
                                .onFailure { erro = it.localizedMessage }
                        }
                    },
                    onExcluir = {
                        scope.launch {
                            ocorrenciaRepository.deletarComMidia(ocorrencia.id)
                                .onSuccess {
                                    erro = null
                                    ocorrencias = ocorrencias.filterNot { it.id == ocorrencia.id }
                                }
                                .onFailure { erro = it.localizedMessage }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminOcorrenciaRow(
    ocorrencia: Ocorrencia,
    onAtualizar: (Ocorrencia, String, String) -> Unit,
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
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(ocorrencia.situacao) },
                    colors = FilterChipDefaults.filterChipColors(
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
                TextButton(onClick = { onAtualizar(ocorrencia, SITUACAO_REGISTRADO, "aberto") }) {
                    Text("Registrado", modifier = Modifier.alpha(0.7f))
                }
                TextButton(onClick = { onAtualizar(ocorrencia, SITUACAO_EM_VERIFICACAO, "aberto") }) {
                    Text("Verif.", modifier = Modifier.alpha(0.7f))
                }
                TextButton(onClick = { onAtualizar(ocorrencia, SITUACAO_CONFIRMADO, "aberto") }) {
                    Text("Confirmado", modifier = Modifier.alpha(0.7f))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onAtualizar(ocorrencia, SITUACAO_RESOLVIDO, "resolvido") }) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resolver")
                }
                TextButton(onClick = { onExcluir() }) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excluir", color = Color.Red)
                }
            }
        }
    }
}