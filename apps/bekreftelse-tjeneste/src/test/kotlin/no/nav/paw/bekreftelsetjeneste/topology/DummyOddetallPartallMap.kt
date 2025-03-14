package no.nav.paw.bekreftelsetjeneste.topology

import no.nav.paw.bekreftelsetjeneste.startdatohaandtering.*
import no.nav.paw.model.Identitetsnummer

class DummyOddetallPartallMap : OddetallPartallMap {
    override operator fun get(identitetsnummer: Identitetsnummer): Ukenummer {
        return when {
            identitetsnummer.verdi.last() in setOf('1', '3', '5', '7') -> Oddetallsuke
            identitetsnummer.verdi.last() in setOf('0', '2', '4', '6', '8') -> Partallsuke
            else -> Ukjent
        }
    }
}