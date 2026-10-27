package com.budzet.global.springDoc

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(info = Info(
    title = "Budzet API 서버",
    version = "beta",
    description = "Budzet API 서버 문서입니다.",
))
@SecurityScheme(name = "BearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class SpringDoc {
    @Bean
    fun allApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("00. 전체 API")
        .pathsToMatch("/**")
        .build()

    @Bean
    fun userApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("01. 회원 & 인증")
        .pathsToMatch("/users/**")
        .build()

    @Bean
    fun roomApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("02. 모임방 & 맴버")
        .pathsToMatch("/rooms", "/rooms/{roomId}", "/rooms/{roomId}/members/**")
        .build()

    @Bean
    fun budgetApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("03. 예산관리 & 예산 신청 & 정산")
        .pathsToMatch("/rooms/{roomId}/budget/**")
        .build()

    @Bean
    fun inviteApi(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("04. 초대")
        .pathsToMatch("/invites/**", "/rooms/{roomId}/invites/**", "/rooms/join/**")
        .build()
}
