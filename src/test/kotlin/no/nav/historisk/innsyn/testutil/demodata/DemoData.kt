package no.nav.historisk.innsyn.testutil.demodata

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
@Profile("demoData")
class DemoData(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        // lang=sql
        val inserts: List<String> = listOf(
            """insert into replikering_status (schema_name, table_name, ready) values ('testtabeller', 'testtabell', 1)""",
            """insert into replikering_status (schema_name, table_name, ready) values ('testtabeller', 'ikke_eksisterende_tabell', 1)""",
            """insert into testtabeller.testtabell (oppdatert) values (current_timestamp)"""
        )
        inserts.forEach {
            jdbcTemplate.jdbcTemplate.execute(it)
        }
    }
}