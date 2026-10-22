# Packages the jar built by Maven, which needs Docker for the jOOQ codegen and so cannot run inside docker build:
#   ./mvnw -B package -DskipTests && docker build .
FROM eclipse-temurin:25-jre
# The time zone of the whole application, Brasília time: the JVM reads it from TZ. It decides, for example, when the
# month turns for the tuition payments.
ENV TZ=America/Sao_Paulo
RUN useradd --system --no-create-home app
USER app
WORKDIR /app
COPY app/target/app-*-exec.jar app.jar
EXPOSE 8080
# The image has no curl/wget, so the check talks HTTP through bash's /dev/tcp.
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/localhost/8080 \
        && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 \
        && grep -q "\"status\":\"UP\"" <&3'
ENTRYPOINT ["java", "-jar", "app.jar"]
