package ru.evsyukov.neighbors.bot

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.chat.Chat
import org.telegram.telegrambots.meta.api.objects.message.Message
import ru.evsyukov.neighbors.IntegrationTest
import ru.evsyukov.neighbors.domain.Activity
import ru.evsyukov.neighbors.service.ProposalService
import java.time.LocalDate

/**
 * Полный прогон бота «на двоих» без второго телеграм-аккаунта: апдейты собираются руками,
 * как их присылает Telegram, а отправка наружу замокана.
 */
class UpdateRouterTest : IntegrationTest() {

    @Autowired
    private lateinit var router: UpdateRouter

    @Autowired
    private lateinit var proposals: ProposalService

    private val denis = 1L
    private val neighbor = 2L
    private val stranger = 3L

    @Test
    fun `первые двое регистрируются, третьему отказ`() {
        router.handle(text(denis, "/start"))
        router.handle(text(neighbor, "/start"))
        router.handle(text(stranger, "/start"))

        assertThat(participantRepository.count()).isEqualTo(2)
        // первому сообщают, что напарник на месте
        verify(sender).sendText(eq(denis), eq(Messages.partnerJoined(participantRepository.findById(neighbor).get())), eq(null))
        verify(sender).sendText(eq(stranger), eq(Messages.NO_ROOM), eq(null))
    }

    @Test
    fun `посторонний не может ничего предложить`() {
        router.handle(text(denis, "/start"))
        router.handle(text(neighbor, "/start"))

        router.handle(text(stranger, Messages.BTN_PROPOSE))

        verify(sender).sendText(eq(stranger), eq(Messages.NOT_A_MEMBER), eq(null))
        assertThat(proposalRepository.count()).isZero()
    }

    @Test
    fun `пересечение желаний двух людей присылает стикер обоим`() {
        bothRegistered()
        val day = proposals.today().plusDays(1)

        // Денис хочет секс + фильм, соседка — только фильм
        router.handle(pick(denis, day, Activity.SEX_AND_MOVIE))
        router.handle(pick(neighbor, day, Activity.MOVIE))

        verify(sender).sendWithSticker(eq(denis), eq(Activity.MOVIE), any())
        verify(sender).sendWithSticker(eq(neighbor), eq(Activity.MOVIE), any())
        assertThat(matchRepository.findByDay(day)?.activity).isEqualTo(Activity.MOVIE)
    }

    @Test
    fun `без совпадения напарнику приходит уведомление с кнопками`() {
        bothRegistered()
        val day = proposals.today()

        router.handle(pick(denis, day, Activity.SEX))

        val expected = Messages.partnerProposed(
            participantRepository.findById(denis).get(),
            day,
            proposals.today(),
            Activity.SEX,
        )
        verify(sender).sendText(eq(neighbor), eq(expected), any())
        verify(sender, never()).sendWithSticker(any(), any(), any())
    }

    @Test
    fun `кнопка «я тоже» создаёт встречное предложение и матч`() {
        bothRegistered()
        val day = proposals.today()
        router.handle(pick(denis, day, Activity.MOVIE))

        router.handle(callback(neighbor, CallbackData.MeToo(day, Activity.MOVIE)))

        assertThat(proposals.activeOn(day)).hasSize(2)
        verify(sender).sendWithSticker(eq(denis), eq(Activity.MOVIE), any())
        verify(sender).sendWithSticker(eq(neighbor), eq(Activity.MOVIE), any())
    }

    @Test
    fun `скинутое предложение снимает матч`() {
        bothRegistered()
        val day = proposals.today()
        router.handle(pick(denis, day, Activity.SEX_AND_MOVIE))
        router.handle(pick(neighbor, day, Activity.SEX_AND_MOVIE))
        val mine = proposals.activeOn(day).first { it.author.telegramId == denis }

        router.handle(callback(denis, CallbackData.Drop(requireNotNull(mine.id))))

        assertThat(matchRepository.findByDay(day)).isNull()
        assertThat(proposals.activeOn(day)).hasSize(1)
    }

    @Test
    fun `чужое предложение скинуть нельзя`() {
        bothRegistered()
        val day = proposals.today()
        router.handle(pick(denis, day, Activity.MOVIE))
        val notMine = proposals.activeOn(day).single()

        router.handle(callback(neighbor, CallbackData.Drop(requireNotNull(notMine.id))))

        assertThat(proposals.activeOn(day)).hasSize(1)
    }

    @Test
    fun `устаревшая кнопка с прошедшим днём не ломает бота`() {
        bothRegistered()

        router.handle(pick(denis, proposals.today().minusDays(5), Activity.SEX))

        assertThat(proposalRepository.count()).isZero()
    }

    private fun bothRegistered() {
        router.handle(text(denis, "/start"))
        router.handle(text(neighbor, "/start"))
    }

    /** Выбор активности на день — то же, что нажатие инлайн-кнопки в боте. */
    private fun pick(userId: Long, day: LocalDate, activity: Activity): Update =
        callback(userId, CallbackData.PickActivity(day, activity))

    private fun text(userId: Long, text: String): Update = Update().apply {
        message = Message.builder()
            .messageId(messageIds++)
            .from(user(userId))
            .chat(Chat.builder().id(userId).type("private").build())
            .date((System.currentTimeMillis() / 1000).toInt())
            .text(text)
            .build()
    }

    private fun callback(userId: Long, data: CallbackData): Update = Update().apply {
        callbackQuery = CallbackQuery().apply {
            id = "cb-${messageIds++}"
            from = user(userId)
            this.data = data.serialize()
            chatInstance = "chat-instance"
            message = Message.builder()
                .messageId(messageIds++)
                .chat(Chat.builder().id(userId).type("private").build())
                .date((System.currentTimeMillis() / 1000).toInt())
                .build()
        }
    }

    private fun user(userId: Long): User =
        User(userId, if (userId == denis) "Денис" else "Соседка", false)

    private companion object {
        private var messageIds = 100
    }
}
