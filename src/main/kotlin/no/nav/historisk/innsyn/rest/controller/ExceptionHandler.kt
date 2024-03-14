package no.nav.historisk.innsyn.rest.controller

import no.nav.historisk.innsyn.exception.Feilkode
import no.nav.historisk.innsyn.exception.Feilmelding
import no.nav.historisk.innsyn.exception.KildesystemException
import no.nav.historisk.innsyn.exception.SoekeException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class ExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler
    fun kildesystemException(e: KildesystemException) : ResponseEntity<Feilmelding> {
        val detaljertBeskrivelse = when(e.status) {
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.TOO_MANY_REQUESTS -> {
                "Tjenesten er midlertidig utilgjengelig. Prøv på nytt om noen minutter."
            }
            else -> "Noe har gått galt. Ta kontakt med support dersom problemet vedvarer."
        }

        logger.warn("Noe gikk galt ved kall til kildesystem (${e.kildesystem})", e)
        return ResponseEntity.status(e.status ?: HttpStatus.INTERNAL_SERVER_ERROR).body(
            Feilmelding(
                kode = Feilkode.SERVERFEIL.kode,
                beskrivelse = "En feil oppstod ved kall til ${e.kildesystem}",
                detaljertBeskrivelse = detaljertBeskrivelse
            ))
    }

    @ExceptionHandler
    fun soekeException(e: SoekeException) : ResponseEntity<Feilmelding> {
        logger.info("Kunne ikke søke opp person", e)
        return ResponseEntity.status(e.status)
            .body(e.feilmelding)
    }

    @ExceptionHandler
    fun httpMediaTypeNotSupportedException(e: HttpMediaTypeNotSupportedException) : ResponseEntity<Feilmelding> {
        val feilkode = Feilkode.KLIENTFEIL
        return ResponseEntity.status(feilkode.status)
            .body(Feilmelding(
                kode = feilkode.kode,
                beskrivelse = "Noe gikk galt",
                detaljertBeskrivelse = e.message!!
            ))
    }

    @ExceptionHandler
    fun exception(e: Exception): ResponseEntity<Feilmelding> {
        if(e.javaClass.isAnnotationPresent(ResponseStatus::class.java)) {
            throw e
        }
        logger.warn("Uhåndtert exception", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                Feilmelding(
                    kode = Feilkode.SERVERFEIL.kode,
                    beskrivelse = "Noe gikk galt.",
                    detaljertBeskrivelse = "Dette skyldes en feil i applikasjonen. Vennligst ta kontakt med support."
            ))
    }
}