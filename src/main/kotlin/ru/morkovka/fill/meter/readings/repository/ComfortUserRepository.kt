package ru.morkovka.fill.meter.readings.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.morkovka.fill.meter.readings.entity.ComfortUser

interface ComfortUserRepository : JpaRepository<ComfortUser, Long>