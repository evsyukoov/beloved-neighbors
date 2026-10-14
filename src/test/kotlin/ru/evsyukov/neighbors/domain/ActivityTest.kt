package ru.evsyukov.neighbors.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ActivityTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "SEX|SEX|SEX",
            "MOVIE|MOVIE|MOVIE",
            "SEX_AND_MOVIE|SEX_AND_MOVIE|SEX_AND_MOVIE",
            "MOVIE|SEX_AND_MOVIE|MOVIE",
            "SEX_AND_MOVIE|SEX|SEX",
            "SEX|MOVIE|",
            "MOVIE|SEX|",
        ],
        delimiter = '|',
    )
    fun `пересечение желаний`(first: Activity, second: Activity, expected: Activity?) {
        assertThat(Activity.intersect(first, second)).isEqualTo(expected)
    }

    @Test
    fun `пересечение симметрично`() {
        Activity.entries.forEach { first ->
            Activity.entries.forEach { second ->
                assertThat(Activity.intersect(first, second))
                    .isEqualTo(Activity.intersect(second, first))
            }
        }
    }

    @Test
    fun `у каждой активности свой уникальный набор тегов`() {
        val tagSets = Activity.entries.map { it.tags }
        assertThat(tagSets).doesNotHaveDuplicates()
        Activity.entries.forEach { assertThat(it.tags).isNotEmpty() }
    }

    @Test
    fun `подпись содержит эмодзи всех тегов`() {
        assertThat(Activity.SEX_AND_MOVIE.label).isEqualTo("🔥🍿 Секс + фильм")
    }
}
