package ru.morkovka.fill.meter.readings.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.morkovka.fill.meter.readings.service.BotService

@RestController
@RequestMapping("/api/bot")
class BotController {

    @Autowired
    private lateinit var botService: BotService

    @GetMapping("start")
    fun start () {
        botService.start()
    }

    @GetMapping("stop")
    fun stop () {
        botService.stop()
    }
}