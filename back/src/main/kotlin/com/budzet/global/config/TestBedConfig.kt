package com.budzet.global.config

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!prod & !test")
class TestBedConfig(private val testBedInitializer: TestBedInitializer) {
    @Bean
    fun testBedRunner(): ApplicationRunner = ApplicationRunner { testBedInitializer.seed() }
}
