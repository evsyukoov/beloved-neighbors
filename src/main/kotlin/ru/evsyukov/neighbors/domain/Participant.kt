package ru.evsyukov.neighbors.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/** Один из двух участников. Первичный ключ — telegram id, других участников не предполагается. */
@Entity
@Table(name = "participant")
class Participant(
    @Id
    @Column(name = "telegram_id")
    val telegramId: Long,

    @Column(name = "chat_id", nullable = false)
    var chatId: Long,

    @Column(name = "first_name", nullable = false, length = 128)
    var firstName: String,

    @Column(name = "username", length = 64)
    var username: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
