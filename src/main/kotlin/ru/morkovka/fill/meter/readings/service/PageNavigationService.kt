package ru.morkovka.fill.meter.readings.service

import ru.morkovka.fill.meter.readings.entity.ResultFill
import ru.morkovka.fill.meter.readings.entity.ComfortUser

interface PageNavigationService {
    fun sendReadings(comfortUser: ComfortUser): ResultFill
}