package com.example.syncus.ui

import java.util.Calendar
import java.util.Date

fun sameMinute(a: Date, b: Date): Boolean {
    val ca = Calendar.getInstance().apply { time = a }
    val cb = Calendar.getInstance().apply { time = b }

    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH) &&
            ca.get(Calendar.DAY_OF_MONTH) == cb.get(Calendar.DAY_OF_MONTH) &&
            ca.get(Calendar.HOUR_OF_DAY) == cb.get(Calendar.HOUR_OF_DAY) &&
            ca.get(Calendar.MINUTE) == cb.get(Calendar.MINUTE)
}
