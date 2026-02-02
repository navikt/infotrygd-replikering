package no.nav.historisk.innsyn.testutil.rest.filter

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestConfig {
    @Bean
    fun meterRegistry() = SimpleMeterRegistry()
}