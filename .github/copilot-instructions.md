# Copilot Instructions – paw-arbeidssoekerregisteret-monorepo-intern

Kotlin/Gradle monorepo for NAV team PAW. Services that write to or read from the arbeidssøker hendelseslogg (event log). External consumers belong in the separate `-ekstern` monorepo.

## Build, test, lint

Requires a `githubPassword` in `~/.gradle/gradle.properties` (or `gradle.properties`) for GitHub Package Registry access.

```bash
# Build everything
./gradlew build

# Build a single module
./gradlew :apps:bekreftelse-tjeneste:build

# Run all tests
./gradlew test

# Run tests for a single module
./gradlew :apps:bekreftelse-tjeneste:test

# Run a single test class (Kotest FreeSpec)
./gradlew :apps:bekreftelse-tjeneste:test --tests "no.nav.paw.bekreftelsetjeneste.SomeSpecName"

# Build + push container image (used by CI)
./gradlew :apps:bekreftelse-tjeneste:build :apps:bekreftelse-tjeneste:jib \
  -Pversion=<version> -Pimage=<image>
```

Tests use JUnit5 platform with Kotest assertions. All test tasks are configured with `useJUnitPlatform()`.

## Module layout

```
domain/     – Avro schemas and Kotlin domain models (no business logic)
lib/        – Shared infrastructure libraries (kafka, security, database, metrics, …)
apps/       – Deployable Ktor/Netty services
jobs/       – Scheduled Naisjobs (e.g. batch sync)
test/       – Shared test helpers (test-data-lib, kafka-streams-test-functions)
buildSrc/   – Custom Gradle convention plugin: jib-chainguard.gradle.kts
```

Every `app` and `job` applies `jib-chainguard` for container builds, targeting Chainguard JRE images on `europe-north1-docker.pkg.dev`.

## Architecture

### ApplicationContext pattern
Each app wires all dependencies in a `data class ApplicationContext` with a `companion object { fun create(): ApplicationContext }`. `main()` calls `create()`, then passes the context to `Application.module(applicationContext)`. Do not use dependency injection frameworks; keep all wiring explicit in `ApplicationContext`.

### Config loading
Config is loaded with `loadNaisOrLocalConfiguration<T>(filename)` from `lib:hoplite-config`. It reads from `/nais/<filename>` when `NAIS_CLUSTER_NAME` is `prod-gcp` or `dev-gcp`, and from `/local/<filename>` otherwise. Config files are TOML (sometimes YAML) placed in `src/main/resources/local/` and `src/main/resources/nais/`.

Config for standard libs (Kafka, security, database) lives in the lib modules and is referenced by constant (e.g. `KAFKA_CONFIG`, `SECURITY_CONFIG`, `DATABASE_CONFIG`).

### Ktor plugin conventions
Ktor features are installed via `installXxxPlugin()` extension functions from the `lib/` modules:
- `installLoggingPlugin()` – structured JSON logging
- `installContentNegotiationPlugin()` – Jackson (with `JavaTimeModule`)
- `installWebAppMetricsPlugin(registry)` – Prometheus + `/internal/metrics`
- `installAuthenticationPlugin(authProviders)` – TokenX / AzureAD / IdPorten / MaskinPorten
- `installErrorHandlingPlugin()` – standardised error responses
- `installDatabasePlugin(dataSource)` – Exposed/HikariCP lifecycle
- `installKafkaStreamsPlugins(kafkaStreamsList)` – Kafka Streams lifecycle + health

Internal routes (health, metrics) are at `/internal/*`.

### Kafka and event model
- Schemas are Avro (generated from `domain/` with `davidmc24` Avro plugin).
- Kafka messages use `Long` keys (arbeidssøkerId from `kafka-key-generator`).
- Kafka Streams apps build topology in `buildTopology()` and wire state stores explicitly before creating the `KafkaStreams` instance.
- `KAFKA_STREAMS_CONFIG_WITH_SCHEME_REG` / `KAFKA_CONFIG_WITH_SCHEME_REG` for Schema Registry; `KAFKA_CONFIG` for plain Kafka.

### Authentication model
- **Sluttbrukere** (end users): TokenX tokens (claims: `pid`, `acr`)
- **Saksbehandlere**: Azure AD tokens (claims: `NAVident`, `oid`)
- **M2M**: Azure AD M2M client credentials
- Security provider config comes from `SecurityConfig` loaded from `security_config.toml`.

### Database
- ORM: Jetbrains Exposed (`exposed-jdbc`, `exposed-java-time`, `exposed-json`)
- Migrations: Flyway (`V<n>__<description>.sql` in `src/main/resources/db/migration/`)
- Connection pool: HikariCP via `createHikariDataSource(databaseConfig)`

## Key conventions

- **RuntimeEnvironment** is a sealed interface (`Local`, `DevGcp`, `ProdGcp`) available as `currentRuntimeEnvironment`. Use `appNameOrDefaultForLocal()` / `namespaceOrDefaultForLocal()` etc. for environment-aware defaults.
- **Logging**: use `buildApplicationLogger` (from `lib:logging`) for the application-level logger; `nav.common.log` for audit/team logs.
- **Kotest style**: tests use `FreeSpec` or `StringSpec`. Use `assertEvent<T>()` / `assertNoMessage()` helpers from `test:kafka-streams-test-functions` for Kafka Streams topology tests.
- **Arrow**: used for functional types (`Either`, `Option`, etc.) and `arrow-integrations-jackson-module` for serialization. Avoid Arrow's `NonEmptyList` with Jackson – use the repo's own `PawNonEmptyList` instead.
- **OpenAPI code generation**: some apps generate Kotlin models from OpenAPI specs at compile time (task `generate<pkg>`, wired as a dependency of `KotlinCompilationTask`). Generated sources land in `build/generated/src/main/kotlin/`.
- **JVM version**: 25 (configured via toolchain in each module, `jvmMajorVersion=25` in `gradle.properties`).
- **Versioning**: `YY.MM.DD.<run_number>-<run_attempt>` set during CI via `-Pversion=…`.
- **Nais manifests**: `nais/nais-dev.yaml` and `nais/nais-prod.yaml` per module. Topic names differ between dev and prod – check the manifests before hardcoding.
- Each app has its own GitHub Actions workflow in `.github/workflows/<app>.yaml`, triggered on changes to `apps/<app>/**`, `lib/**`, `domain/**`, `gradle/**`, and common Gradle files.
- Version catalog is at `gradle/libs.versions.toml`. Always use catalog aliases (e.g. `libs.bundles.ktor.server.instrumented`) rather than raw coordinates.
