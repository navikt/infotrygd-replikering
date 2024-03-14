package no.nav.historisk.innsyn.service

import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.utils.TokenHelper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TilgangskontrollService(
    private val tokenHelper: TokenHelper,
    @Value("\${app.gruppe.admin}") private val gruppeAdmin: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun validerGruppetilgangForAdmin() {
        if(!harGruppetilgangForAdmin()) {
            logger.info("Mangler tilgang til følgende gruppe: \"$gruppeAdmin\"")
            throw SoekeException(Feilkode.IKKE_TILGANG_TIL_APPLIKASJON,
                "Mangler admintilgang til applikasjon", "Kontakt identansvarlig dersom du behøver denne tilgangen.")
        }
    }

    private fun harGruppetilgangForAdmin(): Boolean {
        val grupper = tokenHelper.grupper()
        logger.debug("Fikk følgende grupper fra Azure: $grupper")
        return grupper.contains(gruppeAdmin)
    }
}