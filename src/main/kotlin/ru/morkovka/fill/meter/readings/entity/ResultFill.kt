package ru.morkovka.fill.meter.readings.entity

import java.io.File

data class ResultFill (var srcWater: List<File>? = null, var srcHeat: List<File>? = null, var error: File? = null)