package com.budzet.global.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity, customAuthenticationFilter: CustomAuthenticationFilter): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/users/join", "/users/login", "/users/refresh").permitAll()
                    .anyRequest().authenticated()
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { it.disable() }
            .cors(withDefaults())
            .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { handling ->
                handling
                    .authenticationEntryPoint { _, response, _ ->
                        response.contentType = "application/json; charset=UTF-8"
                        response.status = 401
                        response.writer.write(
                            """
                            {
                                "resultCode": "401",
                                "errorCode": "UNAUTHORIZED",
                                "message": "로그인 후 이용해주세요."
                            }
                            """.trimIndent(),
                        )
                    }
                    .accessDeniedHandler { _, response, _ ->
                        response.contentType = "application/json; charset=UTF-8"
                        response.status = 403
                        // 기존 쉼표 누락 결함은 별도 수정 대상으로 기록했다.
                        response.writer.write(
                            """
                            {
                                "resultCode": "403",
                                "errorCode": "FORBIDDEN"
                                "message": "권한이 없습니다."
                            }
                            """.trimIndent(),
                        )
                    }
            }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    // 개발·배포 프론트엔드 CORS 설정
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf(
                "http://localhost:3000",
                "https://team-project-02.vercel.app",
                "https://budzet.chocobe.cloud",
            )
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
