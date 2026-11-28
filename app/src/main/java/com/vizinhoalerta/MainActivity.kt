package com.vizinhoalerta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vizinhoalerta.data.AuthRepository
import com.vizinhoalerta.data.BackendSelector
import com.vizinhoalerta.data.GoogleSignInHelper
import com.vizinhoalerta.data.OcorrenciaRepository
import com.vizinhoalerta.ui.admin.AdminScreen
import com.vizinhoalerta.ui.cadastro.CadastroScreen
import com.vizinhoalerta.ui.detalhes.DetalhesScreen
import com.vizinhoalerta.ui.lista.OcorrenciasListScreen
import com.vizinhoalerta.ui.login.LoginScreen
import com.vizinhoalerta.ui.mapa.MapaScreen
import com.vizinhoalerta.ui.mapa.FiltroOcorrencias
import com.vizinhoalerta.ui.perfil.PerfilScreen
import com.vizinhoalerta.ui.reportar.ReportarScreen
import com.vizinhoalerta.ui.welcome.WelcomeScreen
import com.vizinhoalerta.ui.components.BottomNav
import com.vizinhoalerta.ui.theme.PreferenciaTema
import com.vizinhoalerta.ui.theme.RepositorioTema
import com.vizinhoalerta.ui.theme.VizinhoAlertaTheme
import kotlinx.coroutines.launch

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

    private var preferenciaTema = mutableStateOf(PreferenciaTema.SISTEMA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferenciaTema.value = RepositorioTema.ler(this)
        solicitarPermissoesSeNecessario()

        setContent {
            VizinhoAlertaTheme(preferencia = preferenciaTema.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        lifecycleScope.launch {
            val configReal = !getString(R.string.google_api_key).contains("DUMMY", ignoreCase = true)
            val modo = BackendSelector.detectar(configReal = configReal)
            setContent {
                VizinhoAlertaTheme(preferencia = preferenciaTema.value) {
                    VizinhoAlertaApp(
                        modo = modo,
                        context = this@MainActivity,
                        preferenciaTema = preferenciaTema.value,
                        onPreferenciaTemaChange = {
                            RepositorioTema.salvar(this@MainActivity, it)
                            preferenciaTema.value = it
                        }
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
    modo: BackendSelector.Mode,
    context: Context,
    preferenciaTema: PreferenciaTema,
    onPreferenciaTemaChange: (PreferenciaTema) -> Unit
) {
    val demo = modo == BackendSelector.Mode.DEMO
    val authRepository = remember(modo) { AuthRepository(demo = demo, context = context) }
    val ocorrenciaRepository = remember(modo) { OcorrenciaRepository(demo = demo, authRepository = authRepository) }
    val googleSignIn = remember(context) { GoogleSignInHelper(context) }
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var completarPerfil by remember { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = completarPerfil) { }

    LaunchedEffect(modo) {
        val uid = authRepository.currentUid
        val destino = when {
            uid == null -> "welcome"
            authRepository.precisaCompletarPerfil(uid) -> "perfil"
            else -> "mapa"
        }
        completarPerfil = (destino == "perfil")
        startDestination = destino
    }

    fun irParaDestino(incompleto: Boolean) {
        completarPerfil = incompleto
        navController.navigate(if (incompleto) "perfil" else "mapa") {
            popUpTo("welcome") { inclusive = true }
        }
    }

    fun sair() {
        val uid = authRepository.currentUid
        uid?.let { authRepository.limparOnboardingPendente(it) }
        authRepository.logout()
        scope.launch { googleSignIn.clearCredentialState() }
        completarPerfil = false
        navController.navigate("welcome") {
            popUpTo(0) { inclusive = true }
        }
    }

    val destinoInicial = startDestination

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val rotasSemBottomNav = setOf("welcome", "login", "cadastro") +
        if (completarPerfil) setOf("perfil", "admin") else emptySet()
    val mostrarBottomNav = currentRoute?.let { route ->
        rotasSemBottomNav.none { route.startsWith(it) }
    } ?: false

    val bottomBarContent: @Composable () -> Unit = if (mostrarBottomNav) {
        { BottomNav(
            rotaAtual = currentRoute,
            onMapa = { navController.navigate("mapa") { launchSingleTop = true; popUpTo("mapa") { inclusive = false } } },
            onAlertas = { navController.navigate("lista_ocorrencias") { launchSingleTop = true; popUpTo("lista_ocorrencias") { inclusive = false } } },
            onNova = { navController.navigate("reportar") { launchSingleTop = true; popUpTo("reportar") { inclusive = false } } },
            onPerfil = { navController.navigate("perfil") { launchSingleTop = true; popUpTo("perfil") { inclusive = false } } }
        ) }
    } else {
        { /* empty */ }
    }

    var filtro by remember { mutableStateOf(FiltroOcorrencias()) }

    Scaffold(
        bottomBar = bottomBarContent
    ) { innerPadding ->
        if (destinoInicial == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            NavHost(
                navController = navController,
                startDestination = destinoInicial,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("welcome") {
                    WelcomeScreen(
                        onLoginClick = { navController.navigate("login") },
                        onCadastrarClick = { navController.navigate("cadastro") }
                    )
                }

                composable("login") {
                    LoginScreen(
                        authRepository = authRepository,
                        modoDemo = demo,
                        googleSignIn = googleSignIn,
                        onGoogleSucesso = { incompleto -> irParaDestino(incompleto) },
                        onLoginSucesso = {
                            scope.launch {
                                val incompleto = authRepository.currentUid?.let {
                                    authRepository.precisaCompletarPerfil(it)
                                } ?: false
                                irParaDestino(incompleto)
                            }
                        },
                        onCadastrarClick = { navController.navigate("cadastro") }
                    )
                }

                composable("cadastro") {
                    CadastroScreen(
                        authRepository = authRepository,
                        modoDemo = demo,
                        googleSignIn = googleSignIn,
                        onGoogleSucesso = { incompleto -> irParaDestino(incompleto) },
                        onCadastroSucesso = {
                            scope.launch {
                                val incompleto = authRepository.currentUid?.let {
                                    authRepository.precisaCompletarPerfil(it)
                                } ?: false
                                irParaDestino(incompleto)
                            }
                        },
                        onLoginClick = { navController.navigate("login") }
                    )
                }

                composable("mapa") {
                    MapaScreen(
                        ocorrenciaRepository = ocorrenciaRepository,
                        authRepository = authRepository,
                        filtro = filtro,
                        onFiltroChange = { filtro = it },
                        onNovaOcorrenciaClick = { navController.navigate("reportar") },
                        onOcorrenciaClick = { id -> navController.navigate("detalhes/$id") },
                        onLogoutClick = { sair() },
                        onListaClick = { navController.navigate("lista_ocorrencias") },
                        onProfileClick = { navController.navigate("perfil") },
                        onConfirmLocation = { lat, lng -> /* handle */ }
                    )
                }

                composable("reportar") {
                    ReportarScreen(
                        ocorrenciaRepository = ocorrenciaRepository,
                        authRepository = authRepository,
                        onSucesso = { navController.popBackStack() },
                        onVoltar = { navController.popBackStack() }
                    )
                }

                composable("lista_ocorrencias") {
                    OcorrenciasListScreen(
                        ocorrenciaRepository = ocorrenciaRepository,
                        filtro = filtro,
                        authRepository = authRepository,
                        onVoltar = { navController.popBackStack() },
                        onOcorrenciaClick = { id -> navController.navigate("detalhes/$id") }
                    )
                }

                composable("perfil") {
                    PerfilScreen(
                        authRepository = authRepository,
                        onLogoutClick = { sair() },
                        onAdminClick = { navController.navigate("admin") },
                        onVoltar = { navController.popBackStack() },
                        completarPerfil = completarPerfil,
                        onPerfilConcluido = {
                            completarPerfil = false
                            navController.navigate("mapa") {
                                popUpTo("welcome") { inclusive = true }
                            }
                        },
                        preferenciaTema = preferenciaTema,
                        onPreferenciaTemaChange = onPreferenciaTemaChange
                    )
                }

                composable(
                    route = "detalhes/{ocorrenciaId}",
                    arguments = listOf(navArgument("ocorrenciaId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("ocorrenciaId") ?: ""
                    DetalhesScreen(
                        ocorrenciaId = id,
                        ocorrenciaRepository = ocorrenciaRepository,
                        authRepository = authRepository,
                        onVoltar = { navController.popBackStack() }
                    )
                }

                composable("admin") {
                    var permitido by remember { mutableStateOf<Boolean?>(null) }
                    LaunchedEffect(Unit) { permitido = authRepository.isAdmin() }
                    when (permitido) {
                        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        true -> AdminScreen(
                            ocorrenciaRepository = ocorrenciaRepository,
                            authRepository = authRepository,
                            onVoltar = { navController.popBackStack() }
                        )
                        false -> Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Acesso negado")
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.material3.Button(onClick = { navController.popBackStack() }) { Text("Voltar") }
                        }
                    }
                }
            }
        }
    }
}