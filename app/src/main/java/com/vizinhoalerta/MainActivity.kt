package com.vizinhoalerta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.ui.detalhes.DetalhesScreen
import com.vizinhoalerta.ui.lista.OcorrenciasListScreen
import com.vizinhoalerta.ui.login.LoginScreen
import com.vizinhoalerta.ui.mapa.MapaScreen
import com.vizinhoalerta.ui.perfil.PerfilScreen
import com.vizinhoalerta.ui.reportar.ReportarScreen
import com.vizinhoalerta.ui.welcome.WelcomeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {

    private val permissoesNecessarias = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val launcherPermissoes = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o MapLibre SDK
        MapLibre.getInstance(this)

        solicitarPermissoesSeNecessario()

        val authRepository = AuthRepository()
        val ocorrenciaRepository = OcorrenciaRepository()

        // Insere ocorrências de teste caso o banco esteja vazio
        CoroutineScope(Dispatchers.IO).launch {
            ocorrenciaRepository.seedOcorrenciasDeTeste()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VizinhoAlertaApp(
                        authRepository = authRepository,
                        ocorrenciaRepository = ocorrenciaRepository
                    )
                }
            }
        }
    }

    private fun solicitarPermissoesSeNecessario() {
        val faltantes = permissoesNecessarias.filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
        }
        if (faltantes.isNotEmpty()) {
            launcherPermissoes.launch(faltantes.toTypedArray())
        }
    }
}

@Composable
fun VizinhoAlertaApp(
    authRepository: AuthRepository,
    ocorrenciaRepository: OcorrenciaRepository
) {
    val navController = rememberNavController()

    // Se o usuário já estiver logado (sessão em cache), vai direto para o mapa
    val startDestination = remember {
        if (authRepository.currentUser != null) "mapa" else "welcome"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("welcome") {
            WelcomeScreen(
                onExplorarMapa = {
                    navController.navigate("mapa")
                },
                onEntrarClick = {
                    navController.navigate("login")
                }
            )
        }

        composable("login") {
            LoginScreen(
                authRepository = authRepository,
                onLoginSucesso = {
                    navController.navigate("mapa") {
                        popUpTo("welcome") { inclusive = true }
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("mapa") {
            val isLoggedIn = authRepository.currentUser != null
            MapaScreen(
                ocorrenciaRepository = ocorrenciaRepository,
                isLoggedIn = isLoggedIn,
                onNovaOcorrenciaClick = {
                    if (authRepository.currentUser != null) {
                        navController.navigate("reportar")
                    } else {
                        navController.navigate("login")
                    }
                },
                onOcorrenciaClick = { ocorrenciaId ->
                    navController.navigate("detalhes/$ocorrenciaId")
                },
                onListaClick = {
                    navController.navigate("lista_ocorrencias")
                },
                onProfileClick = {
                    navController.navigate("perfil")
                },
                onLoginClick = {
                    navController.navigate("login")
                },
                onLogoutClick = {
                    authRepository.logout()
                    navController.navigate("welcome") {
                        popUpTo("mapa") { inclusive = true }
                    }
                },
                onConfirmLocation = { lat, lng ->
                    if (authRepository.currentUser != null) {
                        navController.navigate("reportar?lat=$lat&lng=$lng")
                    } else {
                        navController.navigate("login")
                    }
                },
                onUserLocationReady = { _, _ -> }
            )
        }

        composable("lista_ocorrencias") {
            val isLoggedIn = authRepository.currentUser != null
            OcorrenciasListScreen(
                ocorrenciaRepository = ocorrenciaRepository,
                authRepository = authRepository,
                isLoggedIn = isLoggedIn,
                onOcorrenciaClick = { ocorrenciaId ->
                    navController.navigate("detalhes/$ocorrenciaId")
                },
                onNavigateMapa = {
                    navController.navigate("mapa") {
                        popUpTo("mapa") { inclusive = true }
                    }
                },
                onNavigateLista = {},
                onNavigatePerfil = {
                    navController.navigate("perfil")
                },
                onNavigateLogin = {
                    navController.navigate("login")
                },
                onLogout = {
                    authRepository.logout()
                    navController.navigate("welcome") {
                        popUpTo("mapa") { inclusive = true }
                    }
                },
                onVoltar = {
                    navController.popBackStack()
                }
            )
        }

        composable("perfil") {
            PerfilScreen(
                authRepository = authRepository,
                onVoltar = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "reportar?lat={lat}&lng={lng}",
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType; defaultValue = 0f },
                navArgument("lng") { type = NavType.FloatType; defaultValue = 0f }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 0.0
            ReportarScreen(
                ocorrenciaRepository = ocorrenciaRepository,
                authRepository = authRepository,
                initialLat = lat,
                initialLng = lng,
                onSucesso = { navController.popBackStack() },
                onVoltar = { navController.popBackStack() }
            )
        }

        composable(
            route = "detalhes/{ocorrenciaId}",
            arguments = listOf(navArgument("ocorrenciaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val ocorrenciaId = backStackEntry.arguments?.getString("ocorrenciaId") ?: ""
            DetalhesScreen(
                ocorrenciaId = ocorrenciaId,
                ocorrenciaRepository = ocorrenciaRepository,
                onVoltar = {
                    navController.popBackStack()
                }
            )
        }
    }
}
