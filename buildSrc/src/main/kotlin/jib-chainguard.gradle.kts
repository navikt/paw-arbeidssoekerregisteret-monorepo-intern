plugins {
    id("com.google.cloud.tools.jib")
}

val chainguardJavaImage : String by project
val jvmMajorVersion: String by project
val image: String? by project
val targetImage: String = "${image ?: project.name}:${project.version}"
val baseImage = "$chainguardJavaImage$jvmMajorVersion"

// Workaround: Jib cannot parse OCI Image Index v1.1 manifests with the `artifactType`
// field (unresolved upstream bug). Pre-pull via Docker, which handles OCI v1.1 natively,
// then point Jib at the local daemon image to bypass the registry manifest parsing.
val pullBaseImage by tasks.registering(Exec::class) {
    group = "jib"
    description = "Pre-pull base image to local Docker daemon"
    commandLine("docker", "pull", baseImage)
}

jib {
    from.image = "docker://$baseImage"
    to.image = targetImage
    container {
        jvmFlags = listOf("-XX:ActiveProcessorCount=8", "-XX:+UseZGC", "-XX:+ZGenerational")
        environment = mapOf(
            "IMAGE_WITH_VERSION" to targetImage
        )
    }
}

tasks.named("jib") { dependsOn(pullBaseImage) }
tasks.named("jibDockerBuild") { dependsOn(pullBaseImage) }
