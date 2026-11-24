FROM eclipse-temurin:25-jre-alpine-3.24@sha256:69c2a26e9894016e274c880fa542956d005b3755f84db765145185e87405bd89

USER 65532:65532

ENV JAVA_OPTS="${JAVA_OPTS} -Xms270M"
ENV TZ="Europe/Oslo"

WORKDIR /app
COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar"]
CMD ["/app/app.jar"]

EXPOSE 8080
