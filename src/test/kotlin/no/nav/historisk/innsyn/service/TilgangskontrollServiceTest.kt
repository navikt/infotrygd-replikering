package no.nav.historisk.innsyn.service

import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.historisk.innsyn.utils.TokenHelper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(
    classes = [ TilgangskontrollService::class
     ],
    properties = [
        "app.gruppe.admin=gruppeAdmin"
    ]
)
@ActiveProfiles("test")
internal class TilgangskontrollServiceTest {
    @Autowired
    private lateinit var tilgangskontrollService: TilgangskontrollService

    @MockitoBean
    private lateinit var tokenHelper: TokenHelper

    @Test
    internal fun `skal ikke kaste exception dersom bruker er medlem av admin-gruppe`() {
        `when`(tokenHelper.grupper()).thenReturn(listOf("gruppeAdmin"))

        tilgangskontrollService.validerGruppetilgangForAdmin()
    }

    @Test
    internal fun `skal kaste exception dersom bruker ikke er medlem av admin-gruppe`() {
        `when`(tokenHelper.grupper()).thenReturn(listOf("urelatert-gruppe"))

        val e = assertThrows<SoekeException> { tilgangskontrollService.validerGruppetilgangForAdmin() }
        Assertions.assertThat(e.feilkode).isEqualTo(Feilkode.IKKE_TILGANG_TIL_APPLIKASJON)
    }


}