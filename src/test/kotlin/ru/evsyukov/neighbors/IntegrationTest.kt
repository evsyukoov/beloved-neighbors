package ru.evsyukov.neighbors

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.containers.PostgreSQLContainer
import ru.evsyukov.neighbors.bot.TelegramSender
import ru.evsyukov.neighbors.domain.Participant
import ru.evsyukov.neighbors.domain.repository.ActivityMatchRepository
import ru.evsyukov.neighbors.domain.repository.ParticipantRepository
import ru.evsyukov.neighbors.domain.repository.ProposalRepository

/**
 * База для интеграционных тестов: настоящий Postgres в контейнере и Flyway, но без выхода
 * в Telegram — `app.bot.enabled=false` плюс замоканный [TelegramSender].
 */
@SpringBootTest(
    properties = [
        "app.bot.enabled=false",
        "app.bot.token=test:token",
        "app.timezone=Europe/Moscow",
        "app.horizon-days=7",
    ],
)
abstract class IntegrationTest {

    @MockitoBean
    protected lateinit var sender: TelegramSender

    @Autowired
    protected lateinit var participantRepository: ParticipantRepository

    @Autowired
    protected lateinit var proposalRepository: ProposalRepository

    @Autowired
    protected lateinit var matchRepository: ActivityMatchRepository

    @BeforeEach
    fun cleanDatabase() {
        matchRepository.deleteAll()
        proposalRepository.deleteAll()
        participantRepository.deleteAll()
    }

    protected fun givenParticipant(telegramId: Long, name: String): Participant =
        participantRepository.save(
            Participant(telegramId = telegramId, chatId = telegramId, firstName = name),
        )

    companion object {
        /**
         * Жизненным циклом управляет Spring Boot: контейнер поднимается один раз на
         * закэшированный контекст и живёт до конца прогона. С `@Testcontainers`/`@Container`
         * он останавливался после первого тест-класса, а переиспользованный контекст
         * продолжал стучаться на старый порт.
         */
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:17-alpine")
    }
}
