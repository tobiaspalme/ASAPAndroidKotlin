package net.sharksystem.asap.android.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun Any.getLogStart(): String = this::class.java.simpleName

fun getFormattedTimestamp(): String {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    return now.format(formatter)
}