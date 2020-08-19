package ru.morkovka.fill.meter.readings.service

import ru.morkovka.fill.meter.readings.entity.User
import java.io.File

interface PageNavigationService {
    fun sendReadings(user: User): MutableList<File>
}