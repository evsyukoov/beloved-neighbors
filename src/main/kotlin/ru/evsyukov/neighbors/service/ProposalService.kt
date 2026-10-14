package ru.evsyukov.neighbors.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.evsyukov.neighbors.config.AppProperties
import ru.evsyukov.neighbors.domain.Activity
import ru.evsyukov.neighbors.domain.Participant
import ru.evsyukov.neighbors.domain.Proposal
import ru.evsyukov.neighbors.domain.ProposalStatus
import ru.evsyukov.neighbors.domain.repository.ProposalRepository
import java.time.Clock
import java.time.LocalDate

/** Предложения: создать, обновить, скинуть, посмотреть. */
@Service
@Transactional
class ProposalService(
    private val proposals: ProposalRepository,
    private val props: AppProperties,
    private val clock: Clock,
) {
    fun today(): LocalDate = LocalDate.now(clock)

    /** Дни, которые можно предлагать: от сегодня и на horizonDays вперёд. */
    fun horizon(): List<LocalDate> =
        (0..props.horizonDays).map { today().plusDays(it.toLong()) }

    /**
     * Создаёт активное предложение или меняет активность в существующем.
     * @throws IllegalArgumentException если день вне окна планирования (устаревшая клавиатура).
     */
    fun propose(author: Participant, day: LocalDate, activity: Activity): Result {
        require(day in horizon()) { "День $day вне окна планирования" }
        val existing = proposals.findByAuthorTelegramIdAndDayAndStatus(
            author.telegramId, day, ProposalStatus.ACTIVE,
        )
        if (existing == null) {
            val created = proposals.save(Proposal(author = author, day = day, activity = activity))
            return Result(created, changed = true)
        }
        if (existing.activity == activity) {
            return Result(existing, changed = false)
        }
        existing.activity = activity
        return Result(existing, changed = true)
    }

    /** Скидывает своё предложение. `null` — чужое, уже скинутое или несуществующее. */
    fun drop(proposalId: Long, owner: Participant): Proposal? {
        val proposal = proposals.findById(proposalId).orElse(null) ?: return null
        if (proposal.author.telegramId != owner.telegramId || proposal.status != ProposalStatus.ACTIVE) {
            return null
        }
        proposal.status = ProposalStatus.CANCELLED
        return proposal
    }

    @Transactional(readOnly = true)
    fun activeOn(day: LocalDate): List<Proposal> =
        proposals.findByDayAndStatus(day, ProposalStatus.ACTIVE)

    /** Все актуальные предложения обоих участников, начиная с сегодня. */
    @Transactional(readOnly = true)
    fun upcoming(): List<Proposal> =
        proposals.findByStatusAndDayGreaterThanEqualOrderByDayAscIdAsc(ProposalStatus.ACTIVE, today())

    data class Result(val proposal: Proposal, val changed: Boolean)
}
