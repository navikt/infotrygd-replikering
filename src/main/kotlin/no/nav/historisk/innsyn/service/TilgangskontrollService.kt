package no.nav.historisk.innsyn.service

import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.SoekeException
import no.nav.historisk.innsyn.model.value.Tabellnavn
import no.nav.historisk.innsyn.utils.TokenHelper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TilgangskontrollService(
    private val tokenHelper: TokenHelper,
    @Value("\${app.gruppe.admin}") private val gruppeAdmin: String
) {

    fun validerGruppetilgangForAdmin() {
        if(!harGruppetilgangForAdmin()) {
            throw SoekeException(Feilkode.IKKE_TILGANG_TIL_APPLIKASJON,
                "Mangler admintilgang til applikasjon", "Kontakt identansvarlig dersom du behøver denne tilgangen.")
        }
    }

    private fun harGruppetilgangForAdmin(): Boolean {
        val grupper = tokenHelper.grupper()
        return grupper.contains(gruppeAdmin)
    }
}