package no.nav.paw.arbeidssokerregisteret.api.extensions

import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.JaNeiVetIkke

fun JaNeiVetIkke?.toNullableInternalApi(): no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke? =
    when (this) {
        null -> null
        JaNeiVetIkke.JA -> no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke.JA
        JaNeiVetIkke.NEI -> no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke.NEI
        JaNeiVetIkke.VET_IKKE -> no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke.VET_IKKE
    }

fun JaNeiVetIkke.toInternalApi(): no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke =
    when (this) {
        JaNeiVetIkke.JA -> no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke.JA
        JaNeiVetIkke.NEI -> no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke.NEI
        JaNeiVetIkke.VET_IKKE -> no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke.VET_IKKE
    }
