FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25

ENV TZ="Europe/Oslo"

WORKDIR /app
COPY target/*.jar app.jar

CMD ["-Xms270M", "-jar", "/app/app.jar"]

EXPOSE 8080
