package com.rwbot.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val activity = LocalContext.current as ComponentActivity
    val reviewsViewModel: ReviewsViewModel = viewModel(activity)
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
                val vm: StatsViewModel = viewModel()
                StatsScreen(viewModel = vm)
            }
            composable(NavRoutes.SETTINGS) {
                val vm: SettingsViewModel = viewModel()
                SettingsScreen(viewModel = vm)
            }
            composable(
                NavRoutes.REVIEW_DETAIL,
                arguments = listOf(navArgument("reviewId") { type = androidx.navigation.NavType.StringType })
            ) {
                val vm: ReviewDetailViewModel = viewModel()
                ReviewDetailScreen(viewModel = vm)
            }
        }
    }
}
