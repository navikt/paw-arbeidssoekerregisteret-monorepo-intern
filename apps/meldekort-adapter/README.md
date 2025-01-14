# Adapter for gamle meldekort
Adapter skal konvertere meldekort fra gammelt format til nytt format.

## Info, copy paste fra slack
```
Hei.
Arena leser meldekort fra Kafka topics aapen-meldeplikt-meldekortgodkjentalle-v1-{{env}} og privat-meldeplikt-meldekortgodkjent-v1-{{env}}
Jeg ser at team PAW har tilgang til den første
- team: paw
  application: veilarbregistrering
  access: read
  Jeg kan gi tilgang også til den andre slik at dere kan lese alt vi sender til Arena
  14:22
  Til privat-meldeplikt-meldekortgodkjent-v1-{{env}} sender vi
  Kafkahendelse.builder()
  .kontrollertMeldekortId(kontrollMeldekort.getId())
  .korrelasjonId(MdcOperations.get(MdcOperations.MDC_CORRELATION_ID))
  .status(Kafkahendelse.STATUS_NY)
  .statusTidspunkt(LocalDateTime.now())
  .hendelse(kafkaMeldekort.tilJson())
  .type(Kafkahendelse.Type.KONTROLLERT_MK)
  .build();
  14:22
  hvor kafkaMeldekort er
  public class KafkaMeldekort {

  @JsonIgnore
  private Long personId;

  private Long hendelseId;

  private Long meldekortId;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  private LocalDateTime opprettet;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate meldedato;

  private String kommentar;

  private Boolean arbeidssoker;

  private Boolean arbeidet;

  private Boolean syk;

  private Boolean annetFravaer;

  private Boolean kurs;

  private String kontrollStatus;

  private List<KafkaMeldekortDag> meldekortDager;

  public String tilJson() {
  ObjectMapper objectMapper = new ObjectMapper()
  .registerModule(new JavaTimeModule())
  .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  try {
  return objectMapper.writeValueAsString(this);
  } catch (JsonProcessingException e) {
  throw new TekniskException("Feil under mapping av KafkaMeldekort fra objekt til JSON; feilmelding=" + e.getMessage(),
  e);
  }
  }
  }
  14:23
  hvor KafkaMeldekortDag er
  public class KafkaMeldekortDag {

  private Boolean syk;

  private Double arbeidetTimerSum;

  private Boolean annetFravaer;

  private Boolean kurs;

  private Integer dag; // Dager er nummerert fra 1 og oppover

  private String meldegruppe;
  }


Nils Martin Sande
14:53
'veilarbregistrering', registrering er død og begravet, jeg sender et nytt app navn som skal ha tilgang i morgen (må finne ut hva den skal hete først). Stemmer det at:
1. Boolean arbeidssoeker -> svar om om vedkommende fremdeles vil være arbeidssøker.
2. Boolean arbeidet -> svar på om vedkommende har jobbet i den aktuelle perioden


Igor Shuliakov
14:59
1. Ja
2. Ja
```