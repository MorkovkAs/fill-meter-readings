package ru.morkovka.fill.meter.readings.service.impl

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import ru.morkovka.fill.meter.readings.entity.BotUser
import ru.morkovka.fill.meter.readings.entity.ComfortUser
import ru.morkovka.fill.meter.readings.entity.FillingDataStep
import ru.morkovka.fill.meter.readings.repository.BotUserRepository
import ru.morkovka.fill.meter.readings.repository.ComfortUserRepository
import ru.morkovka.fill.meter.readings.service.BotService
import ru.morkovka.fill.meter.readings.service.EncryptorService
import ru.morkovka.fill.meter.readings.service.PageNavigationService
import java.time.LocalDateTime.now

@Service
class BotServiceImpl(
    @Value("\${telegram.bot.token}")
    private val telegramToken: String
) : BotService {

    @Autowired
    private lateinit var botUserRepository: BotUserRepository

    @Autowired
    private lateinit var comfortUserRepository: ComfortUserRepository

    @Autowired
    private lateinit var pageNavigationService: PageNavigationService

    @Autowired
    private lateinit var encryptorService: EncryptorService

    lateinit var bot: Bot

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    companion object {
        var botIsStarted = false
    }

    init {
        start()
    }

    final override fun start() {
        if (!botIsStarted) {
            botIsStarted = true
            bot = bot {
                token = telegramToken
                logLevel = HttpLoggingInterceptor.Level.NONE

                try {

                    dispatch {
                        command("start") { bot, update ->
                            val userId = update.message!!.from!!.id
                            logger.info("[user.id = $userId] [Telegram bot] got \'start\' command")
                            var botUser: BotUser? = botUserRepository.findByIdOrNull(userId)
                            if (botUser == null) {
                                botUser = BotUser(
                                    id = userId,
                                    username = update.message!!.from!!.username,
                                    createDate = now(),
                                    updateDate = now()
                                )
                            } else {
                                botUser.updateDate = now()
                            }
                            botUserRepository.save(botUser)

                            bot.sendMessage(
                                chatId = update.message!!.chat.id,
                                text = "Привет!\n" +
                                        "Я помогу вам отправлять показания счетчиков в УК Комфорт.\n" +
                                        "Для работы мне необходимы логин и пароль от личного кабинета и текущие показания.\n" +
                                        "Для вызова подсказки используйте /help"
                            )
                        }

                        command("help") { bot, update ->
                            val userId = update.message!!.from!!.id
                            logger.info("[user.id = $userId] [Telegram bot] got \'help\' command")
                            bot.sendMessage(
                                chatId = update.message!!.chat.id,
                                text = "/addOrEditUser - создание/редактирование пользователя\n" +
                                        "/fillReadings - заполнение показаний\n" +
                                        "/sendReadings - отправка показаний\n" +
                                        "/help - описание команд"
                            )
                        }

                        command("addOrEditUser") { bot, update ->
                            val userId = update.message!!.from!!.id
                            logger.info("[user.id = $userId] [Telegram bot] got \'addOrEditUser\' command")
                            val botUser = botUserRepository.getOne((userId))
                            val user = botUser.comfortUser

                            if (user == null) {
                                botUser.comfortUser = ComfortUser(
                                    id = userId,
                                    fillingDataStep = FillingDataStep.WAIT_FOR_LOGIN,
                                    botUser = botUser
                                )
                            } else {
                                user.fillingDataStep = FillingDataStep.WAIT_FOR_LOGIN
                            }
                            botUserRepository.save(botUser)

                            bot.sendMessage(
                                chatId = update.message!!.chat.id,
                                text = "Введите свой логин от личного кабинета https://room.comfort-group.ru/login/"
                            )
                        }

                        command("fillReadings") { bot, update ->
                            val userId = update.message!!.from!!.id
                            logger.info("[user.id = $userId] [Telegram bot] got \'fillReadings\' command")
                            val botUser = botUserRepository.getOne((userId))
                            val user = botUser.comfortUser

                            if (user == null) {
                                bot.sendMessage(
                                    chatId = update.message!!.chat.id,
                                    text = "Перед приемом показаний необходимо указать учетные данные. Используйте /addOrEditUser"
                                )
                            } else {
                                user.fillingDataStep = FillingDataStep.WAIT_FOR_COLD
                                comfortUserRepository.save(user)

                                bot.sendMessage(
                                    chatId = update.message!!.chat.id,
                                    text = "Введите показания холодной воды"
                                )
                            }
                        }

                        command("sendReadings") { bot, update ->
                            val userId = update.message!!.from!!.id
                            logger.info("[user.id = $userId] [Telegram bot] got \'sendReadings\' command")
                            val botUser = botUserRepository.getOne((userId))
                            val user = botUser.comfortUser

                            if (user == null) {
                                bot.sendMessage(
                                    chatId = update.message!!.chat.id,
                                    text = "Перед отправкой показаний необходимо указать учетные данные. Используйте /addOrEditUser"
                                )
                            } else {
                                user.fillingDataStep = FillingDataStep.WAIT_FOR_SENT
                                comfortUserRepository.save(user)
                                bot.sendMessage(
                                    chatId = update.message!!.chat.id, text = "Отправить данные показания?\n" +
                                            "холодная ${user.cold}\n" +
                                            "горячая ${user.hot}\n" +
                                            "отопление ${user.heat}\n" +
                                            "\n/Yes\n/No"
                                )
                            }
                        }

                        command("Yes") { bot, update ->
                            val userId = update.message!!.from!!.id
                            logger.info("[user.id = $userId] [Telegram bot] got \'Yes\' command")
                            val botUser = botUserRepository.getOne((userId))
                            val user = botUser.comfortUser

                            if (user == null) {
                                bot.sendMessage(
                                    chatId = update.message!!.chat.id,
                                    text = "Перед отправкой показаний необходимо указать учетные данные. Используйте /addOrEditUser"
                                )
                            } else if (
                                StringUtils.isEmpty(user.login) ||
                                StringUtils.isEmpty(user.password) ||
                                StringUtils.isEmpty(user.cold) ||
                                StringUtils.isEmpty(user.hot) ||
                                StringUtils.isEmpty(user.heat)
                            ) {
                                bot.sendMessage(
                                    chatId = update.message!!.chat.id,
                                    text = "Перед отправкой показаний необходимо заполнить показания. Используйте /fillReadings"
                                )
                            } else {
                                bot.sendMessage(
                                    chatId = update.message!!.chat.id,
                                    text = "Начата отправка показаний. Это может занять 10-15 секунд."
                                )
                                user.fillingDataStep = FillingDataStep.NONE
                                comfortUserRepository.save(user)
                                val result = pageNavigationService.sendReadings(user)
                                if (result.error != null) {
                                    bot.sendMessage(
                                        chatId = update.message!!.chat.id,
                                        text = "Произошла ошибка во время передачи показаний \uD83D\uDC47"
                                    )
                                    result.error?.let {
                                        bot.sendPhoto(
                                            chatId = update.message!!.chat.id,
                                            photo = it
                                        )
                                    }
                                }
                                if (result.srcWater == null && result.srcHeat == null && result.error == null) {
                                    bot.sendMessage(
                                        chatId = update.message!!.chat.id,
                                        text = "Произошла неизвестная ошибка во время передачи показаний"
                                    )
                                }
                                result.srcWater?.let {
                                    bot.sendMessage(
                                        chatId = update.message!!.chat.id,
                                        text = "Показания воды успешно отправлены \uD83D\uDC47"
                                    )
                                    bot.sendPhoto(
                                        chatId = update.message!!.chat.id,
                                        photo = it
                                    )
                                }
                                result.srcHeat?.let {
                                    bot.sendMessage(
                                        chatId = update.message!!.chat.id,
                                        text = "Показания отопления успешно отправлены \uD83D\uDC47"
                                    )
                                    bot.sendPhoto(
                                        chatId = update.message!!.chat.id,
                                        photo = it
                                    )
                                }
                            }
                        }

                        command("No") { bot, update ->
                            val userId = update.message!!.from!!.id
                            logger.info("[user.id = $userId] [Telegram bot] got \'No\' command")
                            val botUser = botUserRepository.getOne((userId))
                            val user = botUser.comfortUser
                            if (user == null) {
                                bot.sendMessage(
                                    chatId = update.message!!.chat.id,
                                    text = "Перед отправкой показаний необходимо указать учетные данные. Используйте /addOrEditUser"
                                )
                            } else if (user.fillingDataStep == FillingDataStep.WAIT_FOR_SENT) {
                                user.fillingDataStep = FillingDataStep.NONE
                                comfortUserRepository.save(user)
                                bot.sendMessage(
                                    chatId = update.message!!.chat.id,
                                    text = "Отправка показаний остановлена. Для повтора используйте /sendReadings"
                                )
                            }
                        }

                        command("checkPass") { bot, update ->
                            val userId = update.message!!.from!!.id
                            logger.info("[user.id = $userId] [Telegram bot] got \'checkPass\' command")
                            val user = comfortUserRepository.getOne(userId)
                            var password = ""
                            if (!StringUtils.isEmpty(user.password)) {
                                password = encryptorService.decrypt(user.password) ?: ""
                            }
                            bot.sendMessage(
                                chatId = update.message!!.chat.id,
                                text = "Ваш настоящий пароль: $password"
                            )
                        }

                        text { bot, update ->
                            val userId = update.message!!.from!!.id
                            val botUser = botUserRepository.findByIdOrNull(userId)
                            botUser?.let {
                                botUser.username = update.message!!.from!!.username
                                botUser.updateDate = now()
                                botUserRepository.save(botUser)
                            }

                            if (update.message?.text?.startsWith("/") == true) {
                                return@text
                            }

                            val user = botUser!!.comfortUser

                            if (user != null) {
                                when (user.fillingDataStep) {
                                    FillingDataStep.WAIT_FOR_LOGIN -> {
                                        user.login = update.message?.text
                                        user.fillingDataStep = FillingDataStep.WAIT_FOR_PASSWORD
                                        comfortUserRepository.save(user)
                                        bot.sendMessage(
                                            chatId = update.message!!.chat.id, text = "Введите свой пароль. \n" +
                                                    "Не беспокойтесь, он будет храниться в зашифрованном виде."
                                        )
                                        return@text
                                    }
                                    FillingDataStep.WAIT_FOR_PASSWORD -> {
                                        var password = ""
                                        if (!StringUtils.isEmpty(update.message?.text)) {
                                            password = encryptorService.encrypt(update.message?.text!!) ?: ""
                                        }
                                        user.password = password
                                        user.fillingDataStep = FillingDataStep.NONE
                                        comfortUserRepository.save(user)
                                        bot.sendMessage(
                                            chatId = update.message!!.chat.id,
                                            text = "Пользователь успешно обновлен.\n" +
                                                    "Я забочусь о безопасности, поэтому шифрую пароли.\n" +
                                                    "Ваша учетная запись [${user.login} - ${user.password}]\n" +
                                                    "Теперь вы можете обновлять текущие показания счетчиков. Используйте /fillReadings"
                                        )
                                        return@text
                                    }
                                    FillingDataStep.WAIT_FOR_COLD -> {
                                        if (update.message?.text!!.toDoubleOrNull() == null) {
                                            bot.sendMessage(
                                                chatId = update.message!!.chat.id,
                                                text = "Введите показания холодной воды. Только цифры с разделителем точка."
                                            )
                                        } else {
                                            user.cold = update.message?.text
                                            user.fillingDataStep = FillingDataStep.WAIT_FOR_HOT
                                            comfortUserRepository.save(user)
                                            bot.sendMessage(
                                                chatId = update.message!!.chat.id,
                                                text = "Введите показания горячей воды"
                                            )
                                        }
                                        return@text
                                    }
                                    FillingDataStep.WAIT_FOR_HOT -> {
                                        if (update.message?.text!!.toDoubleOrNull() == null) {
                                            bot.sendMessage(
                                                chatId = update.message!!.chat.id,
                                                text = "Введите показания горячей воды. Только цифры с разделителем точка."
                                            )
                                        } else {
                                            user.hot = update.message?.text
                                            user.fillingDataStep = FillingDataStep.WAIT_FOR_HEAT
                                            comfortUserRepository.save(user)
                                            bot.sendMessage(
                                                chatId = update.message!!.chat.id,
                                                text = "Введите показания отопления"
                                            )
                                        }
                                        return@text
                                    }
                                    FillingDataStep.WAIT_FOR_HEAT -> {
                                        if (update.message?.text!!.toDoubleOrNull() == null) {
                                            bot.sendMessage(
                                                chatId = update.message!!.chat.id,
                                                text = "Введите показания отопления. Только цифры с разделителем точка."
                                            )
                                        } else {
                                            user.heat = update.message?.text
                                            user.fillingDataStep = FillingDataStep.NONE
                                            comfortUserRepository.save(user)
                                            bot.sendMessage(
                                                chatId = update.message!!.chat.id,
                                                text = "Показания успешно обновлены:\n" +
                                                        "холодная ${user.cold}\n" +
                                                        "горячая ${user.hot}\n" +
                                                        "отопление ${user.heat}\n" +
                                                        "Теперь вы можете отправить текущие показания счетчиков. Используйте /sendReadings"
                                            )
                                        }
                                        return@text
                                    }
                                    FillingDataStep.WAIT_FOR_SENT, FillingDataStep.NONE -> {
                                    }
                                }
                            }

                            val text = "Сожалею, но я вас не понимаю: " + update.message?.text
                            bot.sendMessage(chatId = update.message!!.chat.id, text = text)
                        }

                        text("ping") { bot, update ->
                            val userId = update.message!!.from!!.id
                            logger.info("[user.id = $userId] [Telegram bot] got \'ping\' command")
                            bot.sendMessage(chatId = update.message!!.chat.id, text = "Pong")
                        }
                    }
                } catch (ex: Exception) {
                    logger.warn(ExceptionUtils.getStackTrace(ex))
                }
            }
            bot.startPolling()
        }
    }

    override fun stop() {
        if (botIsStarted) {
            bot.stopPolling()
            botIsStarted = false
        }
    }
}