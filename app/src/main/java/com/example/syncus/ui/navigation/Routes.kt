package com.example.syncus.ui.navigation

object Routes {

    const val SPLASH = "splash"

    const val LOGIN = "login"
    const val REGISTER = "register"

    const val HOME = "home"

    const val TASKS = "tasks"
    const val ADD_TASK = "add_task"

    const val EDIT_TASK = "edit_task/{taskId}"

    const val CALENDAR = "calendar"

    const val PROFILE = "profile"

    const val SETTINGS = "settings"

    fun editTask(taskId: String) = "edit_task/$taskId"
}