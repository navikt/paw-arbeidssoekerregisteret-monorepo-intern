plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":lib:hoplite-config"))
    api(project(":lib:hoplite-config"))
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
