package ru.morkovka.fill.meter.readings.entity

import java.io.File

data class ResultFill (var srcWater: File? = null, var srcHeat: File? = null, var error: File? = null)