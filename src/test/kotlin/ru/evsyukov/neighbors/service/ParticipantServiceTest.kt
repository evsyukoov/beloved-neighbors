package ru.evsyukov.neighbors.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.evsyukov.neighbors.IntegrationTest

class ParticipantServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var participants: ParticipantService

    @Test
    fun `первые двое занимают места, третий получает отказ`() {
        val first = participants.register(1L, 1L, "Денис", "denis")
        val second = participants.register(2L, 2L, "Соседка", null)
        val third = participants.register(3L, 3L, "Лишний", null)

        assertThat(first).isInstanceOf(ParticipantService.Registration.Joined::class.java)
        assertThat((first as ParticipantService.Registration.Joined).alone).isTrue()
        assertThat((second as ParticipantService.Registration.Joined).alone).isFalse()
        assertThat(third).isEqualTo(ParticipantService.Registration.NoRoom)
        assertThat(participantRepository.count()).isEqualTo(2)
    }

    @Test
    fun `повторный start не занимает второе место, а обновляет данные`() {
        participants.register(1L, 1L, "Денис", "denis")
        val again = participants.register(1L, 99L, "Денис Е.", "denis_e")

        assertThat(again).isInstanceOf(ParticipantService.Registration.AlreadyIn::class.java)
        assertThat(participantRepository.count()).isEqualTo(1)

        val stored = participants.find(1L)!!
        assertThat(stored.chatId).isEqualTo(99L)
        assertThat(stored.firstName).isEqualTo("Денис Е.")
        assertThat(stored.username).isEqualTo("denis_e")
    }

    @Test
    fun `напарник находится по второму участнику`() {
        participants.register(1L, 1L, "Денис", null)
        assertThat(participants.partnerOf(1L)).isNull()

        participants.register(2L, 2L, "Соседка", null)
        assertThat(participants.partnerOf(1L)?.telegramId).isEqualTo(2L)
        assertThat(participants.partnerOf(2L)?.telegramId).isEqualTo(1L)
    }
}
