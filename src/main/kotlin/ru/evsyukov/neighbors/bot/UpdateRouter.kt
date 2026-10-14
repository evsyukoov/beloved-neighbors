package ru.evsyukov.neighbors.bot

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import ru.evsyukov.neighbors.domain.Activity
import ru.evsyukov.neighbors.domain.Participant
import ru.evsyukov.neighbors.service.MatchService
import ru.evsyukov.neighbors.service.ParticipantService
import ru.evsyukov.neighbors.service.ProposalService
import java.time.LocalDate

/** Разбор апдейтов: команды, кнопки меню и инлайн-колбэки. */
@Component
class UpdateRouter(
    private val participants: ParticipantService,
    private val proposals: ProposalService,
    private val matches: MatchService,
    private val sender: TelegramSender,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(update: Update) {
        runCatching {
            when {
                update.hasCallbackQuery() -> onCallback(update.callbackQuery)
                update.hasMessage() -> onMessage(update.message)
                else -> log.debug("Апдейт {} без сообщения и колбэка — пропускаю", update.updateId)
            }
        }.onFailure { log.error("Не смог обработать апдейт {}", update.updateId, it) }
    }

    private fun onMessage(message: Message) {
        val from = message.from ?: return
        val chatId = message.chatId

        if (message.text?.trim()?.startsWith("/start") == true) {
            start(from, chatId)
            return
        }

        val me = participants.find(from.id)
        if (me == null) {
            sender.sendText(chatId, Messages.NOT_A_MEMBER)
            return
        }

        // стикер — отвечаем его file_id, чтобы было что вписать в конфиг
        if (message.hasSticker()) {
            sender.sendText(chatId, Messages.stickerId(message.sticker.fileId))
            return
        }

        when (message.text?.trim()) {
            "/propose", Messages.BTN_PROPOSE -> askDay(me)
            "/list", Messages.BTN_LIST -> showProposals(me)
            "/matches", Messages.BTN_MATCHES -> showMatches(me)
            else -> sender.sendText(chatId, Messages.HELP, Keyboards.mainMenu())
        }
    }

    private fun start(from: User, chatId: Long) {
        when (val registration = participants.register(from.id, chatId, from.firstName, from.userName)) {
            is ParticipantService.Registration.Joined -> {
                val greeting = if (registration.alone) Messages.ALONE else Messages.WELCOME_PAIR
                sender.sendText(chatId, greeting, Keyboards.mainMenu())
                if (!registration.alone) {
                    participants.partnerOf(from.id)?.let {
                        sender.sendText(it.chatId, Messages.partnerJoined(registration.participant))
                    }
                }
            }

            is ParticipantService.Registration.AlreadyIn ->
                sender.sendText(chatId, Messages.WELCOME_BACK, Keyboards.mainMenu())

            ParticipantService.Registration.NoRoom ->
                sender.sendText(chatId, Messages.NO_ROOM)
        }
    }

    private fun askDay(me: Participant) {
        if (participants.partnerOf(me.telegramId) == null) {
            sender.sendText(me.chatId, Messages.NO_PARTNER)
            return
        }
        sender.sendText(me.chatId, Messages.PICK_DAY, Keyboards.days(proposals.horizon(), proposals.today()))
    }

    private fun showProposals(me: Participant) {
        val upcoming = proposals.upcoming()
        if (upcoming.isEmpty()) {
            sender.sendText(me.chatId, Messages.NOTHING_PLANNED, Keyboards.mainMenu())
            return
        }
        val today = proposals.today()
        sender.sendText(me.chatId, Messages.LIST_HEADER)
        upcoming.forEach { proposal ->
            val mine = proposal.author.telegramId == me.telegramId
            if (mine) {
                sender.sendText(
                    me.chatId,
                    Messages.myProposal(proposal.day, today, proposal.activity),
                    Keyboards.myProposal(requireNotNull(proposal.id)),
                )
            } else {
                sender.sendText(
                    me.chatId,
                    Messages.partnerProposal(proposal.author, proposal.day, today, proposal.activity),
                    Keyboards.partnerProposal(proposal.day, proposal.activity),
                )
            }
        }
    }

    private fun showMatches(me: Participant) {
        val upcoming = matches.upcoming()
        if (upcoming.isEmpty()) {
            sender.sendText(me.chatId, Messages.NO_MATCHES, Keyboards.mainMenu())
            return
        }
        val today = proposals.today()
        val text = upcoming.joinToString("\n") { Messages.matchLine(it.day, today, it.activity) }
        sender.sendText(me.chatId, "${Messages.MATCHES_HEADER}\n$text")
    }

    private fun onCallback(callback: CallbackQuery) {
        sender.answerCallback(callback.id)

        val me = participants.find(callback.from.id)
        if (me == null) {
            sender.sendText(callback.from.id, Messages.NOT_A_MEMBER)
            return
        }
        val data = CallbackData.parse(callback.data.orEmpty())
        if (data == null) {
            sender.sendText(me.chatId, Messages.STALE_BUTTON)
            return
        }
        val messageId = callback.message?.messageId

        when (data) {
            is CallbackData.PickDay -> replace(
                me,
                messageId,
                Messages.pickActivity(data.day, proposals.today()),
                Keyboards.activities(data.day),
            )

            is CallbackData.PickActivity -> propose(me, data.day, data.activity, messageId)

            is CallbackData.MeToo -> propose(me, data.day, data.activity, messageId)

            is CallbackData.Drop -> drop(me, data.proposalId, messageId)

            CallbackData.Dismiss -> replace(me, messageId, Messages.DISMISSED, null)
        }
    }

    private fun propose(me: Participant, day: LocalDate, activity: Activity, messageId: Int?) {
        val result = try {
            proposals.propose(me, day, activity)
        } catch (e: IllegalArgumentException) {
            log.debug("Устаревшая кнопка: {}", e.message)
            replace(me, messageId, Messages.STALE_BUTTON, null)
            return
        }
        val today = proposals.today()
        val text = if (result.changed) {
            Messages.saved(day, today, activity)
        } else {
            Messages.unchanged(day, today, activity)
        }
        replace(me, messageId, text, null)
        if (!result.changed) {
            return
        }
        // при матче оба и так получают поздравление со стикером — дублировать не нужно
        if (matches.recompute(day) == MatchService.Outcome.NONE) {
            participants.partnerOf(me.telegramId)?.let { partner ->
                sender.sendText(
                    partner.chatId,
                    Messages.partnerProposed(me, day, today, activity),
                    Keyboards.partnerProposal(day, activity),
                )
            }
        }
    }

    private fun drop(me: Participant, proposalId: Long, messageId: Int?) {
        val dropped = proposals.drop(proposalId, me)
        if (dropped == null) {
            replace(me, messageId, Messages.ALREADY_GONE, null)
            return
        }
        val today = proposals.today()
        replace(me, messageId, Messages.dropped(dropped.day, today, dropped.activity), null)
        if (matches.recompute(dropped.day) != MatchService.Outcome.CANCELLED) {
            participants.partnerOf(me.telegramId)?.let { partner ->
                sender.sendText(
                    partner.chatId,
                    Messages.partnerDropped(me, dropped.day, today, dropped.activity),
                )
            }
        }
    }

    /** Переписывает сообщение с кнопками, если оно ещё доступно, иначе присылает новое. */
    private fun replace(
        me: Participant,
        messageId: Int?,
        text: String,
        markup: InlineKeyboardMarkup?,
    ) {
        if (messageId == null) {
            sender.sendText(me.chatId, text)
        } else {
            sender.editText(me.chatId, messageId, text, markup)
        }
    }
}
