package ru.morkovka.fill.meter.readings.entity

import javax.persistence.*

@Entity
@Table(name = "Person")
data class User(
    @Id
    @Column val id: Long,
    @Column val username: String?,
    @Column var login: String? = null,
    @Column var password: String? = null,
    @Column var cold: String? = null,
    @Column var hot: String? = null,
    @Column var heat: String? = null,
    @Enumerated(EnumType.STRING)
    @Column var fillingDataStep: FillingDataStep
)