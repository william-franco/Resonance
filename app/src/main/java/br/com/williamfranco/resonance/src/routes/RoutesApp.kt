package br.com.williamfranco.resonance.src.routes

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.williamfranco.resonance.src.design.components.Dimens
import br.com.williamfranco.resonance.src.features.library.routes.CollectionRoute
import br.com.williamfranco.resonance.src.features.library.routes.LibraryRoute
import br.com.williamfranco.resonance.src.features.player.routes.PlayerRoute
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModel
import br.com.williamfranco.resonance.src.features.player.view_models.PlayerViewModelImpl
import br.com.williamfranco.resonance.src.features.settings.routes.SettingsRoute
import org.koin.androidx.compose.koinViewModel

@Composable
fun RoutesApp() {
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    val playerViewModel: PlayerViewModel = koinViewModel<PlayerViewModelImpl>(viewModelStoreOwner = activity)
    val playbackState by playerViewModel.state.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val onLibrary = backStackEntry?.destination?.route == Routes.LIBRARY

    // O mini-player flutua acima da barra de abas na biblioteca e rente à borda nas demais telas.
    val playerBottomInset = if (onLibrary) Dimens.BottomBarHeight else 0.dp
    val contentBottomInset = if (playbackState.currentSong != null) Dimens.MiniPlayerHeight + 12.dp else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            composable(Routes.LIBRARY) {
                LibraryRoute(
                    bottomPadding = contentBottomInset,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenCollection = { type, key ->
                        navController.navigate(Routes.collection(type, key))
                    },
                )
            }

            composable(
                route = Routes.COLLECTION_PATTERN,
                arguments = listOf(
                    navArgument(Routes.ARG_TYPE) { type = NavType.StringType },
                    navArgument(Routes.ARG_KEY) { type = NavType.StringType },
                ),
            ) { entry ->
                val type = entry.arguments
                    ?.getString(Routes.ARG_TYPE)
                    ?.let { name -> CollectionType.entries.firstOrNull { it.name == name } }
                    ?: CollectionType.ALBUM
                val key = entry.arguments?.getString(Routes.ARG_KEY).orEmpty()

                CollectionRoute(
                    type = type,
                    collectionKey = key,
                    bottomPadding = contentBottomInset,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsRoute(onBack = { navController.popBackStack() })
            }
        }

        PlayerRoute(bottomPadding = playerBottomInset)
    }
}
