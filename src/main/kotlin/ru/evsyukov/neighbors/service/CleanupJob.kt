package ru.evsyukov.neighbors.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.evsyukov.neighbors.domain.repository.ActivityMatchRepository
import ru.evsyukov.neighbors.domain.repository.ProposalRepository
import java.time.Clock
import java.time.LocalDate

/** Ночная уборка: прошедшие дни никому не интересны и только мешают в списках. */
@Component
class CleanupJob(
    private val proposals: ProposalRepository,
    private val matches: ActivityMatchRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 17 4 * * *")
    @Transactional
    fun dropPast() {
        val today = LocalDate.now(clock)
        val droppedMatches = matches.deleteByDayLessThan(today)
        val droppedProposals = proposals.deleteByDayLessThan(today)
        if (droppedProposals + droppedMatches > 0) {
            log.info("Убрал прошлое: предложений {}, матчей {}", droppedProposals, droppedMatches)
        }
    }
}
