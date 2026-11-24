package com.vizinhoalerta.ui.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.model.Ocorrencia
import com.vizinhoalerta.service.LocalizacaoService
import com.vizinhoalerta.ui.theme.*
import com.vizinhoalerta.ui.components.Avatar
import com.vizinhoalerta.ui.components.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    ocorrenciaRepository: OcorrenciaRepository,
    authRepository: AuthRepository,
    filtro: FiltroOcorrencias,
    onFiltroChange: (FiltroOcorrencias) -> Unit,
    onNovaOcorrenciaClick: () -> Unit,
    onOcorrenciaClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onListaClick: () -> Unit,
    onProfileClick: () -> Unit,
    onConfirmLocation: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val ocorrencias by ocorrenciaRepository.observarOcorrencias()
        .collectAsState(initial = emptyList())

    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var loadingLocation by remember { mutableStateOf(true) }
    var centralizar by remember { mutableIntStateOf(0) }
    var selecionada by remember { mutableStateOf<Ocorrencia?>(null) }

    // Cache de fotos do autor (mesmo padrão da lista)
    val fotoCache = remember { mutableStateMapOf<String, String?>() } as MutableMap<String, String?>

    LaunchedEffect(Unit) {
        LocalizacaoService.obterLocalizacaoAtualLatLng(
            context = context,
            onSuccess = { lat, lng ->
                userLocation = lat to lng
                loadingLocation = false
            },
            onFailure = {
                loadingLocation = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vizinho Alerta", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onListaClick) {
                        Icon(Icons.Default.List, contentDescription = "Lista")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB circular 64dp laranja
            FloatingActionButton(
                onClick = {
                    if (authRepository.currentUid != null) {
                        onNovaOcorrenciaClick()
                    }
                },
                containerColor = Laranja,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(end = 16.dp, bottom = if (selecionada != null) 148.dp else 24.dp)
                    .size(64.dp)
            ) {
                Icon(Icons.Default.Add, "Nova ocorrência", Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Map
            if (loadingLocation) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val ocorrenciasFiltradas = ocorrencias.filter(filtro::match)
                MapaBackend.Content(
                    ocorrencias = ocorrenciasFiltradas,
                    userLocation = userLocation,
                    centralizar = centralizar,
                    onOcorrenciaClick = { id ->
                        val ocorrencia = ocorrencias.find { it.id == id }
                        if (ocorrencia != null) {
                            selecionada = ocorrencia
                        }
                        onOcorrenciaClick(id)
                    }
                )
            }

            // My location button
            if (userLocation != null) {
                FloatingActionButton(
                    onClick = { centralizar++ },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (selecionada != null) 148.dp else 16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Ir para mim")
                }
            }

            // Overlay flutuante
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Busca flutuante
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 6.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Slate)
                        TextField(
                            value = filtro.busca,
                            onValueChange = { onFiltroChange(filtro.copy(busca = it)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Buscar endereço ou ocorrência", color = Slate) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                        // Sem ícone Tune (não há filtros avançados)
                    }
                }

                // Chips de categoria (5: Todas + 4 categorias)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Todas
                    item {
                        ChipFiltro(
                            rotulo = "Todas",
                            ponto = null,
                            ativo = filtro.categoria == CategoriaFiltro.TODAS,
                            onClick = { onFiltroChange(filtro.copy(categoria = CategoriaFiltro.TODAS)) }
                        )
                    }
                    // Segurança = Vermelho
                    item {
                        ChipFiltro(
                            rotulo = "Segurança",
                            ponto = Vermelho,
                            ativo = filtro.categoria == CategoriaFiltro.SEGURANCA,
                            onClick = { onFiltroChange(filtro.copy(categoria = CategoriaFiltro.SEGURANCA)) }
                        )
                    }
                    // Infra = Ambar
                    item {
                        ChipFiltro(
                            rotulo = "Infra",
                            ponto = Ambar,
                            ativo = filtro.categoria == CategoriaFiltro.INFRA,
                            onClick = { onFiltroChange(filtro.copy(categoria = CategoriaFiltro.INFRA)) }
                        )
                    }
                    // Suspeita = Azul
                    item {
                        ChipFiltro(
                            rotulo = "Suspeita",
                            ponto = Azul,
                            ativo = filtro.categoria == CategoriaFiltro.SUSPEITA,
                            onClick = { onFiltroChange(filtro.copy(categoria = CategoriaFiltro.SUSPEITA)) }
                        )
                    }
                }
            }

            // Cartão flutuante inferior
            if (selecionada != null) {
                CartaoOcorrencia(
                    ocorrencia = selecionada!!,
                    onClick = {
                        onOcorrenciaClick(selecionada!!.id)
                    },
                    authRepository = authRepository,
                    fotoCache = fotoCache,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ChipFiltro(
    rotulo: String,
    ponto: Color?,
    ativo: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        shadowElevation = 3.dp,
        color = if (ativo) Marinho else Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (ponto != null) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(ponto))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = rotulo,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (ativo) Color.White else Marinho
            )
        }
    }
}

@Composable
fun CartaoOcorrencia(
    ocorrencia: Ocorrencia,
    onClick: () -> Unit,
    authRepository: AuthRepository,
    fotoCache: MutableMap<String, String?>,
    modifier: Modifier = Modifier
) {
    val categoria = getCategoriaInfo(ocorrencia.tipo)
    var autorFoto by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(ocorrencia.userId) {
        if (!fotoCache.containsKey(ocorrencia.userId)) {
            val usuario = authRepository.getDadosUsuario(ocorrencia.userId)
            val fotoUrl = usuario?.fotoUrl
            val foto = if (fotoUrl != null && fotoUrl.isNotBlank()) fotoUrl else null
            fotoCache[ocorrencia.userId] = foto
            autorFoto = foto
        } else {
            autorFoto = fotoCache[ocorrencia.userId] ?: null
        }
    }

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 10.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bloco 64dp com fundo da categoria e ícone
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(categoria.corClara),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoria.icone, contentDescription = null, tint = categoria.cor)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ocorrencia.tipo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Marinho,
                    maxLines = 1
                )
                Text(
                    text = ocorrencia.descricao,
                    color = Slate,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = "${ocorrencia.bairro}, ${ocorrencia.cidade}",
                    color = Slate,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                StatusBadge(
                    texto = if (ocorrencia.status == "resolvido") "RESOLVIDO" else "ABERTO",
                    cor = if (ocorrencia.status == "resolvido") Verde else Vermelho
                )
            }
            Avatar(
                nome = ocorrencia.usuarioNome.ifBlank { "?" },
                fotoUrl = autorFoto,
                tamanho = 40.dp
            )
        }
    }
}

private data class CategoriaInfo(
    val cor: Color,
    val corClara: Color,
    val icone: androidx.compose.ui.graphics.vector.ImageVector
)

private fun getCategoriaInfo(tipo: String): CategoriaInfo = when (tipo) {
    "Iluminação danificada", "Outra" -> CategoriaInfo(Ambar, AmbarClaro, Icons.Default.Lightbulb)
    "Suspeita" -> CategoriaInfo(Azul, AzulClaro, Icons.Default.Visibility)
    "Briga", "Vandalismo", "Animal perigoso" -> CategoriaInfo(Vermelho, VermelhoClaro, Icons.Default.Shield)
    else -> CategoriaInfo(Slate, Borda, Icons.Default.Help)
}