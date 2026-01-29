package no.nav.historisk.innsyn.testutil.annotation

import no.nav.historisk.innsyn.testutil.oracle.OracleContainerInitializer
import no.nav.historisk.innsyn.testutil.oracle.OracleContainerTruncateExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = [OracleContainerInitializer::class])
@ExtendWith(OracleContainerTruncateExtension::class)
@Import(JacksonAutoConfiguration::class)
@ActiveProfiles("test")
annotation class ServiceTest