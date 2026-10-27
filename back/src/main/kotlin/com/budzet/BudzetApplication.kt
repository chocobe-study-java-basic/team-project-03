package com.budzet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.retry.annotation.EnableRetry

@EnableRetry
@EnableJpaAuditing
@SpringBootApplication
class BudzetApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<BudzetApplication>(*args)
        }
    }
}
