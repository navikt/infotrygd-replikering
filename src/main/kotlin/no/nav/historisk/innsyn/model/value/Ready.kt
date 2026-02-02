package no.nav.historisk.innsyn.model.value

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class BooleanToIntConverter : AttributeConverter<Boolean, Int> {
    override fun convertToDatabaseColumn(attribute: Boolean?): Int? =
        when (attribute) {
            null -> null
            true -> 1
            false -> 0
        }

    override fun convertToEntityAttribute(dbData: Int?): Boolean? =
        when (dbData) {
            null -> null
            1 -> true
            0 -> false
            else -> throw IllegalArgumentException("Invalid value for Boolean: $dbData")
        }
}
