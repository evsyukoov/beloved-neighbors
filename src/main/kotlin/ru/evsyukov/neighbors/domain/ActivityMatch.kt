package ru.evsyukov.neighbors.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

/**
 * Состоявшееся совпадение на день. Одна строка на день — она же защита от повторной
 * отправки стикеров: уведомляем только когда строка появилась или в ней сменилась активность.
 */
@Entity
@Table(name = "activity_match")
class ActivityMatch(
    @Column(name = "day", nullable = false)
    val day: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "activity", nullable = false, length = 32)
    var activity: Activity,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
}
