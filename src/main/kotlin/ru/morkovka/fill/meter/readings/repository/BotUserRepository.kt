package ru.morkovka.fill.meter.readings.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.morkovka.fill.meter.readings.entity.BotUser

interface BotUserRepository : JpaRepository<BotUser, Long>