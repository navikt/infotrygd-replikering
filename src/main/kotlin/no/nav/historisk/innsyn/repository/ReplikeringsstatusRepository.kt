package no.nav.historisk.innsyn.repository

import no.nav.historisk.innsyn.model.Replikeringsstatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReplikeringsstatusRepository : JpaRepository<Replikeringsstatus, Long> {

}