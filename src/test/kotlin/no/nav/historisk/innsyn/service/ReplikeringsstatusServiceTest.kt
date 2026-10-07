package no.nav.historisk.innsyn.service

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.historisk.innsyn.model.Replikeringsstatus
import no.nav.historisk.innsyn.model.value.Schemanavn
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.repository.ReplikeringsstatusRepository
import no.nav.historisk.innsyn.testutil.annotation.ServiceTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@ServiceTest
//@Transactional
class ReplikeringsstatusServiceTest {

    @Autowired
    private lateinit var replikeringsstatusService: ReplikeringsstatusService

    @Autowired
    private lateinit var replikeringsstatusRepository: ReplikeringsstatusRepository

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var registry: MeterRegistry

    @Autowired
    private lateinit var prometheusRegistry: PrometheusMeterRegistry

    @Test
    fun `henter siste timestamp`() {
        val replikeringsstatusEntity = Replikeringsstatus(
            schema = Schemanavn("testtabeller"),
            tabell = Tabellnavn("testtabell"),
            ready = true
        )

        val feilaktigReplikeringsstatusEntity = Replikeringsstatus(
            schema = Schemanavn("testtabeller"),
            tabell = Tabellnavn("ikke_eksisterende_tabell"),
            ready = true
        )
        replikeringsstatusRepository.saveAllAndFlush(listOf(replikeringsstatusEntity, feilaktigReplikeringsstatusEntity))

        val foerste = OffsetDateTime.parse("2020-01-01T11:01:00Z")
        val siste = foerste.plusSeconds(32)

        insertTesttabell(foerste)
        insertTesttabell(siste)

        replikeringsstatusService.oppdater()
        val statistikk = replikeringsstatusService.status()
        assertThat(statistikk?.metrikker?.get(replikeringsstatusEntity.tabellRef)?.sistOppdatering?.timestamp?.atOffset(ZoneOffset.UTC)?.second)
            .isEqualTo(siste.second)
        assertThat(registry.get("infotrygd_replikering_tabellforsinkelse")
            .tag("tabell", replikeringsstatusEntity.tabellRef.toString()).gauge().value()).isPositive()
        assertThat(registry.get("infotrygd_replikering_oppdatering_vellykket").gauge().value()).isZero()
    }

    @Test
    fun `sist oppdatert timestamp er null når vi ser på en tom tabell`() {
        val replikeringsstatusEntity = Replikeringsstatus(
            schema = Schemanavn("testtabeller"),
            tabell = Tabellnavn("testtabell"),
            ready = true
        )
        replikeringsstatusRepository.saveAndFlush(replikeringsstatusEntity)

        replikeringsstatusService.oppdater()
        val statistikk = replikeringsstatusService.status()
        val sistOppdatering = statistikk?.metrikker?.get(replikeringsstatusEntity.tabellRef)?.sistOppdatering
        assertThat(sistOppdatering).isNotNull()
        assertThat(sistOppdatering?.timestamp).isNull()
        assertThat(registry.get("infotrygd_replikering_tabellforsinkelse")
            .tag("tabell", replikeringsstatusEntity.tabellRef.toString()).gauge().value()).isNaN()
        assertThat(prometheusRegistry.scrape())
            .contains("infotrygd_replikering_tabellforsinkelse{tabell=\"testtabeller.testtabell\"} NaN")
        assertThat(registry.get("infotrygd_replikering_oppdatering_vellykket").gauge().value()).isEqualTo(1.0)
    }

    @Test
    fun `deaktivert tabell viser ikke null forsinkelse`() {
        val tabell = Replikeringsstatus(
            schema = Schemanavn("testtabeller"),
            tabell = Tabellnavn("testtabell"),
            ready = true
        )
        replikeringsstatusRepository.saveAndFlush(tabell)
        insertTesttabell(OffsetDateTime.parse("2020-01-01T11:01:00Z"))
        replikeringsstatusService.oppdater()
        val gauge = registry.get("infotrygd_replikering_tabellforsinkelse")
            .tag("tabell", tabell.tabellRef.toString()).gauge()
        assertThat(gauge.value()).isPositive()

        replikeringsstatusRepository.deleteAll()
        replikeringsstatusRepository.saveAndFlush(Replikeringsstatus(
            schema = Schemanavn("testtabeller"),
            tabell = Tabellnavn("testtabell"),
            ready = false
        ))
        replikeringsstatusService.oppdater()

        assertThat(gauge.value()).isNaN()
    }

    private fun insertTesttabell(timestamp: OffsetDateTime) {
        //lang=sql
        val insertSql = """insert into testtabeller.testtabell (OPPDATERT) values (:oppdatert)"""

        jdbcTemplate.update(
            insertSql, mapOf<String, Any>(
                "oppdatert" to timestamp
            )
        )
    }
}
