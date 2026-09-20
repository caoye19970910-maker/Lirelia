package com.yugentech.quill.navigation.screen

/**
 * Lirelia V0.1 navigation surface.
 *
 * Keep the baseline deliberately small: the local library is the home screen,
 * and category rows can open the all-books view. ReaderActivity remains a
 * separate activity provided by the reader module.
 */
sealed class AppScreen(val route: String) {
    data object Main : AppScreen("main")
    data object AllBooks : AppScreen("all_books")
}
