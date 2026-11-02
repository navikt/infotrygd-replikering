OTEL_JAVAAGENT_VERSION := 2.33.1
OTEL_JVM_ARGUMENTS := -javaagent:"$(CURDIR)/target/otel/opentelemetry-javaagent.jar"
OTEL_ENV := OTEL_SERVICE_NAME=infotrygd-replikering OTEL_TRACES_EXPORTER=none OTEL_METRICS_EXPORTER=none OTEL_LOGS_EXPORTER=none

.PHONY: otel-agent
otel-agent:
	mvn --batch-mode dependency:copy -Dartifact=io.opentelemetry.javaagent:opentelemetry-javaagent:$(OTEL_JAVAAGENT_VERSION) -DoutputDirectory=target/otel -Dmdep.stripVersion=true

.PHONY: backend-demo
backend-demo: otel-agent
	$(OTEL_ENV) mvn --batch-mode test-compile spring-boot:test-run -Dspring-boot.run.main-class=DevMainKt -Dspring-boot.run.jvmArguments='$(OTEL_JVM_ARGUMENTS)'
