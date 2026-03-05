package com.communicationcoach.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.communicationcoach.ui.screens.ConversationDetailScreen
import com.communicationcoach.ui.screens.ConversationsScreen
import com.communicationcoach.ui.screens.HomeScreen
import com.communicationcoach.ui.screens.ProfileScreen
import com.communicationcoach.ui.screens.ProgressScreen

// ── Route constants ───────────────────────────────────────────────────────────

private object Routes {
    const val HOME = "home"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val CONVERSATIONS = "conversations"
    const val CONVERSATION_DETAIL = "conversation_detail/{conversationId}"

    fun conversationDetail(id: Long) = "conversation_detail/$id"
}

// ── Bottom nav tabs ───────────────────────────────────────────────────────────

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val tabs = listOf(
    NavTab(Routes.HOME,     "Home",     Icons.Filled.Home,      Icons.Outlined.Home),
    NavTab(Routes.PROGRESS, "Progress", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    NavTab(Routes.PROFILE,  "Profile",  Icons.Filled.Person,    Icons.Outlined.Person)
)

// Routes where the bottom bar is hidden (sub-screens)
private val hideBottomBarRoutes = setOf(
    Routes.CONVERSATIONS,
    Routes.CONVERSATION_DETAIL
)

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute !in hideBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = backStackEntry?.destination
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == tab.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    // Pop to the start destination so we don't build a huge stack
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToConversations = {
                        navController.navigate(Routes.CONVERSATIONS)
                    }
                )
            }

            composable(Routes.PROGRESS) {
                ProgressScreen()
            }

            composable(Routes.PROFILE) {
                ProfileScreen()
            }

            composable(Routes.CONVERSATIONS) {
                ConversationsScreen(
                    onBack = { navController.popBackStack() },
                    onConversationClick = { id ->
                        navController.navigate(Routes.conversationDetail(id))
                    }
                )
            }

            composable(Routes.CONVERSATION_DETAIL) { backStackEntry ->
                val conversationId = backStackEntry.arguments
                    ?.getString("conversationId")
                    ?.toLongOrNull()
                    ?: return@composable

                ConversationDetailScreen(
                    conversationId = conversationId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
