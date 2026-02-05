package no.nav.historisk.innsyn.config

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MeterRegistryConfig {
    @Bean
    fun meterRegistry() = SimpleMeterRegistry()
}