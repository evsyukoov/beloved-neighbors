package ru.evsyukov.neighbors.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.evsyukov.neighbors.IntegrationTest
import ru.evsyukov.neighbors.domain.Activity
import ru.evsyukov.neighbors.domain.ProposalStatus

class ProposalServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var proposals: ProposalService

    @Test
    fun `повторный выбор на тот же день обновляет активность, а не плодит записи`() {
        val me = givenParticipant(1L, "Денис")
        val day = proposals.today().plusDays(1)

        val first = proposals.propose(me, day, Activity.MOVIE)
        val changed = proposals.propose(me, day, Activity.SEX_AND_MOVIE)

        assertThat(first.changed).isTrue()
        assertThat(changed.changed).isTrue()
        assertThat(changed.proposal.id).isEqualTo(first.proposal.id)
        assertThat(proposalRepository.count()).isEqualTo(1)
        assertThat(proposals.activeOn(day).single().activity).isEqualTo(Activity.SEX_AND_MOVIE)
    }

    @Test
    fun `тот же выбор второй раз ничего не меняет`() {
        val me = givenParticipant(1L, "Денис")
        val day = proposals.today()

        proposals.propose(me, day, Activity.SEX)
        val again = proposals.propose(me, day, Activity.SEX)

        assertThat(again.changed).isFalse()
    }

    @Test
    fun `день вне окна планирования не принимается`() {
        val me = givenParticipant(1L, "Денис")

        assertThatThrownBy { proposals.propose(me, proposals.today().minusDays(1), Activity.SEX) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { proposals.propose(me, proposals.today().plusDays(30), Activity.SEX) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `скинуть можно только своё активное предложение`() {
        val me = givenParticipant(1L, "Денис")
        val partner = givenParticipant(2L, "Соседка")
        val day = proposals.today()
        val mine = proposals.propose(me, day, Activity.MOVIE).proposal

        assertThat(proposals.drop(requireNotNull(mine.id), partner)).isNull()

        val dropped = proposals.drop(requireNotNull(mine.id), me)
        assertThat(dropped?.status).isEqualTo(ProposalStatus.CANCELLED)
        assertThat(proposals.activeOn(day)).isEmpty()

        // второй раз скинуть уже нечего
        assertThat(proposals.drop(requireNotNull(mine.id), me)).isNull()
    }

    @Test
    fun `после отмены можно предложить тот же день заново`() {
        val me = givenParticipant(1L, "Денис")
        val day = proposals.today()
        val mine = proposals.propose(me, day, Activity.MOVIE).proposal
        proposals.drop(requireNotNull(mine.id), me)

        val again = proposals.propose(me, day, Activity.SEX)

        assertThat(again.changed).isTrue()
        assertThat(again.proposal.id).isNotEqualTo(mine.id)
        assertThat(proposals.activeOn(day).single().activity).isEqualTo(Activity.SEX)
    }

    @Test
    fun `в списке актуального только активные предложения от сегодня`() {
        val me = givenParticipant(1L, "Денис")
        val partner = givenParticipant(2L, "Соседка")
        val today = proposals.today()
        proposals.propose(me, today, Activity.SEX)
        val toDrop = proposals.propose(partner, today.plusDays(2), Activity.MOVIE).proposal
        proposals.drop(requireNotNull(toDrop.id), partner)

        assertThat(proposals.upcoming()).singleElement()
            .satisfies({ assertThat(it.author.telegramId).isEqualTo(1L) })
    }
}
