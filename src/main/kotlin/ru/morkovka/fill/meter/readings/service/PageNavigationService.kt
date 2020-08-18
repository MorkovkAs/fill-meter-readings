package ru.morkovka.fill.meter.readings.service

import ru.morkovka.fill.meter.readings.entity.User

interface PageNavigationService {
    fun sendReadings(user: User)
}