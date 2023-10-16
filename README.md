# paw-arbeidssokerregisteret

```mermaid
graph LR
    %% Start bruker
    Startpunkt -->|HTTP POST| ApiStart
    ApiStart --> SjekkOppholdstillatelse
    SjekkOppholdstillatelse --> OppholdstillatelseOK
    SjekkOppholdstillatelse -->OppholdstillatelseIkkeOK
    OppholdstillatelseIkkeOK -->|HTTP 403 Forbidden| Startpunkt
    OppholdstillatelseOK --> SendStartMelding
    OppholdstillatelseOK --> |HTTP 202 Accepted| Startpunkt

    %% Stopp bruker
    Startpunkt -->|HTTP PUT| ApiStop
    ApiStop --> SendStoppMelding
    SendStoppMelding --> |HTTP 202 Accepted| Startpunkt

    Startpunkt[Arbeidssøkerregistrering frontend]
    ApiStart["/api/v1/arbeidssoker/perioder"]
    ApiStop["/api/v1/arbeidssoker/perioder"]
    SendStartMelding[Send start-melding til Kafka-topic paw.arbeidssokerperiode-v1]
    OppholdstillatelseOK[Oppholdstillatelse OK]:::ok
    OppholdstillatelseIkkeOK[Oppholdstillatelse ikke OK]:::nok
    SjekkOppholdstillatelse{Er oppholdstillatelse OK i PDL}
    SendStoppMelding[Send stopp-melding til Kafka-topic paw.arbeidssokerperiode-v1]

    classDef ok fill:#2ecc71, color:#000000, stroke:#000000;
    classDef nok fill:#f51841, color:#000000, stroke:#000000;
```