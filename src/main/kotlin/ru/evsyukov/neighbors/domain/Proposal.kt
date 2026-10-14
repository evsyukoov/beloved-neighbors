package ru.evsyukov.neighbors.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

enum class ProposalStatus { ACTIVE, CANCELLED }

/**
 * Предложение «хочу вот это в такой-то день». На человека и день активно не больше одного —
 * повторный выбор обновляет активность (гарантируется частичным уникальным индексом).
 */
@Entity
@Table(name = "proposal")
class Proposal(
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    val author: Participant,

    @Column(name = "day", nullable = false)
    val day: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "activity", nullable = false, length = 32)
    var activity: Activity,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: ProposalStatus = ProposalStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
}
