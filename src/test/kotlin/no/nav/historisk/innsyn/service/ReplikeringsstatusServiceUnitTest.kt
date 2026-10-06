package no.nav.historisk.innsyn.service

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.historisk.innsyn.model.Replikeringsstatus
import no.nav.historisk.innsyn.repository.ReplikeringsstatusRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

class ReplikeringsstatusServiceUnitTest {
    @Test
    fun `repositoryfeil setter oppdateringsmetrikken til null og kaster videre`() {
        val antallKall = AtomicInteger()
        val repository = Proxy.newProxyInstance(
            ReplikeringsstatusRepository::class.java.classLoader,
            arrayOf(ReplikeringsstatusRepository::class.java)
        ) { _, method, _ ->
            check(method.name == "findAll")
            if (antallKall.getAndIncrement() == 0) emptyList<Replikeringsstatus>()
            else throw IllegalStateException("databasefeil")
        } as ReplikeringsstatusRepository
        val registry = SimpleMeterRegistry()
        val service = ReplikeringsstatusService(
            repository,
            NamedParameterJdbcTemplate(DriverManagerDataSource()),
            registry
        )
        val gauge = registry.get("infotrygd_replikering_oppdatering_vellykket").gauge()
        assertThat(gauge.value()).isZero()

        service.oppdater()
        assertThat(gauge.value()).isEqualTo(1.0)
        assertThat(service.status()).isNotNull()

        assertThrows<IllegalStateException> { service.oppdater() }
        assertThat(gauge.value()).isZero()
        assertThat(service.status()).isNull()
    }
}
