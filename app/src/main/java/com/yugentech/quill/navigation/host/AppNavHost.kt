package com.yugentech.quill.navigation.host

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.yugentech.quill.navigation.navgraph.mainGraph
import com.yugentech.quill.navigation.screen.AppScreen

/**
 * Lirelia clean baseline navigation.
 *
 * Accounts, onboarding, subscriptions, cloud sync, remote catalogs and Aira
 * are intentionally kept out of the runtime graph while the reading core is
 * stabilized.
 */
@Composable
fun AppNavHost(
    navController: NavHostController
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = AppScreen.Main.route
    ) {
        mainGraph(
            navController = navController,
            context = context
        )
    }
}
