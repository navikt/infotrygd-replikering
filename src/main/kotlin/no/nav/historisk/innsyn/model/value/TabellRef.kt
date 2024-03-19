package no.nav.historisk.innsyn.model.value

data class TabellRef(val schemanavn: Schemanavn, val tabellnavn: Tabellnavn) {
    override fun toString(): String {
        return "$schemanavn.$tabellnavn"
    }
}
