package ru.evsyukov.neighbors.bot

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.evsyukov.neighbors.domain.Activity
import java.time.LocalDate

class CallbackDataTest {

    private val day = LocalDate.of(2026, 10, 15)

    @Test
    fun `сериализация и разбор дают тот же объект`() {
        val all = listOf(
            CallbackData.PickDay(day),
            CallbackData.PickActivity(day, Activity.SEX_AND_MOVIE),
            CallbackData.MeToo(day, Activity.MOVIE),
            CallbackData.Drop(42),
            CallbackData.Dismiss,
        )
        all.forEach { assertThat(CallbackData.parse(it.serialize())).isEqualTo(it) }
    }

    @Test
    fun `пэйлоад влезает в лимит telegram`() {
        val longest = CallbackData.PickActivity(day, Activity.entries.maxBy { it.name.length })
        assertThat(longest.serialize().toByteArray()).hasSizeLessThanOrEqualTo(64)
    }

    @Test
    fun `мусор не ломает разбор`() {
        listOf("", "zzz", "d|не-дата", "a|2026-10-15|НЕТУ", "x|не-число", "a|2026-10-15")
            .forEach { assertThat(CallbackData.parse(it)).isNull() }
    }
}
