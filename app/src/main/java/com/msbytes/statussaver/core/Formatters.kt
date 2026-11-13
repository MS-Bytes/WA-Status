package com.msbytes.statussaver.core

import java.text.DateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun size(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.getDefault(), "%.0f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.getDefault(), "%.1f MB", mb)
        return String.format(Locale.getDefault(), "%.2f GB", mb / 1024.0)
    }

    fun duration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    fun dateTime(epochMillis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

    fun time(epochMillis: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(epochMillis))
}
