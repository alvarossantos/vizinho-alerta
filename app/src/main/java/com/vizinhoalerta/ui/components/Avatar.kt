package com.vizinhoalerta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.vizinhoalerta.R

@Composable
fun Avatar(
    nome: String,
    fotoUrl: String? = null,
    tamanho: androidx.compose.ui.unit.Dp = 32.dp,
    editavel: Boolean = false,
    onEditarClick: () -> Unit = {}
) {
    val inicial = if (nome.isNotBlank()) nome.trim().first().uppercase() else "?"
    val temFoto = fotoUrl?.isNotBlank() == true

    Box(
        modifier = Modifier.size(tamanho),
        contentAlignment = Alignment.Center
    ) {
        if (temFoto) {
            AsyncImage(
                model = fotoUrl!!,
                contentDescription = "Foto de $nome",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_usuario_padrao),
                    contentDescription = "Sem foto de perfil",
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize(0.62f)
                )
            }
        }

        if (editavel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.BottomEnd
            ) {
                IconButton(
                    onClick = onEditarClick,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Editar foto",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}