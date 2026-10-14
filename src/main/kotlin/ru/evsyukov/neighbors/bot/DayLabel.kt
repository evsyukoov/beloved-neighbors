package ru.evsyukov.neighbors.bot

import java.time.LocalDate

/** Человеческие подписи дат. Названия дней зашиты, чтобы не зависеть от локали JVM. */
object DayLabel {

    private val WEEKDAYS = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")

    private fun weekday(day: LocalDate) = WEEKDAYS[day.dayOfWeek.value - 1]

    private fun date(day: LocalDate) = "%02d.%02d".format(day.dayOfMonth, day.monthValue)

    /** Для кнопок: «Сегодня», «Завтра», «ср 15.10». */
    fun short(day: LocalDate, today: LocalDate): String = when (day) {
        today -> "Сегодня"
        today.plusDays(1) -> "Завтра"
        else -> "${weekday(day)} ${date(day)}"
    }

    /** Для текстов: «сегодня (15.10)», «завтра (16.10)», «ср, 17.10». */
    fun full(day: LocalDate, today: LocalDate): String = when (day) {
        today -> "сегодня (${date(day)})"
        today.plusDays(1) -> "завтра (${date(day)})"
        else -> "${weekday(day)}, ${date(day)}"
    }
}
