package com.budzet.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@Profile("!prod & !test")
public class PrometheusSecurityConfig {

    private static final String[] PATHS = {"/actuator/prometheus", "/actuator/prometheus/**"};

    @Bean
    @Order(1)
    SecurityFilterChain prometheusFilterChain(
            HttpSecurity http,
            PasswordEncoder passwordEncoder,
            @Value("${custom.monitoring.password:}") String password
    ) throws Exception {
        // 인증 관리자를 이 체인 안에만 두어 일반 회원의 인증과 섞이지 않게 한다.
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        if (!password.isBlank()) {
            users.createUser(User.withUsername("prometheus")
                    .password(passwordEncoder.encode(password))
                    .roles("PROMETHEUS")
                    .build());
        }
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwordEncoder);

        http.securityMatcher(PATHS)
                .authenticationManager(new ProviderManager(provider))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .httpBasic(withDefaults())
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) -> response.setStatus(403)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, PATHS).hasRole("PROMETHEUS")
                        .requestMatchers(HttpMethod.HEAD, PATHS).hasRole("PROMETHEUS")
                        .anyRequest().denyAll());

        return http.build();
    }
}
