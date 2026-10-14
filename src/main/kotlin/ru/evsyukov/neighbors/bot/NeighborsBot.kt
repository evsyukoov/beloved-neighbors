package ru.evsyukov.neighbors.bot

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import ru.evsyukov.neighbors.config.AppProperties

/**
 * Точка входа Telegram: стартер сам поднимает long polling для этого бина.
 * Апдейты обрабатываются в одном потоке, поэтому гонок за матчи нет.
 */
@Component
@ConditionalOnProperty(prefix = "app.bot", name = ["enabled"], matchIfMissing = true)
class NeighborsBot(
    private val props: AppProperties,
    private val router: UpdateRouter,
) : SpringLongPollingBot {

    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = LongPollingSingleThreadUpdateConsumer { update ->
        log.debug("Апдейт {}", update.updateId)
        router.handle(update)
    }

    override fun getBotToken(): String = props.bot.token

    override fun getUpdatesConsumer(): LongPollingUpdateConsumer = consumer
}
