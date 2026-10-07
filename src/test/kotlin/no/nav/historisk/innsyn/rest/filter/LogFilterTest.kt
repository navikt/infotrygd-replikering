package no.nav.historisk.innsyn.rest.filter

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class LogFilterTest {
    @Test
    fun `klientstyrt consumer id lager ikke nye tidsserier`() {
        val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        val filter = LogFilter(registry, "infotrygd-replikering")

        for (consumerId in listOf("en-klient", "en-annen-klient")) {
            val request = MockHttpServletRequest("GET", "/actuator/health")
            request.addHeader("Nav-Consumer-Id", consumerId)
            filter.doFilter(request, MockHttpServletResponse(), FilterChain { _, _ -> })
        }

        val counters = registry.find("infotrygd-replikering_consumers").counters()
        assertThat(counters).hasSize(1)
        assertThat(counters.single().id.tags).isEmpty()
        assertThat(counters.single().count()).isEqualTo(2.0)
        assertThat(registry.scrape()).contains("infotrygd_replikering_consumers_total 2.0")
    }
}
