FROM ghcr.io/navikt/baseimages/temurin:21

COPY app/build/libs/fat.jar app.jar
