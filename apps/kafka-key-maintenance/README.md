# Håndtering av id-merge fra PDL

## paw-kafka-key-maintenance:
Når merge oppdages ->
    0 aktive perioder -> 
        finnes det inaktive? 
            ja -> sender IdentitetsnummerSammenslaatt hendelse med arbeidssøkerId for den siste lukkede perioden
            nei -> sender IdentitetsnummerSammenslaatt hendelse med den nyeste arbeidssokerId
    1 aktiv periode -> velger arbeidssokerId som perioden er på merger til den og sender IdentitetsnummerSammenslaatt hendelse
    2+ aktive perioder -> manuell håndtering

Hendelsen IdentitetsnummerSammenslaatt sendes på Hendelse.id som matcher arbeidssøkerId som ikke lenger er gjeldende.

## paw-kafka-key-generator:
konsumerer hendelse ->
    IdentitetsnummerSammenslaatt -> 
        oppdaterer database med ny arbeidssokerId og lagrer historikk i egen id-tabell

## paw-hendelseprosessor:
konsumerer hendelse ->
    IdentitetsnummerSammenslaatt -> 
        hvis aktiv periode -> 
            avslutt aktiv periode med årsak "IdentitetsnummerSammenslaatt" og setter GjeldendeTilstand til "OPPHOERT"


