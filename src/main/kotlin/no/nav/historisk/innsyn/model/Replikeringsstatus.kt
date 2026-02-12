package no.nav.historisk.innsyn.model

import jakarta.persistence.*
import no.nav.historisk.innsyn.model.value.*

@Entity
@Table(name = "replikering_status")
class Replikeringsstatus(
    @Column(name = "SCHEMA_NAME")
    @Convert(converter = SchemanavnConverter::class)
    val schema: Schemanavn,

    @Column(name = "TABLE_NAME")
    @Convert(converter = TabellnavnConverter::class)
    val tabell: Tabellnavn,

    @Column(name = "READY")
    @Convert(converter = BooleanToIntConverter::class)
    val ready: Boolean
) {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column(name = "ID_REP_STATUS")
    private val id: Long? = null

    val tabellRef: TabellRef
        get() = TabellRef(schema, tabell)
}