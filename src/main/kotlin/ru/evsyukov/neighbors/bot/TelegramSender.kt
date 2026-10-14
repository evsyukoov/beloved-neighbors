package ru.evsyukov.neighbors.bot

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendSticker
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.generics.TelegramClient
import ru.evsyukov.neighbors.config.AppProperties
import ru.evsyukov.neighbors.domain.Activity

/**
 * Тонкая обёртка над Bot API. Ошибки отправки логируются, а не пробрасываются: упавшее
 * уведомление не должно рушить обработку апдейта.
 */
@Component
class TelegramSender(
    private val client: TelegramClient,
    private val props: AppProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendText(chatId: Long, text: String, markup: ReplyKeyboard? = null) {
        val message = SendMessage.builder()
            .chatId(chatId.toString())
            .text(text.trim())
            .also { builder -> markup?.let { builder.replyMarkup(it) } }
            .build()
        safely("sendMessage в чат $chatId") { client.execute(message) }
    }

    fun sendSticker(chatId: Long, fileId: String) {
        val sticker = SendSticker.builder()
            .chatId(chatId.toString())
            .sticker(InputFile(fileId))
            .build()
        safely("sendSticker в чат $chatId") { client.execute(sticker) }
    }

    /** Стикер активности (если настроен) плюс текст. Без стикера эмодзи в тексте и так есть. */
    fun sendWithSticker(chatId: Long, activity: Activity, text: String) {
        props.stickerFor(activity)?.let { sendSticker(chatId, it) }
        sendText(chatId, text)
    }

    fun editText(chatId: Long, messageId: Int, text: String, markup: InlineKeyboardMarkup? = null) {
        val edit = EditMessageText.builder()
            .chatId(chatId.toString())
            .messageId(messageId)
            .text(text.trim())
            .also { builder -> markup?.let { builder.replyMarkup(it) } }
            .build()
        safely("editMessageText $messageId") { client.execute(edit) }
    }

    /** Гасит «часики» на инлайн-кнопке. Без этого Telegram крутит спиннер до таймаута. */
    fun answerCallback(callbackId: String, text: String? = null) {
        val answer = AnswerCallbackQuery.builder()
            .callbackQueryId(callbackId)
            .also { builder -> text?.let { builder.text(it) } }
            .build()
        safely("answerCallbackQuery $callbackId") { client.execute(answer) }
    }

    private fun safely(what: String, action: () -> Unit) {
        runCatching(action).onFailure { log.error("Не получилось: {}", what, it) }
    }
}
