plugins {
    id("com.google.cloud.tools.jib")
}

val image: String? by project

jib {
    from.image = project.property("distrolessJavaImage").toString()
    to.image = "${image ?: project.name}:${project.version}"
    container {
        jvmFlags = listOf("-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational")
        environment = mapOf(
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}",

        )
    }
}
