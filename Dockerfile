FROM navikt/java:17

ENV JAVA_OPTS="${JAVA_OPTS} -Xms270M"

COPY target/*.jar app.jar

COPY init-scripts/init.sh /init-scripts/init.sh

EXPOSE 8080

