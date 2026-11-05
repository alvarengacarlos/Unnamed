FROM amazoncorretto:25-alpine AS build
WORKDIR /core-api
COPY .mvn .mvn
COPY mvnw pom.xml checkstyle.xml jooq-codegen.xml ./
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=jdbc_url,env=JDBC_URL,required=true \
    --mount=type=secret,id=db_username,env=DB_USERNAME,required=true \
    --mount=type=secret,id=db_password,env=DB_PASSWORD,required=true \
    ./mvnw -B package -DskipTests

FROM amazoncorretto:25-al2023-headless
ARG JDBC_URL
ARG DB_USERNAME
ARG DB_PASSWORD
ARG CORS_ALLOWED_ORIGINS
ARG JWT_JWK_SET_URI
ARG JWT_AUDIENCE
ARG JWT_ISSUER
ARG JWT_ORGANIZATION_CLAIM=organization
ARG AI_API_KEY
ARG OTEL_EXPORTER_OTLP_ENDPOINT
ARG OTEL_EXPORTER_OTLP_HEADERS
ENV JDBC_URL=${JDBC_URL} \
    DB_USERNAME=${DB_USERNAME} \
    DB_PASSWORD=${DB_PASSWORD} \
    CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS} \
    JWT_JWK_SET_URI=${JWT_JWK_SET_URI} \
    JWT_AUDIENCE=${JWT_AUDIENCE} \
    JWT_ISSUER=${JWT_ISSUER} \
    JWT_ORGANIZATION_CLAIM=${JWT_ORGANIZATION_CLAIM} \
    AI_API_KEY=${AI_API_KEY} \
    OTEL_EXPORTER_OTLP_ENDPOINT=${OTEL_EXPORTER_OTLP_ENDPOINT} \
    OTEL_EXPORTER_OTLP_HEADERS=${OTEL_EXPORTER_OTLP_HEADERS}
# The time zone of the whole application, Brasília time: the JVM reads it from TZ. It decides, for example, when the
# month turns for the tuition payments.
ENV TZ=America/Sao_Paulo
USER nobody
WORKDIR /app
COPY --from=build /core-api/target/coreapi-*-exec.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/localhost/8080 \
        && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 \
        && grep -q "\"status\":\"UP\"" <&3'
ENTRYPOINT ["java", "-jar", "app.jar"]
