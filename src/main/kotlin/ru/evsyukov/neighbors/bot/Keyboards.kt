package ru.evsyukov.neighbors.bot

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import ru.evsyukov.neighbors.domain.Activity
import java.time.LocalDate

object Keyboards {

    /** Постоянное меню под полем ввода. */
    fun mainMenu(): ReplyKeyboardMarkup = ReplyKeyboardMarkup(
        listOf(
            KeyboardRow(Messages.BTN_PROPOSE),
            KeyboardRow(Messages.BTN_LIST, Messages.BTN_MATCHES),
            KeyboardRow(Messages.BTN_HELP),
        ),
    ).apply { resizeKeyboard = true }

    /** Дни планирования, по две кнопки в ряд. */
    fun days(days: List<LocalDate>, today: LocalDate): InlineKeyboardMarkup = InlineKeyboardMarkup(
        days.chunked(2).map { chunk ->
            InlineKeyboardRow(
                *chunk.map { button(DayLabel.short(it, today), CallbackData.PickDay(it)) }.toTypedArray(),
            )
        },
    )

    /** Активности на выбранный день, по одной в ряд. */
    fun activities(day: LocalDate): InlineKeyboardMarkup = InlineKeyboardMarkup(
        // InlineKeyboardRow сам является списком, поэтому плюс-оператор здесь бы его «расплескал»
        buildList {
            Activity.entries.forEach {
                add(InlineKeyboardRow(button(it.label, CallbackData.PickActivity(day, it))))
            }
            add(InlineKeyboardRow(button("🙈 Передумал", CallbackData.Dismiss)))
        },
    )

    /** Под своим предложением — кнопка скинуть. */
    fun myProposal(proposalId: Long): InlineKeyboardMarkup = InlineKeyboardMarkup(
        listOf(InlineKeyboardRow(button("❌ Скинуть", CallbackData.Drop(proposalId)))),
    )

    /** Под предложением напарника — согласиться или отказаться. */
    fun partnerProposal(day: LocalDate, activity: Activity): InlineKeyboardMarkup = InlineKeyboardMarkup(
        listOf(
            InlineKeyboardRow(
                button("🙌 Я тоже", CallbackData.MeToo(day, activity)),
                button("🙈 Не сегодня", CallbackData.Dismiss),
            ),
        ),
    )

    private fun button(text: String, data: CallbackData): InlineKeyboardButton =
        InlineKeyboardButton.builder()
            .text(text)
            .callbackData(data.serialize())
            .build()
}
