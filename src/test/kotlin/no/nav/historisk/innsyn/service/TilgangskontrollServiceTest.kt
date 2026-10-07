package no.nav.historisk.innsyn.service

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.historisk.innsyn.utils.TokenHelper
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [TilgangskontrollService::class, TilgangskontrollServiceTest.TokenHelperTestConfiguration::class],
    properties = [
        "app.gruppe.admin=gruppeAdmin"
    ]
)
@ActiveProfiles("test")
internal class TilgangskontrollServiceTest {
    @Autowired
    private lateinit var tilgangskontrollService: TilgangskontrollService

    @Autowired
    private lateinit var tokenHelper: TestTokenHelper

    @BeforeEach
    fun resetGroups() {
        tokenHelper.groups = emptyList()
    }

    @Test
    internal fun `skal ikke kaste exception dersom bruker er medlem av admin-gruppe`() {
        tokenHelper.groups = listOf("gruppeAdmin")

        tilgangskontrollService.validerGruppetilgangForAdmin()
    }

    @Test
    internal fun `skal kaste exception dersom bruker ikke er medlem av admin-gruppe`() {
        tokenHelper.groups = listOf("urelatert-gruppe")

        val e = assertThrows<SoekeException> { tilgangskontrollService.validerGruppetilgangForAdmin() }
        Assertions.assertThat(e.feilkode).isEqualTo(Feilkode.IKKE_TILGANG_TIL_APPLIKASJON)
    }

    @Test
    internal fun `logger ikke gruppemedlemskap selv om debug er aktivert`() {
        val logger = LoggerFactory.getLogger(TilgangskontrollService::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            Assertions.assertThat(logger.isDebugEnabled).isTrue()
            tokenHelper.groups = listOf("sensitiv-gruppe", "gruppeAdmin")

            tilgangskontrollService.validerGruppetilgangForAdmin()

            Assertions.assertThat(appender.list.map { it.formattedMessage })
                .noneMatch { it.contains("sensitiv-gruppe") }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    internal class TokenHelperTestConfiguration {
        @Bean
        fun tokenHelper() = TestTokenHelper()
    }

    internal class TestTokenHelper : TokenHelper(object : TokenValidationContextHolder {
        override fun getTokenValidationContext(): TokenValidationContext =
            error("Token context must not be used in this test")

        override fun setTokenValidationContext(tokenValidationContext: TokenValidationContext?) {
            error("Token context must not be used in this test")
        }
    }) {
        var groups: List<String> = emptyList()

        override fun grupper(): List<String> = groups
    }
}