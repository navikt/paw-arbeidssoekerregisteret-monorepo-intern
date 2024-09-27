plugins {
    kotlin("jvm")
}
val jvmMajorVersion: String by project
val jvmVersion = JavaVersion.valueOf("VERSION_$jvmMajorVersion")

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.jacksonKotlin)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorSerializationJackson)
    implementation(libs.tokenClientCore)
    api(libs.tokenClient)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}
