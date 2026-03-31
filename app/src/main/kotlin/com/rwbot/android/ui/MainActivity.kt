package com.rwbot.android.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rwbot.android.ui.nav.NavRoutes
import com.rwbot.android.ui.reviews.ReviewsScreen
import com.rwbot.android.ui.reviews.ReviewsViewModel
import com.rwbot.android.ui.settings.SettingsScreen
import com.rwbot.android.ui.settings.SettingsViewModel
import com.rwbot.android.ui.theme.RWBOTAndroidTheme
import com.rwbot.android.util.BadgeHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RWBOTAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainNav()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNav() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val reviewsViewModel: ReviewsViewModel = viewModel(activity)

    // Запрашиваем разрешение на уведомления, чтобы бейдж на иконке работал на Android 13+.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Бейдж показывает количество отзывов, которые еще требуют действия пользователя.
    LaunchedEffect(Unit) {
        reviewsViewModel.unansweredCountFlow.collect { count ->
            BadgeHelper.updateBadge(context, count)
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (currentRoute == NavRoutes.SETTINGS) "Настройки" else "Отзывы")
                },
                navigationIcon = {
                    if (currentRoute == NavRoutes.SETTINGS) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute != NavRoutes.SETTINGS) {
                        IconButton(onClick = { navController.navigate(NavRoutes.SETTINGS) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Открыть настройки"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        MainNavHost(
            navController = navController,
            padding = padding,
            reviewsViewModel = reviewsViewModel
        )
    }
}

@Composable
private fun MainNavHost(
    navController: NavHostController,
    padding: PaddingValues,
    reviewsViewModel: ReviewsViewModel
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.REVIEWS,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        composable(NavRoutes.REVIEWS) {
            ReviewsScreen(viewModel = reviewsViewModel)
        }
        composable(NavRoutes.SETTINGS) {
            val vm: SettingsViewModel = hiltViewModel()
            SettingsScreen(viewModel = vm)
        }
    }
}
