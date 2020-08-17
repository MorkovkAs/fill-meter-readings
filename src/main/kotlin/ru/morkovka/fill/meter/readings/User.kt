package ru.morkovka.fill.meter.readings

data class User(
    val id: Long,
    var login: String? = null,
    var password: String? = null,
    var cold: String? = null,
    var hot: String? = null,
    var heat: String? = null,
    var creationStep: CreationStep,
    var fillingDataStep: FillingDataStep
)