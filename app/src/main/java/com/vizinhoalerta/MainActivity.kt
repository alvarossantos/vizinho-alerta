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
import com.vizinhoalerta.ui.login.LoginScreen
import com.vizinhoalerta.ui.mapa.MapaScreen
import com.vizinhoalerta.ui.reportar.ReportarScreen

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

        solicitarPermissoesSeNecessario()

        val authRepository = AuthRepository()
        val ocorrenciaRepository = OcorrenciaRepository()

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
    val startDestination = remember {
        if (authRepository.currentUser != null) "mapa" else "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                authRepository = authRepository,
                onLoginSucesso = {
                    navController.navigate("mapa") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("mapa") {
            MapaScreen(
                ocorrenciaRepository = ocorrenciaRepository,
                onNovaOcorrenciaClick = {
                    navController.navigate("reportar")
                },
                onOcorrenciaClick = { ocorrenciaId ->
                    navController.navigate("detalhes/$ocorrenciaId")
                },
                onLogoutClick = {
                    authRepository.logout()
                    navController.navigate("login") {
                        popUpTo("mapa") { inclusive = true }
                    }
                }
            )
        }

        composable("reportar") {
            ReportarScreen(
                ocorrenciaRepository = ocorrenciaRepository,
                authRepository = authRepository,
                onSucesso = {
                    navController.popBackStack()
                },
                onVoltar = {
                    navController.popBackStack()
                }
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
