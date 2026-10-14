package ru.evsyukov.neighbors.bot

import ru.evsyukov.neighbors.domain.Activity
import java.time.LocalDate

/**
 * Пэйлоад инлайн-кнопки. Telegram ограничивает callback_data 64 байтами, поэтому формат
 * короткий: тег и аргументы через `|`.
 */
sealed interface CallbackData {

    fun serialize(): String

    /** Выбран день — показываем активности. */
    data class PickDay(val day: LocalDate) : CallbackData {
        override fun serialize() = "d|$day"
    }

    /** Выбрана активность на день. */
    data class PickActivity(val day: LocalDate, val activity: Activity) : CallbackData {
        override fun serialize() = "a|$day|${activity.name}"
    }

    /** «Я тоже» под предложением напарника. */
    data class MeToo(val day: LocalDate, val activity: Activity) : CallbackData {
        override fun serialize() = "m|$day|${activity.name}"
    }

    /** Скинуть своё предложение. */
    data class Drop(val proposalId: Long) : CallbackData {
        override fun serialize() = "x|$proposalId"
    }

    /** Передумал / закрыть. */
    data object Dismiss : CallbackData {
        override fun serialize() = "n"
    }

    companion object {
        fun parse(raw: String): CallbackData? {
            val parts = raw.split('|')
            return runCatching {
                when (parts[0]) {
                    "d" -> PickDay(LocalDate.parse(parts[1]))
                    "a" -> PickActivity(LocalDate.parse(parts[1]), Activity.valueOf(parts[2]))
                    "m" -> MeToo(LocalDate.parse(parts[1]), Activity.valueOf(parts[2]))
                    "x" -> Drop(parts[1].toLong())
                    "n" -> Dismiss
                    else -> null
                }
            }.getOrNull()
        }
    }
}
