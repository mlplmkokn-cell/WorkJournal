package com.workjournal.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.workjournal.ui.screens.*

object Routes {
    const val HOME        = "home"
    const val ADD_WORK    = "add_work"
    const val SEARCH      = "search"
    const val ASSISTANT   = "assistant"
    const val SETTINGS    = "settings"
    const val CATEGORIES  = "categories"
    const val DETAIL      = "detail/{id}"
    fun detail(id: Long)  = "detail/$id"
}

@Composable
fun WorkJournalNavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onAddClick     = { nav.navigate(Routes.ADD_WORK) },
                onSearchClick  = { nav.navigate(Routes.SEARCH) },
                onAssistantClick = { nav.navigate(Routes.ASSISTANT) },
                onSettingsClick  = { nav.navigate(Routes.SETTINGS) },
                onRecordClick  = { id -> nav.navigate(Routes.detail(id)) }
            )
        }

        composable(Routes.ADD_WORK) {
            AddWorkScreen(onSaved = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onRecordClick = { id -> nav.navigate(Routes.detail(id)) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.ASSISTANT) {
            AssistantScreen(
                onBack          = { nav.popBackStack() },
                onRecordClick   = { id -> nav.navigate(Routes.detail(id)) },
                onSettingsClick = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack            = { nav.popBackStack() },
                onCategoriesClick = { nav.navigate(Routes.CATEGORIES) }
            )
        }

        composable(Routes.CATEGORIES) {
            CategoriesScreen(onBack = { nav.popBackStack() })
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { back ->
            val id = back.arguments?.getLong("id") ?: return@composable
            DetailScreen(recordId = id, onBack = { nav.popBackStack() }, onEdit = {})
        }
    }
}
