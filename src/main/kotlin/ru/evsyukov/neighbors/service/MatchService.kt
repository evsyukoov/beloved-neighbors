package ru.evsyukov.neighbors.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.evsyukov.neighbors.bot.Messages
import ru.evsyukov.neighbors.bot.TelegramSender
import ru.evsyukov.neighbors.domain.Activity
import ru.evsyukov.neighbors.domain.ActivityMatch
import ru.evsyukov.neighbors.domain.repository.ActivityMatchRepository
import java.time.LocalDate

/** Матчинг: пересечение желаний на день и уведомления обоим. */
@Service
@Transactional
class MatchService(
    private val matches: ActivityMatchRepository,
    private val proposals: ProposalService,
    private val participants: ParticipantService,
    private val sender: TelegramSender,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    enum class Outcome {
        /** Матч появился. */
        MATCHED,

        /** Матч был, но активность стала другой. */
        UPDATED,

        /** Матч развалился. */
        CANCELLED,

        /** Ничего не изменилось. */
        NONE,
    }

    /**
     * Пересчитывает матч на [day] по актуальным предложениям обоих и уведомляет,
     * только если что-то реально изменилось. Идемпотентен — можно звать после любого действия.
     */
    fun recompute(day: LocalDate): Outcome {
        val byAuthor = proposals.activeOn(day).distinctBy { it.author.telegramId }
        val wanted = if (byAuthor.size == 2) {
            Activity.intersect(byAuthor[0].activity, byAuthor[1].activity)
        } else {
            null
        }
        val existing = matches.findByDay(day)
        val today = proposals.today()

        return when {
            wanted == null && existing != null -> {
                matches.delete(existing)
                notifyBoth(Messages.matchCancelled(day, today, existing.activity))
                log.info("Матч на {} отменён", day)
                Outcome.CANCELLED
            }

            wanted != null && existing == null -> {
                matches.save(ActivityMatch(day = day, activity = wanted))
                celebrate(day, today, wanted)
                log.info("Матч на {}: {}", day, wanted)
                Outcome.MATCHED
            }

            wanted != null && existing != null && existing.activity != wanted -> {
                existing.activity = wanted
                celebrate(day, today, wanted)
                log.info("Матч на {} обновлён: {}", day, wanted)
                Outcome.UPDATED
            }

            else -> Outcome.NONE
        }
    }

    @Transactional(readOnly = true)
    fun upcoming(): List<ActivityMatch> =
        matches.findByDayGreaterThanEqualOrderByDayAsc(proposals.today())

    private fun celebrate(day: LocalDate, today: LocalDate, activity: Activity) {
        val text = Messages.matched(day, today, activity)
        participants.both().forEach { sender.sendWithSticker(it.chatId, activity, text) }
    }

    private fun notifyBoth(text: String) {
        participants.both().forEach { sender.sendText(it.chatId, text) }
    }
}
