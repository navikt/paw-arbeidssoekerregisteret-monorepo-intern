# Schema for arbeidssokerregisteret
Arbeidssøker registeret består av 3 topic som er definert i et og same Avro (avdl) schema.

Avro schema er i en tidlig BETA fase og kan endres uten forvarsel. Det vil også bli flyttet til et eget repo senere i prosessen.
Schema finner du [her](https://github.com/navikt/paw-arbeidssokerregisteret-event-prosessor/packages/1978508).

## Generer kode fra schema
Dette kan enkelt gjøres via gradle:
```kotlin
plugins {
    kotlin("jvm") version "1.9.10"
    id("com.commercehub.gradle.plugin.avro") version "1.9.1"
}

repositories {
    mavenCentral()
    val githubPassword: String by project
    maven {
        setUrl("https://maven.pkg.github.com/navikt/paw-arbeidssokerregisteret-event-prosessor")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
}

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    schema("no.nav.paw.arbeidssokerregisteret.api.schema:eksternt-api:23.11.02.28-1")
    implementation("org.apache.avro:avro:1.11.1")
}

tasks.withType(GenerateAvroProtocolTask::class) {
    source(zipTree(schema.singleFile))
}
```


# paw-arbeidssokerregisteret-event-prosessor
Prosesserer interne hendelses i arbeidssøkerregister og produserer API hendelser
