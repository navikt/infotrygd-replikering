package no.nav.historisk.innsyn.model.value

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

data class Schemanavn(val value: String) {
    init {
        require(value.matches("^[a-zA-Z0-9_]+\$".toRegex())) { "Ikke et gyldig schemanavn: $value" }
    }

    override fun toString(): String {
        return value
    }
}

@Converter
class SchemanavnConverter : AttributeConverter<Schemanavn?, String?> {
    override fun convertToDatabaseColumn(attribute: Schemanavn?): String? {
        return attribute?.value
    }

    override fun convertToEntityAttribute(dbData: String?): Schemanavn? {
        return dbData?.let { Schemanavn(it) }
    }
}