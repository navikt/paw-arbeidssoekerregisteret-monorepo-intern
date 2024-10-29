```mermaid
sequenceDiagram
    Registeret->>PeriodeTopic: Periode startet
    PeriodeTopic-->>MeldepliktTjeneste: Periode startet
    MeldepliktTjeneste-->>EndringerTopic: Melding X dager før forfall
    MeldepliktTjeneste-->>EndringerTopic: Melding OK ved innsending
    MeldepliktTjeneste-->>EndringerTopic: Frist utløpt
    MeldepliktTjeneste-->>EndringerTopic: Graceperiode utløpt
    PeriodeTopic-->>MeldepliktTjeneste: Periode avsluttet
    MeldepliktTjeneste-->>EndringerTopic: Periode avsluttet
```

```mermaid
sequenceDiagram
    Registeret->>PeriodeTopic: Periode startet
    Dagpenger-->>BekreftelsePaaVegneAvTopic: Dagpenger startet
    BekreftelsePaaVegneAvTopic-->>BekreftelseTjeneste: Dagpenger tar over
```

```
 periode topic:
    startet: lagre initiell tilstand
    avsluttet: send ok(avsluttet)
        :slett tilstand
 
 bekreftelse paaVegneAv topic:
    starter paaVegneAv: lagre paaVegneAv info
        :sett tidspunkt for siste melding til record ts for paaVegneAv endring
    stopper paaVegneAv: slett paaVegneAv info
    
 bekreftelse melding topic:
    mottatt: lagre tidspunkt for siste melding
        :send OK(mottatt)
        dersom ikke ønsker å fortsette:
            :send VilAvsluttePerioden
        

 hver x time:
    for alle perioder ingen har bekreftelse paaVegneAv for:
        dersom tid siden siste melding (eller periode start) > Y dager:
            send melding om frist nærmer seg            
        dersom tid siden siste melding (eller periode start) > Z dager:
            send melding om frist utløpt
        dersom tid siden siste melding (eller periode start) > Z+Grace dager:
            send melding om graceperiode utløpt
    for alle perioder andre sender bekreftelse paaVegneAv for:
        dersom tid siden siste melding (eller periode start) > Z+G+1dag dager:
            send melding om graceperiode utløpt
                :stopp paaVegneAv, vi overtar
```

Modul: endringer til frontend
```
 meldings topic:
   - OK(mottatt) -> sett oppgave fullført
   - OK(avsluttet) -> slett oppgave kansellert
   - OK(paaVegneAv) -> sett oppgave fullført
   - frist nærmer seg -> opprett oppgave
   - frist utløpt -> opprett oppgave
   - graceperiode utløpt -> ingenting   
```

Modul: endringer til eventlogg
```
 meldings topic:
   - VilAvsluttePerioden -> send avslutt hendelse til eventlogg 
   
```
* OK (grunn: BekreftelseMottatt, PaaVegneAv Start, PeriodeAvsluttet)
* 