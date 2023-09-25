FROM ghcr.io/navikt/baseimages/temurin:17

ENV SERVICE_NAVN=paw-arbeidssoker-registeret
ENV AGENT=agents/opentelemetry-javaagent.jar
ENV ANONYMISERING=agents/opentelemetry-anonymisering-1.30.0-23.09.22.7-1.jar
ENV APPLIKASJON_JAR=App.jar

COPY app/build/libs/fat.jar app.jar
COPY $AGENT opentelemetry-javaagent.jar
COPY $ANONYMISERING opentelemetry-agent-extension.jar
ENV JAVA_OPTS -javaagent:opentelemetry-javaagent.jar -Dotel.javaagent.extensions=opentelemetry-agent-extension.jar -Dotel.resource.attributes=service.name=$SERVICE_NAVN
CMD ["java", "$JAVA_OPTS", "-jar", "$APPLIKASJON_JAR"]
