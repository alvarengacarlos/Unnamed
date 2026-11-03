FROM amazoncorretto:25-alpine AS build
WORKDIR /core-api
COPY .mvn .mvn
COPY mvnw pom.xml checkstyle.xml ./
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=jdbc_url,env=JDBC_URL,required=true \
    --mount=type=secret,id=db_username,env=DB_USERNAME,required=true \
    --mount=type=secret,id=db_password,env=DB_PASSWORD,required=true \
    ./mvnw -B package -DskipTests \
        -Dcodegen.jdbc.url="$JDBC_URL" \
        -Dcodegen.jdbc.user="$DB_USERNAME" \
        -Dcodegen.jdbc.password="$DB_PASSWORD"

FROM amazoncorretto:25-al2023-headless
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
