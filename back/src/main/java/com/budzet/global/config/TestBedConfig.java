package com.budzet.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!prod & !test")
@RequiredArgsConstructor
public class TestBedConfig {

    private final TestBedInitializer testBedInitializer;

    @Bean
    ApplicationRunner testBedRunner() {
        return arguments -> testBedInitializer.seed();
    }
}
