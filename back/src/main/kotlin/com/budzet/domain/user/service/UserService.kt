package com.budzet.domain.user.service

import com.budzet.domain.user.dto.TokenRefreshResponse
import com.budzet.domain.user.dto.UserLoginResponse
import com.budzet.domain.user.entity.User
import com.budzet.domain.user.repository.UserRepository
import com.budzet.global.exception.BusinessException
import com.budzet.global.exception.ErrorCode
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
class UserService(
    private val authTokenService: AuthTokenService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun join(email: String, password: String, name: String): User {
        if (userRepository.existsByEmail(email)) {
            throw BusinessException(ErrorCode.USER_CONFLICT)
        }
        return userRepository.save(User(email, requireNotNull(passwordEncoder.encode(password)), name))
    }

    @Transactional
    fun login(email: String, password: String): UserLoginResponse {
        val user = userRepository.findByEmail(email).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        checkPassword(password, user.password)
        val accessToken = authTokenService.genAccessToken(user)
        val refreshToken = authTokenService.genRefreshToken(user)
        user.updateRefreshToken(refreshToken)
        return UserLoginResponse.from(user, accessToken, refreshToken)
    }

    @Transactional
    fun logout(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        user.clearRefreshToken()
    }

    @Transactional
    fun refresh(refreshToken: String): TokenRefreshResponse {
        val payload = authTokenService.refreshPayloadOrNull(refreshToken)
            ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        val userId = (payload["id"] as Number).toLong()
        val user = userRepository.findById(userId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        if (user.refreshToken == null || user.refreshToken != refreshToken) {
            throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        }
        val newAccessToken = authTokenService.genAccessToken(user)
        val newRefreshToken = authTokenService.genRefreshToken(user)
        user.updateRefreshToken(newRefreshToken)
        return TokenRefreshResponse.from(newAccessToken, newRefreshToken)
    }

    fun checkPassword(inputPassword: String, encodedPassword: String) {
        if (!passwordEncoder.matches(inputPassword, encodedPassword)) {
            throw BusinessException(ErrorCode.PASSWORD_MISMATCH)
        }
    }

    fun accessPayloadOrNull(accessToken: String): Map<String, Any>? =
        authTokenService.accessPayloadOrNull(accessToken)

    fun findById(id: Long): Optional<User> = userRepository.findById(id)

    @Transactional
    fun updatePassword(userId: Long, currentPassword: String, newPassword: String) {
        val user = userRepository.findById(userId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        checkPassword(currentPassword, user.password)
        if (passwordEncoder.matches(newPassword, user.password)) {
            throw BusinessException(ErrorCode.PASSWORD_UNCHANGED)
        }
        user.updatePassword(requireNotNull(passwordEncoder.encode(newPassword)))
        user.clearRefreshToken()
    }

    @Transactional
    fun updateProfile(userId: Long, name: String): User {
        val user = userRepository.findById(userId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        user.updateName(name)
        return user
    }
}
