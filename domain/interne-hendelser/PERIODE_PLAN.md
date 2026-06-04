# Periode-schema: utvidelse med avslutningsårsak

## Kontekst

`Avsluttet`-hendelsen har fått `avsluttetAarsak: AvsluttetAarsak?`. Denne propageres ikke videre
til den eksterne `Periode`-meldingen i dag. Denne planen beskriver hvordan vi gjør det, og
etablerer et mønster for fremtidige utvidelser av Periode-schemat.

---

## FULL_TRANSITIVE — hardt krav for Periode-topic

| Regel | Konsekvens |
|-------|-----------|
| Nye felt må ha `default: null` | Gamle consumere leser nye meldinger uten feil |
| **Optional felt med default kan fjernes** | Forward OK (gamle lesere bruker default); backward OK (felt i gammel data ignoreres) |
| Required felt (uten default) kan ikke fjernes | Gamle lesere forventer feltet, ny data har det ikke — ingen default å falle tilbake på |
| Felttype og feltnavn kan ikke endres | Behandles som remove + add; ikke kompatibelt |
| Namespace-bump (`api.v2.Periode`) er uaktuelt | Det er en ny Avro-type, ikke kompatibel med eksisterende versjoner |

«Utgått» optional felt kan altså faktisk fjernes fra schema — men vurder om det er verdt
bryet kontra å la det ligge som `@doc "Deprecated"`. Fjerning krever at alle versjoner som
noensinne er registrert håndterer fraværet (FULL_TRANSITIVE sjekker mot alle versjoner).

---

## Utvidelsesmønster: tematiske container-records

Flat root-vekst (slik `OpplysningerOmArbeidssoeker` gjør det) er greit for få stabile felt,
men Periode er ment å leve lenge. Valgt strategi: ett container-record per tema, aldri flate
felt på root.

```
Nytt felt?
├─ Gjelder avslutningen?  → legg til i AvslutningsInfo
├─ Gjelder oppstarten?    → opprett OppstartsInfo? (første gang det trengs)
├─ Gjelder oppholdstid?   → opprett OppholdsInfo?  (første gang det trengs — spekulativt)
└─ Passer ingen kategori? → diskuter om det er en kjernestruktur (flat på root) eller nytt tema
```

Deprecated felt fjernes aldri fra schema. Legg til `@doc "Deprecated siden YY.MM"` og sett
alltid til null i produsenten.

---

## Avro-endringer (repo: `paw-arbeidssokerregisteret-api`)

### `vo/avsluttet_aarsak-v1.avdl` (ny)

```avdl
@namespace("no.nav.paw.arbeidssokerregisteret.api.v1")
protocol AvsluttetAarsakProtocol {
  enum AvsluttetAarsakType {
    SVARTE_NEI_I_BEKREFTELSE,
    BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST,
    UDEFINERT,
    UKJENT_VERDI       // alltid sist, alltid default
  } = UKJENT_VERDI;

  record AvsluttetAarsak {
    AvsluttetAarsakType type;
  }
}
```

> Nye enum-verdier legges til **før** `UKJENT_VERDI`. `UKJENT_VERDI` forblir default.

### `vo/avslutnings_info-v1.avdl` (ny)

```avdl
@namespace("no.nav.paw.arbeidssokerregisteret.api.v1")
protocol AvslutningsInfoProtocol {
  import idl "avsluttet_aarsak-v1.avdl";

  record AvslutningsInfo {
    AvsluttetAarsak? aarsak = null;
    // fremtidige avslutnings-felt legges til her
  }
}
```

### `periode-v1.avdl` (oppdatert)

```avdl
@namespace("no.nav.paw.arbeidssokerregisteret.api.v1")
protocol Periode {
  import idl "vo/metadata-v1.avdl";
  import idl "vo/avsluttet_aarsak-v1.avdl";
  import idl "vo/avslutnings_info-v1.avdl";

  record Periode {
    @logicalType("uuid")
    string id;
    string identitetsnummer;
    Metadata startet;
    Metadata? avsluttet = null;
    AvslutningsInfo? avslutningsInfo = null;   // NY
  }
}
```

`avsluttet` (eksisterende Metadata) og `avslutningsInfo` (ny container) er bevisst separate:
`avsluttet` er kjernestruktur som ikke kan flyttes; `avslutningsInfo` er utvidelsescontaineren.

---

## Endringer i dette monorepoet

### 1. `gradle/libs.versions.toml`
```toml
nav-paw-schema-main = "<ny versjon>"
```

### 2. `apps/hendelseprosessor/.../tilstand/Periode.kt`
```kotlin
data class Periode(
    val id: UUID,
    val identitetsnummer: String,
    val startet: Metadata,
    val startetVedOffset: Long = -1L,
    val avsluttet: Metadata?,
    val avsluttetVedOffset: Long? = null,
    val avsluttetAarsak: AvsluttetAarsak? = null,   // NY — intern type
)
```

### 3. `apps/hendelseprosessor/.../funksjoner/AvsluttPeriode.kt`
```kotlin
val stoppetPeriode = tilstand.gjeldenePeriode.copy(
    avsluttet = hendelse.metadata,
    avsluttetVedOffset = scope.offset,
    avsluttetAarsak = hendelse.avsluttetAarsak,   // NY
)
// ...
nyPeriodeTilstand = ApiPeriode(
    stoppetPeriode.id,
    stoppetPeriode.identitetsnummer,
    stoppetPeriode.startet.api(),
    stoppetPeriode.avsluttet?.api(),
    stoppetPeriode.avsluttetAarsak?.let { ApiAvslutningsInfo(it.apiAarsak()) },  // NY
)
```

### 4. `apps/hendelseprosessor/.../tilstand/KonverterTilApiHendelse.kt`
```kotlin
fun AvsluttetAarsak.apiAarsak(): ApiAvsluttetAarsak =
    ApiAvsluttetAarsak(type.apiAarsakType())

fun AvsluttetAarsakType.apiAarsakType(): ApiAvsluttetAarsakType =
    when (this) {
        AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE            -> ApiAvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE
        AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST -> ApiAvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
        AvsluttetAarsakType.UDEFINERT                           -> ApiAvsluttetAarsakType.UDEFINERT
        AvsluttetAarsakType.UKJENT_VERDI                        -> ApiAvsluttetAarsakType.UKJENT_VERDI
    }
```

`when` på enum er exhaustive — kompilatoren feiler hvis nye verdier mangler mapping.

---

## Fremtidige containere

| Container | Status | Trigger |
|-----------|--------|---------|
| `AvslutningsInfo?` | ✅ Lages nå | — |
| `OppstartsInfo?` | Sannsynlig | Første gang et oppstarts-relatert felt skal eksponeres eksternt |
| `OppholdsInfo?` | Spekulativ | Første gang et felt ikke passer i de andre containerne |

---

## Tester

| Test | Fil | Verifiserer |
|------|-----|-------------|
| Topology (ny) | `ApplikasjonsTest.kt` | `Avsluttet` med årsak → `Periode.avslutningsInfo.aarsak` korrekt |
| Topology (eksisterende) | `ApplikasjonsTest.kt` | `Avsluttet` uten årsak → `avslutningsInfo = null` |
| Schema-kompatibilitet | CI | Ny versjon godkjent under FULL_TRANSITIVE |

---

## Sjekkliste

- [ ] `avsluttet_aarsak-v1.avdl` og `avslutnings_info-v1.avdl` laget i `paw-arbeidssokerregisteret-api`
- [ ] `periode-v1.avdl` oppdatert med `avslutningsInfo? = null`
- [ ] Ny schema-versjon publisert og validert
- [ ] `nav-paw-schema-main` oppdatert i `libs.versions.toml`
- [ ] `Periode.kt` oppdatert med `avsluttetAarsak`
- [ ] `AvsluttPeriode.kt` og `KonverterTilApiHendelse.kt` oppdatert
- [ ] Tester grønne: `./gradlew :apps:hendelseprosessor:test`
