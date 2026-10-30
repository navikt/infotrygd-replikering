package no.nav.historisk.innsyn.exception

import io.opentelemetry.api.trace.Span

data class Feilmelding(
    val kode: String,
    val beskrivelse: String,
    val detaljertBeskrivelse: String
) {
    val callId: String? = Span.current().spanContext.takeIf { it.isValid }?.traceId
}