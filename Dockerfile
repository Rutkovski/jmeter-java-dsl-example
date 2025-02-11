#FROM registry-gitlab.corp.mail.ru/rustore-admins/registry/public/gradle:8.12.1-jdk21-alpine AS builder
##USER gradle
#COPY --chown=gradle:gradle build.gradle  ./
##RUN gradle build
#RUN #gradle build --no-daemon -x compileJava -x compileTestJava
#RUN gradle dependencies
#
#
#FROM registry-gitlab.corp.mail.ru/rustore-admins/registry/public/gradle:8.12.1-jdk21-alpine
#USER gradle
#WORKDIR /tests
#COPY --from=builder /home/gradle/.gradle /home/gradle/.gradle
##COPY --from=builder .gradle /tests

#RUN mkdir -p /home/gradle/.java/.userPrefs && \
#    chown -R gradle:gradle /home/gradle/.java &&\
#    chown -R gradle:gradle /tests && \
#    chmod -R 755 /tests



#FROM registry-gitlab.corp.mail.ru/rustore-admins/registry/public/gradle:8.12.1-jdk21-alpine AS builder
#WORKDIR tests
#COPY build.gradle  ./
#COPY settings.gradle  ./
#RUN gradle build --no-daemon -x test
#
#FROM registry-gitlab.corp.mail.ru/rustore-admins/registry/public/gradle:8.12.1-jdk21-alpine
##RUN gradle dependencies
#WORKDIR tests
#COPY --from=builder /home/gradle/.gradle /home/gradle/
#COPY --fr§om=builder /tests /tests
#COPY --from=builder .gradle /home/gradle
#COPY --from=builder build.gradle  /home/gradle/build.gradle
#
#COPY --from=builder .gradle /tests

#FROM registry-gitlab.corp.mail.ru/rustore-admins/registry/public/gradle:8.12.1-jdk21-alpine


FROM gradle:8.12.1-jdk21-alpine AS cache
WORKDIR /app
ENV GRADLE_USER_HOME /cache
COPY build.gradle  ./
RUN gradle --no-daemon build --stacktrace

FROM gradle:8.12.1-jdk21-alpine AS builder
WORKDIR /app
COPY --from=cache /cache /home/gradle/.gradle
COPY . .
USER gradle
RUN #gradle --no-daemon build

#FROM gradle:8.12.1-jdk21-alpine
#WORKDIR /app
#RUN apk --no-cache add curl
#COPY --from=builder /app/build/libs/*.jar crystal-skull-all.jar
#COPY --from=builder /app/build/resources/main/ src/main/resources/
#ENV PORT 80
#EXPOSE 80
#HEALTHCHECK --timeout=5s --start-period=5s --retries=1 \
#    CMD curl -f http://localhost:$PORT/health_check