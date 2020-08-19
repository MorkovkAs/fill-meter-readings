package ru.morkovka.fill.meter.readings.service.impl

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import ru.morkovka.fill.meter.readings.entity.FillingDataStep
import ru.morkovka.fill.meter.readings.entity.User
import ru.morkovka.fill.meter.readings.repository.UserRepository
import ru.morkovka.fill.meter.readings.service.BotService
import ru.morkovka.fill.meter.readings.service.EncryptorService
import ru.morkovka.fill.meter.readings.service.PageNavigationService


@Service
class BotServiceImpl(
    @Value("\${telegram.bot.token}")
    private val telegramToken: String
) : BotService {

    @Autowired
    private lateinit var userRepository: UserRepository

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

                dispatch {
                    command("start") { bot, update ->
                        logger.info("[Telegram bot] got \'start\' command")
                        bot.sendMessage(
                            chatId = update.message!!.chat.id,
                            text = "Привет!\n" +
                                    "Я помогу вам отправлять показания счетчиков в УК Комфорт.\n" +
                                    "Для работы мне необходимы логин и пароль от личного кабинета и текущие показания.\n" +
                                    "Для вызова описания возможных команд используйте /help"
                        )
                    }

                    command("help") { bot, update ->
                        logger.info("[Telegram bot] got \'help\' command")
                        bot.sendMessage(
                            chatId = update.message!!.chat.id,
                            text = "/addOrEditUser - создание/редактирование пользователя\n" +
                                    "/fillReadings - заполнение показаний\n" +
                                    "/sendReadings - отправка показаний\n" +
                                    "/help - описание команд"
                        )
                    }

                    command("addOrEditUser") { bot, update ->
                        logger.info("[Telegram bot] got \'addOrEditUser\' command")
                        val userId = update.message!!.from!!.id
                        var user = userRepository.findByIdOrNull(userId)

                        if (user == null) {
                            user = User(
                                id = userId,
                                username = update.message!!.from!!.username,
                                fillingDataStep = FillingDataStep.WAIT_FOR_LOGIN
                            )
                        } else {
                            user.fillingDataStep = FillingDataStep.WAIT_FOR_LOGIN
                        }
                        userRepository.save(user)

                        bot.sendMessage(
                            chatId = update.message!!.chat.id,
                            text = "Введите свой логин от личного кабинета https://room.comfort-group.ru/login/"
                        )
                    }

                    command("fillReadings") { bot, update ->
                        logger.info("[Telegram bot] got \'fillReadings\' command")
                        val userId = update.message!!.from!!.id
                        val user = userRepository.findByIdOrNull(userId)

                        if (user == null) {
                            bot.sendMessage(
                                chatId = update.message!!.chat.id,
                                text = "Перед приемом показаний необходимо указать учетные данные. Используйте /addOrEditUser"
                            )
                        } else {
                            user.fillingDataStep = FillingDataStep.WAIT_FOR_COLD
                            userRepository.save(user)

                            bot.sendMessage(
                                chatId = update.message!!.chat.id,
                                text = "Введите показания холодной воды"
                            )
                        }
                    }

                    command("sendReadings") { bot, update ->
                        logger.info("[Telegram bot] got \'sendReadings\' command")
                        val userId = update.message!!.from!!.id
                        val user = userRepository.findByIdOrNull(userId)

                        if (user == null) {
                            bot.sendMessage(
                                chatId = update.message!!.chat.id,
                                text = "Перед отправкой показаний необходимо указать учетные данные. Используйте /addOrEditUser"
                            )
                        } else {
                            user.fillingDataStep = FillingDataStep.WAIT_FOR_SENT
                            userRepository.save(user)
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
                        logger.info("[Telegram bot] got \'Yes\' command")
                        val userId = update.message!!.from!!.id
                        val user = userRepository.findByIdOrNull(userId)

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
                                text = "Начата отправка показаний. Это может занять секунд 10-15."
                            )
                            user.fillingDataStep = FillingDataStep.NONE
                            userRepository.save(user)
                            val list = pageNavigationService.sendReadings(user)
                            bot.sendMessage(
                                chatId = update.message!!.chat.id,
                                text = "Показания успешно отправлены"
                            )
                            list.forEach {
                                bot.sendPhoto(
                                    chatId = update.message!!.chat.id,
                                    photo = it
                                )
                            }
                        }
                    }

                    command("No") { bot, update ->
                        logger.info("[Telegram bot] got \'No\' command")
                        val userId = update.message!!.from!!.id
                        val user = userRepository.findByIdOrNull(userId)
                        if (user == null) {
                            bot.sendMessage(
                                chatId = update.message!!.chat.id,
                                text = "Перед отправкой показаний необходимо указать учетные данные. Используйте /addOrEditUser"
                            )
                        } else if (user.fillingDataStep == FillingDataStep.WAIT_FOR_SENT) {
                            user.fillingDataStep = FillingDataStep.NONE
                            userRepository.save(user)
                            bot.sendMessage(
                                chatId = update.message!!.chat.id,
                                text = "Отправка показаний остановлена. Для повтора используйте /sendReadings"
                            )
                        }
                    }

                    command("checkPass") { bot, update ->
                        logger.info("[Telegram bot] got \'checkPass\' command")
                        val userId = update.message!!.from!!.id
                        val user = userRepository.getOne(userId)
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
                        if (update.message?.text?.startsWith("/") == true) {
                            return@text
                        }

                        val userId = update.message!!.from!!.id
                        val user = userRepository.findByIdOrNull(userId)

                        if (user != null) {
                            when (user.fillingDataStep) {
                                FillingDataStep.WAIT_FOR_LOGIN -> {
                                    user.login = update.message?.text
                                    user.fillingDataStep = FillingDataStep.WAIT_FOR_PASSWORD
                                    userRepository.save(user)
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
                                    userRepository.save(user)
                                    bot.sendMessage(
                                        chatId = update.message!!.chat.id, text = "Пользователь успешно обновлен.\n" +
                                                "Я забочусь о безопасности, поэтому шифрую пароли.\n" +
                                                "Ваша учетная запись [${user.login} - ${user.password}]" +
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
                                        userRepository.save(user)
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
                                        userRepository.save(user)
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
                                        userRepository.save(user)
                                        bot.sendMessage(
                                            chatId = update.message!!.chat.id, text = "Показания успешно обновлены:\n" +
                                                    "холодная ${user.cold}\n" +
                                                    "горячая ${user.hot}\n" +
                                                    "отопление ${user.heat}\n" +
                                                    "Теперь вы можете отправить текущие показания счетчиков. Используйте /sendReadings"
                                        )
                                    }
                                    return@text
                                }
                                FillingDataStep.NONE -> {
                                }
                            }
                        }

                        val text = "Сожалею, но я вас не понимаю: " + update.message?.text
                        bot.sendMessage(chatId = update.message!!.chat.id, text = text)
                    }

                    text("ping") { bot, update ->
                        logger.info("[Telegram bot] got \'ping\' command")
                        bot.sendMessage(chatId = update.message!!.chat.id, text = "Pong")
                    }
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