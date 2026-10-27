package com.budzet.domain.user.entity

import com.budzet.domain.room.entity.UserRoomConnection
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
open class User(email: String, password: String, name: String) {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null
        protected set

    @field:Column(unique = true, length = 50)
    open var email: String = email
        protected set

    @field:Column(length = 255)
    open var password: String = password
        protected set

    @field:Column(length = 50)
    open var name: String = name
        protected set

    @field:Column(length = 512)
    open var refreshToken: String? = null
        protected set

    @field:CreatedDate
    open var createdAt: LocalDateTime? = null
        protected set

    @field:LastModifiedDate
    open var modifiedAt: LocalDateTime? = null
        protected set

    @field:OneToMany(mappedBy = "user")
    open var roomConnections: MutableList<UserRoomConnection> = mutableListOf()
        protected set

    fun updateRefreshToken(refreshToken: String) {
        this.refreshToken = refreshToken
    }

    fun clearRefreshToken() {
        refreshToken = null
    }

    fun updatePassword(encodedPassword: String) {
        password = encodedPassword
    }

    fun updateName(name: String) {
        this.name = name
    }
}
