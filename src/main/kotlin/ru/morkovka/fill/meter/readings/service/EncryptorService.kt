package ru.morkovka.fill.meter.readings.service

interface EncryptorService {
    fun encrypt(str: String): String?
    fun decrypt(str: String?): String?
}