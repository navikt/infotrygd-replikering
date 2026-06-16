package no.nav.historisk.innsyn

import no.nav.historisk.innsyn.testutil.annotation.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestTemplate

@IntegrationTest
class ApplicationTest {

    @LocalServerPort
    var port: Int = 0

    @Test
    fun contextLoads() {
    }

    @Test
    fun health() {
        RestTemplate().getForObject("http://localhost:$port/actuator/health", String::class.java)
    }
}
