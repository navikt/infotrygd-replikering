package no.nav.historisk.innsyn.rest.filter

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class LogFilterTest {
    @Test
    fun `request uri saniteres i feillogg naar setting av mdc feiler`() {
        val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        val filter = LogFilter(registry, "infotrygd-replikering")
        val logger = LoggerFactory.getLogger(LogFilter::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)

        try {
            val requestUris = mapOf(
                "/oppslag" to "/oppslag",
                "/oppslag\r\nforfalsket" to "/oppslag__forfalsket",
                "/oppslag\t\u0000\u0085" to "/oppslag___"
            )
            for ((requestUri, expected) in requestUris) {
                val request = object : MockHttpServletRequest("GET", requestUri) {
                    override fun getHeader(name: String): String? {
                        if (name == "Nav-Consumer-Id") {
                            throw IllegalStateException("Testfeil ved lesing av header")
                        }
                        return super.getHeader(name)
                    }
                }

                filter.doFilter(request, MockHttpServletResponse(), FilterChain { _, _ -> })

                val event = appender.list.last { it.level == Level.WARN }
                assertThat(event.argumentArray[0]).isEqualTo(expected)
                assertThat(event.formattedMessage)
                    .contains(expected)
                    .doesNotContain("\r", "\n", "\t", "\u0000", "\u0085")
            }
            assertThat(appender.list.filter { it.level == Level.WARN }).hasSize(requestUris.size)
        } finally {
            logger.detachAppender(appender)
            appender.stop()
            registry.close()
        }
    }

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
