package ru.evsyukov.neighbors.config

import org.springframework.boot.context.properties.ConfigurationProperties
import ru.evsyukov.neighbors.domain.Activity
import java.time.ZoneId

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val bot: Bot,
    /** На сколько дней вперёд (включительно) можно планировать. */
    val horizonDays: Int = 7,
    val timezone: String = "Europe/Moscow",
    /** file_id стикера для активности; пустое значение — отправим просто текст с эмодзи. */
    val stickers: Map<Activity, String> = emptyMap(),
) {
    val zone: ZoneId get() = ZoneId.of(timezone)

    fun stickerFor(activity: Activity): String? = stickers[activity]?.takeIf { it.isNotBlank() }

    data class Bot(
        val token: String,
        val username: String = "",
        /** false — не подключаться к Telegram (нужно в тестах). */
        val enabled: Boolean = true,
    )
}
