# syntax=docker/dockerfile:1

# --- Build stage -------------------------------------------------------------
# The dev host only has Java 8 and no local Gradle install, so the build always happens
# here, inside a JDK 21 image, never on the bare host.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache dependencies: copy only the wrapper + build files first.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# Now copy the rest of the source and build.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# --- Runtime stage -------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN useradd --system aislego
COPY --from=build /app/build/libs/*.jar /app/app.jar
RUN chown -R aislego:aislego /app
USER aislego

ENV SPRING_PROFILES_ACTIVE=local
EXPOSE 8080

# Tuned for tight-memory hosts (e.g. Render's free 512MB tier): caps heap/metaspace/thread
# stacks explicitly rather than trusting container-memory auto-detection, and reduces JIT
# compiler threads since a shared/throttled CPU gains little from extra compiler parallelism
# but pays for it in contention. Without this the JVM's default sizing assumes far more
# headroom than a free-tier container actually has, leading to OOM kills under any real load.
ENTRYPOINT ["java", "-Xmx350m", "-Xss256k", "-XX:MaxMetaspaceSize=96m", "-XX:CICompilerCount=2", "-jar", "/app/app.jar"]
