package aeh.heimvisir

import aeh.heimvisir.ui.LookupViewModel
import aeh.heimvisir.ui.screens.AboutScreen
import aeh.heimvisir.ui.screens.HistoryScreen
import aeh.heimvisir.ui.screens.LookupScreen
import aeh.heimvisir.ui.theme.HeimvisirTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeimvisirTheme {
                HeimvisirApp()
            }
        }
    }
}

private enum class Destination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Lookup("leit", R.string.tab_lookup, Icons.Filled.Search),
    History("saga", R.string.tab_history, Icons.Filled.History),
    About("um", R.string.tab_about, Icons.AutoMirrored.Filled.MenuBook),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeimvisirApp() {
    val navController = rememberNavController()
    // Eitt ViewModel fyrir alla skjái: notandinn á að geta ýtt á færslu
    // í sögunni og séð niðurstöðuna á leitarskjánum án þess að ástandið
    // tapist á leiðinni.
    val viewModel: LookupViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.historyItems.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Lookup.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Lookup.route) {
                LookupScreen(
                    state = state,
                    onTagChanged = viewModel::onTagChanged,
                    onSubmit = { viewModel.lookup() },
                )
            }
            composable(Destination.History.route) {
                HistoryScreen(
                    items = history,
                    onOpen = { tagNumber ->
                        viewModel.onTagChanged(tagNumber)
                        viewModel.lookup(tagNumber)
                        navController.navigate(Destination.Lookup.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onClear = viewModel::clearHistory,
                )
            }
            composable(Destination.About.route) {
                AboutScreen()
            }
        }
    }
}
