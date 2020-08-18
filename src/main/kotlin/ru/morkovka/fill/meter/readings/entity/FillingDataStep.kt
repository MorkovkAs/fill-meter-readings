package ru.morkovka.fill.meter.readings.entity

enum class FillingDataStep {
    NONE,
    WAIT_FOR_LOGIN,
    WAIT_FOR_PASSWORD,
    WAIT_FOR_COLD,
    WAIT_FOR_HOT,
    WAIT_FOR_HEAT,
    WAIT_FOR_SENT
}