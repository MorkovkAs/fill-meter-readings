package ru.morkovka.fill.meter.readings.entity

import javax.persistence.*

@Entity
@Table(name = "ComfortUser")
data class ComfortUser(
    @Id
    @Column
    val id: Long,

    @OneToOne(mappedBy = "comfortUser")
    val botUser: BotUser,

    @Column var login: String? = null,
    @Column var password: String? = null,
    @Column var cold: String? = null,
    @Column var hot: String? = null,
    @Column var heat: String? = null,
    @Enumerated(EnumType.STRING)
    @Column var fillingDataStep: FillingDataStep
)