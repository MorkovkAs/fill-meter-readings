package ru.morkovka.fill.meter.readings

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text

fun main() {
    val userMap: HashMap<Long, User> = HashMap()

    val bot = bot {
        token = ""

        dispatch {
            command("start") { bot, update ->
                bot.sendMessage(chatId = update.message!!.chat.id, text = "Привет!")
            }
            command("addOrEditUser") { bot, update ->
                val userId = update.message!!.from!!.id

                var user = userMap[userId]
                if (user == null) {
                    user = User(
                        id = userId,
                        creationStep = CreationStep.WAIT_FOR_LOGIN,
                        fillingDataStep = FillingDataStep.NONE
                    )
                } else {
                    user.creationStep = CreationStep.WAIT_FOR_LOGIN
                }
                userMap[userId] = user

                bot.sendMessage(
                    chatId = update.message!!.chat.id,
                    text = "Введите свой логин от личного кабинета https://room.comfort-group.ru/login/"
                )
            }

            command("fillReadings") { bot, update ->
                val userId = update.message!!.from!!.id

                if (userMap[userId] == null) {
                    bot.sendMessage(
                        chatId = update.message!!.chat.id,
                        text = "Перед приемом показаний необходимо указать учетные данные. Используйте /addOrEditUser"
                    )
                } else {
                    val user = userMap[userId]!!
                    user.fillingDataStep = FillingDataStep.WAIT_FOR_COLD
                    userMap[userId] = user

                    bot.sendMessage(
                        chatId = update.message!!.chat.id,
                        text = "Введите показания холодной воды"
                    )
                }
            }

            command("sendReadings") { bot, update ->
                val userId = update.message!!.from!!.id

                if (userMap[userId] == null) {
                    bot.sendMessage(
                        chatId = update.message!!.chat.id,
                        text = "Перед отправкой показаний необходимо указать учетные данные. Используйте /addOrEditUser"
                    )
                } else {
                    val user = userMap[userId]!!
                    bot.sendMessage(
                        chatId = update.message!!.chat.id, text = "Отправить данные показания?\n" +
                                "холодная ${user.cold}\n" +
                                "горячая ${user.hot}\n" +
                                "отопление ${user.heat}\n" +
                                "\n/Yes\n/No"
                    )
                }
            }

            text { bot, update ->
                if (update.message?.text?.startsWith("/") == true) {
                    return@text
                }

                val userId = update.message!!.from!!.id
                if (userMap[userId] != null) {
                    val user = userMap[userId]!!

                    when (userMap[userId]?.creationStep) {
                        CreationStep.WAIT_FOR_LOGIN -> {
                            user.login = update.message?.text
                            user.creationStep = CreationStep.WAIT_FOR_PASSWORD
                            bot.sendMessage(chatId = update.message!!.chat.id, text = "Введите свой пароль")
                            return@text
                        }
                        CreationStep.WAIT_FOR_PASSWORD -> {
                            user.password = update.message?.text
                            user.creationStep = CreationStep.NONE
                            bot.sendMessage(
                                chatId = update.message!!.chat.id, text = "Пользователь успешно обновлен.\n" +
                                        "Теперь вы можете обновлять текущие показания счетчиков. Используйте /fillReadings"
                            )
                            return@text
                        }
                        CreationStep.NONE, null -> {
                        }
                    }
                    when (userMap[userId]?.fillingDataStep) {
                        FillingDataStep.WAIT_FOR_COLD -> {
                            user.cold = update.message?.text
                            user.fillingDataStep = FillingDataStep.WAIT_FOR_HOT
                            bot.sendMessage(chatId = update.message!!.chat.id, text = "Введите показания горячей воды")
                            return@text
                        }
                        FillingDataStep.WAIT_FOR_HOT -> {
                            user.hot = update.message?.text
                            user.fillingDataStep = FillingDataStep.WAIT_FOR_HEAT
                            bot.sendMessage(chatId = update.message!!.chat.id, text = "Введите показания отопления")
                            return@text
                        }
                        FillingDataStep.WAIT_FOR_HEAT -> {
                            user.heat = update.message?.text
                            user.fillingDataStep = FillingDataStep.NONE
                            bot.sendMessage(
                                chatId = update.message!!.chat.id, text = "Показания успешно обновлены:\n" +
                                        "холодная ${user.cold}\n" +
                                        "горячая ${user.hot}\n" +
                                        "отопление ${user.heat}\n" +
                                        "Теперь вы можете отправить текущие показания счетчиков. Используйте /sendReadings"
                            )
                            return@text
                        }
                        FillingDataStep.NONE, null -> {
                        }
                    }
                }

                val text = update.message?.text ?: "Hello, World!"
                bot.sendMessage(chatId = update.message!!.chat.id, text = text)
            }

            text("ping") { bot, update ->
                bot.sendMessage(chatId = update.message!!.chat.id, text = "Pong")
            }
        }
    }
    bot.startPolling()
}
