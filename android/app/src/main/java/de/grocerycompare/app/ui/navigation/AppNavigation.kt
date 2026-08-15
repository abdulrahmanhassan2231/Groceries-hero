package de.grocerycompare.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.grocerycompare.app.R
import de.grocerycompare.app.ui.list.ShoppingListScreen
import de.grocerycompare.app.ui.search.SearchScreen
import de.grocerycompare.app.ui.watched.WatchedScreen

private sealed class Dest(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Search : Dest("search", R.string.tab_search, Icons.Filled.Search)
    data object ListMode : Dest("list", R.string.tab_list, Icons.AutoMirrored.Filled.ListAlt)
    data object Watched : Dest("watched", R.string.tab_watched, Icons.Filled.FavoriteBorder)
}

private val TABS = listOf(Dest.Search, Dest.ListMode, Dest.Watched)

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEach { dest ->
                    val selected = current?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(dest.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(stringResource(dest.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Dest.Search.route,
            modifier = Modifier,
            enterTransition = {
                slideInHorizontally(tween(280)) { it / 6 } + fadeIn(tween(280))
            },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = {
                slideInHorizontally(tween(280)) { -it / 6 } + fadeIn(tween(280))
            },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable(Dest.Search.route) { SearchScreen(Modifier.padding(padding)) }
            composable(Dest.ListMode.route) { ShoppingListScreen(Modifier.padding(padding)) }
            composable(Dest.Watched.route) { WatchedScreen(Modifier.padding(padding)) }
        }
    }
}
