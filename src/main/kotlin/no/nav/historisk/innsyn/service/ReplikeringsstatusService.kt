package no.nav.historisk.innsyn.service

import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import jakarta.transaction.Transactional
import no.nav.historisk.innsyn.model.value.TabellRef
import no.nav.historisk.innsyn.repository.ReplikeringsstatusRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ReplikeringsstatusService(
    private val replikeringsstatusRepository: ReplikeringsstatusRepository,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val registry: MeterRegistry
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val statusHolder: ReplikeringsstatistikkHolder = ReplikeringsstatistikkHolder(null)

    fun status(): Replikeringsstatistikk? {
        return statusHolder.replikeringsstatistikk
    }

    private val initialisert: MutableSet<TabellRef> = mutableSetOf()

    @Scheduled(fixedDelay = 1000 * 30)
    @Transactional
    fun oppdater() {
        logger.info("Oppdater")

        val metrikker = replikeringsstatusRepository.findAll()
            .filter { it.ready }
            .map {
                try {
                    it.tabellRef to TabellMetrikker(
                        sistOppdatering = finnSistOppdatert(it.tabellRef)
                    )
                } catch (e: Exception) {
                    logger.error("Kunne ikke lese statistikk for tabell", e)
                    null
                }
        }.filterNotNull().toMap()

        statusHolder.replikeringsstatistikk =
            Replikeringsstatistikk(
                metrikker = metrikker
            )

        for (tabell in metrikker.keys) {
            if (!initialisert.contains(tabell)) {
                gauge(tabell)
                initialisert.add(tabell)
            }
        }
    }

    private fun gauge(tabell: TabellRef) {
        logger.info("Initialiserer metrics for tabell $tabell")
        val tags = listOf(ImmutableTag("tabell", tabell.toString()))
        registry.gauge("infotrygd_replikering_tabellforsinkelse", tags, statusHolder) {
            val status = it.replikeringsstatistikk?.metrikker?.get(tabell)?.sistOppdatering?.antallMillisekunder
            status?.toDouble() ?: 0.0
        }
    }

    private fun finnSistOppdatert(tabellRef: TabellRef): DatabaseTimestamp {
        val rowMapper = RowMapper { rs, rowNum ->
            DatabaseTimestamp(
                timestamp = rs.getTimestamp("OPPDATERT")?.toInstant(),
                currentTimestamp = rs.getTimestamp("TS").toInstant()
            )
        }

        //lang=sql
        val sql = """select max(OPPDATERT) as OPPDATERT, current_timestamp as TS from $tabellRef"""
        val rader = jdbcTemplate.query(sql, emptyMap<String, Any>(), rowMapper)
        return rader[0]
    }

}

data class ReplikeringsstatistikkHolder(
    @get:Synchronized @set:Synchronized
    var replikeringsstatistikk: Replikeringsstatistikk?
)

data class Replikeringsstatistikk(
    val metrikker: Map<TabellRef, TabellMetrikker>
)

data class TabellMetrikker(
    val sistOppdatering: DatabaseTimestamp,
)

data class DatabaseTimestamp(
    val timestamp: Instant?,
    val currentTimestamp: Instant
) {
    val antallMillisekunder: Long? = timestamp?.let {
        currentTimestamp.toEpochMilli() - it.toEpochMilli()
    }
}