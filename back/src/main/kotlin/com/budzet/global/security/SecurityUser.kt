package com.budzet.global.security

import com.budzet.domain.user.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class SecurityUser(val user: User) : UserDetails {
    override fun getUsername(): String = user.email
    override fun getPassword(): String = user.password
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
}
