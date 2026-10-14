package ru.evsyukov.neighbors.bot

import ru.evsyukov.neighbors.domain.Activity
import ru.evsyukov.neighbors.domain.Participant
import java.time.LocalDate

/** Все тексты бота в одном месте. */
object Messages {

    const val BTN_PROPOSE = "🗓 Предложить"
    const val BTN_LIST = "📋 Предложения"
    const val BTN_MATCHES = "💘 Матчи"
    const val BTN_HELP = "❓ Помощь"

    const val HELP = """
Бот на двоих: отмечаешь день и желаемую активность. Как только желания на один день
пересеклись — обоим прилетает стикер.

$BTN_PROPOSE — выбрать день и активность (/propose)
$BTN_LIST — свои и чужие предложения, там же можно скинуть своё (/list)
$BTN_MATCHES — что уже совпало (/matches)

Ещё: пришли боту стикер — он вернёт его file_id, чтобы вписать в конфиг.
"""

    const val NO_ROOM = "Тут уже двое, третий лишний 🙃"

    const val NOT_A_MEMBER = "Бот приватный, места заняты 🙃"

    const val ALONE =
        "Место занято ✌️\nТеперь нужен второй человек: пусть тоже нажмёт /start у этого бота."

    const val WELCOME_PAIR = "Теперь вас двое 🎉 Можно предлагать!"

    const val WELCOME_BACK = "Ты уже в деле ✌️"

    const val PICK_DAY = "На какой день?"

    const val DISMISSED = "Ладно, в другой раз 🙈"

    const val NO_PARTNER = "Второй человек ещё не нажал /start — предлагать пока некому 🤷"

    const val NOTHING_PLANNED = "Пока ничего не предложено. Самое время это исправить 🗓"

    const val NO_MATCHES = "Совпадений пока нет 🤷"

    const val STALE_BUTTON = "Эта кнопка устарела, нажми $BTN_PROPOSE заново"

    const val LIST_HEADER = "Что сейчас на столе:"

    const val MATCHES_HEADER = "Совпадения:"

    const val ALREADY_GONE = "Уже неактуально"

    fun partnerJoined(partner: Participant) =
        "${partner.firstName} на месте — теперь вас двое 🎉 Можно предлагать!"

    fun pickActivity(day: LocalDate, today: LocalDate) =
        "${DayLabel.full(day, today).replaceFirstChar { it.uppercase() }} — чего хочется?"

    fun saved(day: LocalDate, today: LocalDate, activity: Activity) =
        "Записал: ${activity.label} на ${DayLabel.full(day, today)}"

    fun unchanged(day: LocalDate, today: LocalDate, activity: Activity) =
        "Это уже записано: ${activity.label} на ${DayLabel.full(day, today)}"

    fun partnerProposed(partner: Participant, day: LocalDate, today: LocalDate, activity: Activity) =
        "${partner.firstName} предлагает ${activity.label} на ${DayLabel.full(day, today)}"

    fun matched(day: LocalDate, today: LocalDate, activity: Activity) =
        "Совпадение! ${DayLabel.full(day, today).replaceFirstChar { it.uppercase() }} — ${activity.label} 🎉"

    fun matchCancelled(day: LocalDate, today: LocalDate, activity: Activity) =
        "Отменяется: ${activity.label} на ${DayLabel.full(day, today)} больше не совпадает 😶"

    fun dropped(day: LocalDate, today: LocalDate, activity: Activity) =
        "Скинул: ${activity.label} на ${DayLabel.full(day, today)}"

    fun partnerDropped(partner: Participant, day: LocalDate, today: LocalDate, activity: Activity) =
        "${partner.firstName} передумал: ${activity.label} на ${DayLabel.full(day, today)} отменяется"

    fun myProposal(day: LocalDate, today: LocalDate, activity: Activity) =
        "Моё: ${activity.label} на ${DayLabel.full(day, today)}"

    fun partnerProposal(partner: Participant, day: LocalDate, today: LocalDate, activity: Activity) =
        "${partner.firstName}: ${activity.label} на ${DayLabel.full(day, today)}"

    fun matchLine(day: LocalDate, today: LocalDate, activity: Activity) =
        "💘 ${DayLabel.full(day, today)} — ${activity.label}"

    fun stickerId(fileId: String) = "file_id этого стикера:\n$fileId"
}
