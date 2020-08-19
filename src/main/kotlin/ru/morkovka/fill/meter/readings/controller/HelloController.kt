package ru.morkovka.fill.meter.readings.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloController {

    @GetMapping
    fun hello(): String {
        return "Hello! You can start/stop bot by /api/bot/start or api/bot/stop"
    }
}