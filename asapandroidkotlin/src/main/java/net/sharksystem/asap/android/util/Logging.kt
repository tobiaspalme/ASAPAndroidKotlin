package net.sharksystem.asap.android.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * This function is used to get the name of the class that is calling it.
 */
fun Any.getLogStart(): String = this::class.java.simpleName

/**
 * This function returns the current time in the format HH:mm:ss.
 */
fun getFormattedTimestamp(): String {
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    return now.format(formatter)
}