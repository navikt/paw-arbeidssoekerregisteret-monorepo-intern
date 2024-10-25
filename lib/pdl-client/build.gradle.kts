import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.paw"

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.expediagroup.graphql")
    id("org.jmailen.kotlinter")
    id("maven-publish")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
    test {
        useJUnitPlatform()
    }
    lintKotlinMain {
        exclude("no/nav/paw/pdl/graphql/generated/**/*.kt")
    }
    formatKotlinMain {
        exclude("no/nav/paw/pdl/graphql/generated/**/*.kt")
    }
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    mavenNav("*")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        mavenNav("paw-kotlin-clients")
    }
}

graphql {
    client {
        packageName = "no.nav.paw.pdl.graphql.generated"
        schemaFile = File("src/main/resources/pdl-schema.graphql")
        queryFiles = file("src/main/resources").listFiles()?.toList()?.filter { it.name.endsWith(".graphql") }.orEmpty()
        serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.KOTLINX
    }
}

dependencies {
    val coroutinesVersion: String by project
    val kotlinSerializationVersion: String by project
    val ktorVersion: String by project
    val mockkVersion: String by project
    val graphQLKotlinVersion: String by project

    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")

    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    api("com.expediagroup:graphql-kotlin-ktor-client:$graphQLKotlinVersion")

    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation(kotlin("test"))
}

fun RepositoryHandler.mavenNav(repo: String): MavenArtifactRepository {
    val githubPassword: String by project

    return maven {
        setUrl("https://maven.pkg.github.com/navikt/$repo")
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
    }
}
