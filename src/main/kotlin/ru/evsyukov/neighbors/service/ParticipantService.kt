package ru.evsyukov.neighbors.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.evsyukov.neighbors.domain.Participant
import ru.evsyukov.neighbors.domain.repository.ParticipantRepository

/** Регистрация участников: первые двое, нажавшие /start, занимают места. */
@Service
@Transactional
class ParticipantService(private val participants: ParticipantRepository) {

    sealed interface Registration {
        /** Место занято только что. */
        data class Joined(val participant: Participant, val alone: Boolean) : Registration

        /** Уже зарегистрирован, данные обновлены. */
        data class AlreadyIn(val participant: Participant) : Registration

        /** Мест нет — двое уже есть. */
        data object NoRoom : Registration
    }

    fun register(telegramId: Long, chatId: Long, firstName: String, username: String?): Registration {
        val existing = participants.findById(telegramId).orElse(null)
        if (existing != null) {
            existing.chatId = chatId
            existing.firstName = firstName
            existing.username = username
            return Registration.AlreadyIn(existing)
        }
        val taken = participants.count()
        if (taken >= MAX_PARTICIPANTS) {
            return Registration.NoRoom
        }
        val joined = participants.save(Participant(telegramId, chatId, firstName, username))
        return Registration.Joined(joined, alone = taken == 0L)
    }

    @Transactional(readOnly = true)
    fun find(telegramId: Long): Participant? = participants.findById(telegramId).orElse(null)

    @Transactional(readOnly = true)
    fun partnerOf(telegramId: Long): Participant? =
        participants.findAll().firstOrNull { it.telegramId != telegramId }

    @Transactional(readOnly = true)
    fun both(): List<Participant> = participants.findAll()

    companion object {
        const val MAX_PARTICIPANTS = 2
    }
}
