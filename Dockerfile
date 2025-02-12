FROM gradle:8.12.1-jdk21-alpine AS builder
USER gradle
COPY --chown=gradle:gradle build.gradle  ./
#RUN gradle --no-daemon build
RUN gradle dependencies


FROM gradle:8.12.1-jdk21-alpine
USER gradle
WORKDIR /tests
COPY --from=builder /home/gradle/.gradle /home/gradle/.gradle