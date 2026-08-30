package com.compresso.app.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.compresso.app.ui.screens.home.HomeScreen
import com.compresso.app.ui.screens.home.HomeViewModel
import com.compresso.app.ui.screens.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun CompressoNavHost(navController: NavHostController = rememberNavController()) {
    val activity = LocalContext.current as ComponentActivity
    val homeViewModel: HomeViewModel = viewModel(viewModelStoreOwner = activity)

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInHorizontally(animationSpec = tween(320)) { it / 3 } + fadeIn(tween(320))
        },
        exitTransition = {
            fadeOut(tween(160))
        },
        popEnterTransition = {
            fadeIn(tween(220))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(320)) { it / 3 } + fadeOut(tween(220))
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = homeViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
