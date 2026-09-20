package com.yugentech.quill.navigation.navgraph

import android.app.Activity
import android.content.Context
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yugentech.quill.library.viewmodel.LibraryViewModel
import com.yugentech.quill.navigation.screen.AppScreen
import com.yugentech.quill.reader.ReaderActivity
import com.yugentech.quill.ui.main.parent.MainScreen
import com.yugentech.quill.ui.tabs.libraryScreen.parent.AllBooksScreen
import com.yugentech.quill.ui.tabs.libraryScreen.viewmodel.SeeAllViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    context: Context
) {
    composable(AppScreen.Main.route) {
        val libraryViewModel: LibraryViewModel = koinViewModel()

        MainScreen(
            libraryViewModel = libraryViewModel,
            onBookClick = { book ->
                context.startActivity(
                    ReaderActivity.createIntent(
                        context = context,
                        bookId = book.id
                    )
                )
            },
            onResumeClick = { book ->
                context.startActivity(
                    ReaderActivity.createIntent(
                        context = context,
                        bookId = book.id
                    )
                )
            },
            onSeeAllClick = { categoryName ->
                navController.navigate(AppScreen.AllBooks.route + "/$categoryName") {
                    launchSingleTop = true
                }
            },
            onExitApp = {
                (context as? Activity)?.finishAffinity()
            }
        )
    }

    composable(
        route = AppScreen.AllBooks.route + "/{categoryName}",
        arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
    ) { backStackEntry ->
        val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
        val seeAllViewModel: SeeAllViewModel = koinViewModel(
            parameters = { parametersOf(categoryName) }
        )

        AllBooksScreen(
            viewModel = seeAllViewModel,
            onBackClick = { navController.popBackStack() },
            onBookClick = { book ->
                context.startActivity(
                    ReaderActivity.createIntent(
                        context = context,
                        bookId = book.id
                    )
                )
            }
        )
    }
}
