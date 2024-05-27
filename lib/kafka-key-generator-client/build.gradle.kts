plugins {
    kotlin("jvm")
}
val jvmMajorVersion: String by project
val jvmVersion = JavaVersion.valueOf("VERSION_$jvmMajorVersion")
dependencies {
    implementation(jackson.datatypeJsr310)
    implementation(jackson.kotlin)
    implementation(ktorClient.contentNegotiation)
    implementation(ktorClient.core)
    implementation(ktorClient.cio)
    implementation(ktor.serializationJackson)
    implementation(navSecurity.tokenClient)
    api(navCommon.tokenClient)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}
