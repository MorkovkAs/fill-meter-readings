package ru.morkovka.fill.meter.readings.entity

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "BotUser")
data class BotUser(
    @Id
    @Column(name = "bot_user_id")
    val id: Long,

    @Column var username: String?,
    @Column val createDate: LocalDateTime,
    @Column var updateDate: LocalDateTime,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "comfort_user_id", referencedColumnName = "id")
    var comfortUser: ComfortUser? = null
)