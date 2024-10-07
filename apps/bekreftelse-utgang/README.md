# paw-arbeidssoekerregisteret-bekreftelse-utgang

## Beskrivelse
Denne appen er ansvarlig for avslutting av arbeidssøkerperioder når bekreftelse grace periode er utløpt
eller bruker svarer "nei" på bekreftelse spørsmål "Vil du fortsatt være registrert som arbeidssøker?".

Appen lytter på events fra topic `paw.arbeidssoker-bekreftelse-hendelseslogg-beta-v2`
og sender `Avsluttet` hendelser til `paw.arbeidssoker-hendelseslogg-v1` for `RegisterGracePeriodeUtloept`- og `BaOmAaAvsluttePeriode`-hendelser.