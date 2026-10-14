package ru.evsyukov.neighbors.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import ru.evsyukov.neighbors.IntegrationTest
import ru.evsyukov.neighbors.domain.Activity

class MatchServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var proposals: ProposalService

    @Autowired
    private lateinit var matches: MatchService

    @Test
    fun `пересечение желаний даёт матч и стикер обоим`() {
        val me = givenParticipant(1L, "Денис")
        val partner = givenParticipant(2L, "Соседка")
        val day = proposals.today().plusDays(1)

        proposals.propose(me, day, Activity.SEX_AND_MOVIE)
        assertThat(matches.recompute(day)).isEqualTo(MatchService.Outcome.NONE)

        proposals.propose(partner, day, Activity.MOVIE)
        assertThat(matches.recompute(day)).isEqualTo(MatchService.Outcome.MATCHED)

        assertThat(matchRepository.findByDay(day)?.activity).isEqualTo(Activity.MOVIE)
        verify(sender).sendWithSticker(eq(me.chatId), eq(Activity.MOVIE), any())
        verify(sender).sendWithSticker(eq(partner.chatId), eq(Activity.MOVIE), any())
    }

    @Test
    fun `повторный пересчёт не присылает стикеры снова`() {
        val me = givenParticipant(1L, "Денис")
        val partner = givenParticipant(2L, "Соседка")
        val day = proposals.today()
        proposals.propose(me, day, Activity.SEX)
        proposals.propose(partner, day, Activity.SEX)
        matches.recompute(day)

        assertThat(matches.recompute(day)).isEqualTo(MatchService.Outcome.NONE)
        verify(sender, times(2)).sendWithSticker(any(), any(), any())
    }

    @Test
    fun `несовпадающие желания матча не дают`() {
        val me = givenParticipant(1L, "Денис")
        val partner = givenParticipant(2L, "Соседка")
        val day = proposals.today()
        proposals.propose(me, day, Activity.SEX)
        proposals.propose(partner, day, Activity.MOVIE)

        assertThat(matches.recompute(day)).isEqualTo(MatchService.Outcome.NONE)
        assertThat(matchRepository.findByDay(day)).isNull()
        verify(sender, never()).sendWithSticker(any(), any(), any())
    }

    @Test
    fun `смена активности апгрейдит матч`() {
        val me = givenParticipant(1L, "Денис")
        val partner = givenParticipant(2L, "Соседка")
        val day = proposals.today()
        proposals.propose(me, day, Activity.SEX_AND_MOVIE)
        proposals.propose(partner, day, Activity.MOVIE)
        matches.recompute(day)

        proposals.propose(partner, day, Activity.SEX_AND_MOVIE)

        assertThat(matches.recompute(day)).isEqualTo(MatchService.Outcome.UPDATED)
        assertThat(matchRepository.findByDay(day)?.activity).isEqualTo(Activity.SEX_AND_MOVIE)
        assertThat(matchRepository.count()).isEqualTo(1)
    }

    @Test
    fun `скинутое предложение снимает матч`() {
        val me = givenParticipant(1L, "Денис")
        val partner = givenParticipant(2L, "Соседка")
        val day = proposals.today()
        val mine = proposals.propose(me, day, Activity.SEX).proposal
        proposals.propose(partner, day, Activity.SEX_AND_MOVIE)
        matches.recompute(day)

        proposals.drop(requireNotNull(mine.id), me)

        assertThat(matches.recompute(day)).isEqualTo(MatchService.Outcome.CANCELLED)
        assertThat(matchRepository.findByDay(day)).isNull()
        verify(sender, times(2)).sendText(any(), any(), eq(null))
    }

    @Test
    fun `в предстоящих матчах только будущее`() {
        val me = givenParticipant(1L, "Денис")
        val partner = givenParticipant(2L, "Соседка")
        val day = proposals.today().plusDays(3)
        proposals.propose(me, day, Activity.MOVIE)
        proposals.propose(partner, day, Activity.MOVIE)
        matches.recompute(day)

        assertThat(matches.upcoming()).singleElement()
            .satisfies({ assertThat(it.day).isEqualTo(day) })
    }
}
