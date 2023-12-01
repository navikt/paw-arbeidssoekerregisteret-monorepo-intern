FROM ghcr.io/navikt/baseimages/temurin:21

ENV SERVICE_NAVN=paw-arbeidssokerregisteret-event-prosessor
ENV AGENT=agents/opentelemetry-javaagent.jar
ENV ANONYMISERING=agents/opentelemetry-anonymisering-1.31.0-23.10.25.8-1.jar
ENV APPLIKASJON_JAR=App.jar

COPY app/build/libs/fat.jar app.jar
COPY $AGENT opentelemetry-javaagent.jar
COPY $ANONYMISERING opentelemetry-agent-extension.jar
ENV JAVA_OPTS -javaagent:opentelemetry-javaagent.jar -Dotel.javaagent.extensions=opentelemetry-agent-extension.jar -Dotel.resource.attributes=service.name=$SERVICE_NAVN
CMD ["java", "$JAVA_OPTS", "-jar", "$APPLIKASJON_JAR"]
