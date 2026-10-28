package com.budzet.domain.user.repository

import com.budzet.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun existsByEmail(email: String): Boolean
    fun findByEmail(email: String): User?
    fun findByNameAndEmail(name: String, email: String): Optional<User>
}
