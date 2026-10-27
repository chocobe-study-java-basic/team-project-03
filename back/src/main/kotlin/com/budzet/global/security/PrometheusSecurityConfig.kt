package com.budzet.global.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
@Profile("!prod & !test")
class PrometheusSecurityConfig {
    @Bean
    @Order(1)
    fun prometheusFilterChain(
        http: HttpSecurity,
        passwordEncoder: PasswordEncoder,
        @Value("\${custom.monitoring.password:}") password: String,
    ): SecurityFilterChain {
        // 인증 관리자를 이 체인 안에만 두어 일반 회원의 인증과 섞이지 않게 한다.
        val users = InMemoryUserDetailsManager()
        // 기존 Java String.isBlank()의 Unicode 공백 판정을 유지한다.
        if (!password.codePoints().allMatch(Character::isWhitespace)) {
            users.createUser(User.withUsername("prometheus")
                .password(requireNotNull(passwordEncoder.encode(password)))
                .roles("PROMETHEUS")
                .build())
        }
        val provider = DaoAuthenticationProvider(users).apply {
            setPasswordEncoder(passwordEncoder)
        }
        http.securityMatcher(*PATHS)
            .authenticationManager(ProviderManager(provider))
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .requestCache { it.disable() }
            .httpBasic(withDefaults())
            .exceptionHandling { exceptions ->
                exceptions.accessDeniedHandler { _, response, _ -> response.status = 403 }
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(HttpMethod.GET, *PATHS).hasRole("PROMETHEUS")
                    .requestMatchers(HttpMethod.HEAD, *PATHS).hasRole("PROMETHEUS")
                    .anyRequest().denyAll()
            }
        return http.build()
    }

    companion object {
        private val PATHS = arrayOf("/actuator/prometheus", "/actuator/prometheus/**")
    }
}
