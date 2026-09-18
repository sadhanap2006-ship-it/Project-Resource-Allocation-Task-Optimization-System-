# =====================================================================
#  Dockerfile - builds one image containing the backend and the website.
#
#  Most hosting services (Render, Railway, Fly.io, Koyeb ...) look for this
#  file, build it themselves and run the result. You do not need Docker on
#  your own machine to use it.
#
#  It is a "multi-stage" build:
#    stage 1 uses a Maven image to compile the project into a .jar
#    stage 2 copies only that .jar into a small Java runtime image
#  The result is a much smaller image, because Maven and the source code
#  are left behind in stage 1.
# =====================================================================

# ---------- stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copy the pom first and download the dependencies on their own. Docker
# caches this layer, so editing your Java code later does not re-download
# the whole internet.
COPY backend/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline

# Now copy the source and build. Tests already ran in development; skipping
# them here keeps deployments fast.
COPY backend/src ./src
RUN mvn -B clean package -DskipTests


# ---------- stage 2: run ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# The website. WebConfig serves "file:./frontend/", and because the working
# directory is /app that resolves to /app/frontend.
COPY frontend ./frontend

# The application itself.
COPY --from=build /build/target/resource-allocation-system-*.jar ./app.jar

# Documentation only - the hosting service sets the real port through the
# PORT environment variable, which application.properties reads.
EXPOSE 8080

# Do not run as root.
RUN useradd --create-home --shell /bin/bash appuser && chown -R appuser:appuser /app
USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
