plugins {
    id("com.google.cloud.tools.jib")
}

val chainguardJavaImage : String by project
val jvmMajorVersion: String by project
val image: String? by project
val targetImage: String = "${image ?: project.name}:${project.version}"

jib {
    from.image = "$chainguardJavaImage$jvmMajorVersion"
    to.image = targetImage
    container {
        jvmFlags = listOf("-XX:ActiveProcessorCount=8", "-XX:+UseZGC", "-XX:+ZGenerational")
        environment = mapOf(
            "IMAGE_WITH_VERSION" to targetImage
        )
    }
}
