package ru.evsyukov.neighbors.domain

/**
 * Элементарное желание. Активности собираются из тегов, и именно по тегам считается
 * пересечение: «секс + фильм» пересекается и с «сексом», и с «фильмом».
 */
enum class ActivityTag(val emoji: String) {
    SEX("🔥"),
    MOVIE("🍿"),
}

/**
 * Активность, которую можно предложить. Чтобы добавить новую — достаточно одной строки здесь
 * (и, при необходимости, нового тега плюс стикера в конфиге).
 */
enum class Activity(val title: String, val tags: Set<ActivityTag>) {
    SEX("Секс", setOf(ActivityTag.SEX)),
    MOVIE("Фильм", setOf(ActivityTag.MOVIE)),
    SEX_AND_MOVIE("Секс + фильм", setOf(ActivityTag.SEX, ActivityTag.MOVIE)),
    ;

    val emoji: String get() = tags.joinToString("") { it.emoji }

    /** Подпись для кнопок и сообщений: «🔥🍿 Секс + фильм». */
    val label: String get() = "$emoji $title"

    companion object {
        fun byTags(tags: Set<ActivityTag>): Activity? = entries.firstOrNull { it.tags == tags }

        /**
         * Что реально можно сделать вместе, если один хочет [first], а второй — [second].
         * `null` — желания не пересекаются.
         */
        fun intersect(first: Activity, second: Activity): Activity? =
            byTags(first.tags intersect second.tags)
    }
}
