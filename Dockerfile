#ARG GRADLE_VERSION
FROM registry-gitlab.corp.mail.ru/rustore-admins/registry/public/gradle:8.12.1-jdk21-alpine
USER root
WORKDIR /tests
RUN mkdir -p /home/gradle/.java/.userPrefs && \
    chown -R gradle:gradle /home/gradle/.java &&\
    chown -R gradle:gradle /tests &&\
    chmod -R 755 /tests
COPY --chown=gradle:gradle build.gradle  ./
USER gradle
RUN  gradle cacheAllDependencies --no-daemon