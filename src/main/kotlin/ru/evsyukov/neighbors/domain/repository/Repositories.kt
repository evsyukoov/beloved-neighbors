package ru.evsyukov.neighbors.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.evsyukov.neighbors.domain.ActivityMatch
import ru.evsyukov.neighbors.domain.Participant
import ru.evsyukov.neighbors.domain.Proposal
import ru.evsyukov.neighbors.domain.ProposalStatus
import java.time.LocalDate

interface ParticipantRepository : JpaRepository<Participant, Long>

interface ProposalRepository : JpaRepository<Proposal, Long> {

    fun findByDayAndStatus(day: LocalDate, status: ProposalStatus): List<Proposal>

    fun findByAuthorTelegramIdAndDayAndStatus(
        telegramId: Long,
        day: LocalDate,
        status: ProposalStatus,
    ): Proposal?

    fun findByStatusAndDayGreaterThanEqualOrderByDayAscIdAsc(
        status: ProposalStatus,
        from: LocalDate,
    ): List<Proposal>

    fun deleteByDayLessThan(day: LocalDate): Long
}

interface ActivityMatchRepository : JpaRepository<ActivityMatch, Long> {

    fun findByDay(day: LocalDate): ActivityMatch?

    fun findByDayGreaterThanEqualOrderByDayAsc(from: LocalDate): List<ActivityMatch>

    fun deleteByDayLessThan(day: LocalDate): Long
}
