package ru.evsyukov.neighbors.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.generics.TelegramClient
import java.time.Clock

@Configuration
class TelegramConfig(private val props: AppProperties) {

    @Bean
    fun telegramClient(): TelegramClient = OkHttpTelegramClient(props.bot.token)

    /** Часы в таймзоне участников — все «сегодня/завтра» считаются от них. */
    @Bean
    fun clock(): Clock = Clock.system(props.zone)
}
