package com.rwbot.android.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rwbot.android.ui.nav.NavRoutes
import com.rwbot.android.ui.reviews.ReviewDetailScreen
import com.rwbot.android.ui.reviews.ReviewDetailViewModel
import com.rwbot.android.ui.reviews.ReviewsScreen
import com.rwbot.android.ui.reviews.ReviewsViewModel
import com.rwbot.android.ui.settings.SettingsScreen
import com.rwbot.android.ui.settings.SettingsViewModel
import com.rwbot.android.ui.stats.StatsScreen
import com.rwbot.android.ui.stats.StatsViewModel
import com.rwbot.android.ui.theme.RWBOTAndroidTheme
import com.rwbot.android.util.BadgeHelper
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import com.rwbot.android.ui.moderation.ModerationScreen

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

@Composable
fun MainNav() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val reviewsViewModel: ReviewsViewModel = viewModel(activity)

    // Запрос разрешения на уведомления (Android 13+) для бейджа на иконке
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // Обновление бейджа при изменении количества неотвеченных отзывов
    LaunchedEffect(Unit) {
        reviewsViewModel.unansweredCountFlow.collect { count ->
            BadgeHelper.updateBadge(context, count)
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    NavRoutes.REVIEWS to "Отзывы",
                    NavRoutes.MODERATION to "Модерация",
                    NavRoutes.STATS to "Статистика",
                    NavRoutes.SETTINGS to "Настройки"
                ).forEach { (route, label) ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {},
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.REVIEWS,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable(NavRoutes.REVIEWS) {
                LaunchedEffect(Unit) { reviewsViewModel.setFilter(null) }
                ReviewsScreen(
                    viewModel = reviewsViewModel,
                    onReviewClick = { navController.navigate(NavRoutes.reviewDetail(it)) }
                )
            }
            composable(NavRoutes.MODERATION) {
                ModerationScreen(
                    viewModel = reviewsViewModel,
                    onReviewClick = { navController.navigate(NavRoutes.reviewDetail(it)) }
                )
            }
            composable(NavRoutes.STATS) {
                val vm: StatsViewModel = hiltViewModel()
                StatsScreen(viewModel = vm)
            }
            composable(NavRoutes.SETTINGS) {
                val vm: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = vm)
            }
            composable(
                NavRoutes.REVIEW_DETAIL,
                arguments = listOf(navArgument("reviewId") { type = androidx.navigation.NavType.StringType })
            ) {
                val vm: ReviewDetailViewModel = hiltViewModel()
                ReviewDetailScreen(viewModel = vm)
            }
        }
    }
}
