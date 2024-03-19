package no.nav.historisk.innsyn.model.value

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

data class Tabellnavn(val value: String) {
    init {
        require(value.matches("^[a-zA-Z0-9_]+\$".toRegex())) { "Ikke et gyldig tabellnavn: $value" }
    }

    override fun toString(): String {
        return value
    }
}

@Converter
class TabellnavnConverter : AttributeConverter<Tabellnavn?, String?> {
    override fun convertToDatabaseColumn(attribute: Tabellnavn?): String? {
        return attribute?.value
    }

    override fun convertToEntityAttribute(dbData: String?): Tabellnavn? {
        return dbData?.let { Tabellnavn(it) }
    }
}