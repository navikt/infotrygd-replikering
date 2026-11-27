package no.nav.historisk.innsyn

import no.nav.security.mock.oauth2.MockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.OracleContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.testcontainers.images.builder.Transferable
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID

class ContainerImageIT {
    @Test
    fun `packaged image starts with mounted Oracle credentials and exposes live metrics`() {
        val image = System.getProperty("app.image")
            ?.takeIf { it.isNotBlank() && !it.startsWith("\${") }
            ?: throw IllegalStateException("Required system property app.image is missing")

        val password = UUID.randomUUID().toString().replace("-", "")
        val oidc = MockOAuth2Server()
        val network = Network.newNetwork()
        val oracle = object : OracleContainer(
            DockerImageName.parse(ORACLE_IMAGE).asCompatibleSubstituteFor("gvenzl/oracle-xe")
        ) {
            // Oracle Free already has this PDB; avoid creating another database copy.
            override fun getDatabaseName(): String = DATABASE
        }
            .withUsername(DB_USERNAME)
            .withPassword(password)
            .withNetwork(network)
            .withNetworkAliases(ORACLE_ALIAS)
        val app = GenericContainer<Nothing>(DockerImageName.parse(image)).apply {
            withNetwork(network)
            withExposedPorts(APP_PORT)
            waitingFor(
                Wait.forHttp("/actuator/health/readiness")
                    .forStatusCode(200)
                    .withStartupTimeout(Duration.ofMinutes(3))
            )
            withEnv("APP_GRUPPE_ADMIN", "container-smoke-admin")
            withEnv("APP_DEFAULT_SCHEMA", DB_USERNAME)
            withEnv("APP_AUDIT_HOST", "localhost")
            withEnv("APP_AUDIT_PORT", "16515")
            withEnv("APP_DDL_AUTO", "validate")
            withEnv("AZURE_APP_CLIENT_ID", "container-smoke-unused")
            withEnv("NO_PROXY", "host.testcontainers.internal")
            withEnv("VAULT_PASSWORD", PASSWORD_PATH)
            withEnv("VAULT_USERNAME", USERNAME_PATH)
            withEnv("VAULT_CONFIG", CONFIG_DIRECTORY)
            withCopyToContainer(readOnly(password), PASSWORD_PATH)
            withCopyToContainer(readOnly(DB_USERNAME), USERNAME_PATH)
            withCopyToContainer(
                readOnly("jdbc:oracle:thin:@//$ORACLE_ALIAS:1521/$DATABASE"),
                "$CONFIG_DIRECTORY/jdbc_url"
            )
        }

        try {
            oidc.start(InetAddress.getLoopbackAddress(), 0)
            val discoveryUrl = oidc.wellKnownUrl("azure")
            Testcontainers.exposeHostPorts(discoveryUrl.port)
            app.withEnv(
                "AZURE_APP_WELL_KNOWN_URL",
                discoveryUrl.newBuilder().host("host.testcontainers.internal").build().toString()
            )
            oracle.start()
            bootstrapOracle(oracle, password)
            app.start()

            val config = app.containerInfo.config
            assertThat(config.user).isEqualTo("65532")
            assertThat(config.entrypoint?.toList() ?: emptyList()).containsExactly("/usr/bin/java")
            assertThat(config.cmd?.toList() ?: emptyList()).containsExactly("-Xms270M", "-jar", "/app/app.jar")

            val javaVersion = app.execInContainer("/usr/bin/java", "-version")
            assertThat(javaVersion.exitCode).isZero()
            assertThat(
                Regex("(?i)openjdk(?: version)?\\s+\"?25(?:[.\\s\"]|$)")
                    .containsMatchIn(javaVersion.stdout + javaVersion.stderr)
            ).isTrue()

            val baseUrl = "http://${app.host}:${app.getMappedPort(APP_PORT)}"
            val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
            await("readiness endpoint", Duration.ofMinutes(3)) {
                val response = request(http, "$baseUrl/actuator/health/readiness")
                if (response.statusCode() == 200 && READINESS_UP.containsMatchIn(response.body())) {
                    null
                } else {
                    "HTTP ${response.statusCode()}, status UP not observed"
                }
            }

            var initialDelay: Double? = null
            await("initial Oracle metrics", Duration.ofSeconds(75)) {
                val response = request(http, "$baseUrl/actuator/prometheus")
                if (response.statusCode() != 200) {
                    "Prometheus endpoint returned HTTP ${response.statusCode()}"
                } else {
                    val success = metricValue(response.body(), SUCCESS_METRIC)
                    val delay = metricValue(response.body(), DELAY_METRIC, TABLE_REF)
                    if (success == 1.0 && delay != null && delay.isFinite() && delay > 0.0) {
                        initialDelay = delay
                        null
                    } else {
                        "success=$success, table delay=$delay"
                    }
                }
            }
            val previousDelay = initialDelay
                ?: throw AssertionError("Initial table delay metric was not captured")

            updateSmokeTimestamp(oracle, password, Instant.now().minusSeconds(60))
            var updatedDelay: Double? = null
            await("scheduled Oracle reread", Duration.ofSeconds(75)) {
                val response = request(http, "$baseUrl/actuator/prometheus")
                if (response.statusCode() != 200) {
                    "Prometheus endpoint returned HTTP ${response.statusCode()}"
                } else {
                    val success = metricValue(response.body(), SUCCESS_METRIC)
                    val delay = metricValue(response.body(), DELAY_METRIC, TABLE_REF)
                    if (success == 1.0 && delay != null && delay.isFinite() && delay > 0.0 && delay < previousDelay) {
                        updatedDelay = delay
                        null
                    } else {
                        "success=$success, previous delay=$previousDelay, current delay=$delay"
                    }
                }
            }
            assertThat(updatedDelay).isNotNull().isLessThan(previousDelay)
        } finally {
            try {
                app.stop()
            } finally {
                try {
                    oracle.stop()
                } finally {
                    try {
                        network.close()
                    } finally {
                        oidc.shutdown()
                    }
                }
            }
        }
    }

    private fun bootstrapOracle(oracle: OracleContainer, password: String) {
        DriverManager.getConnection(oracle.jdbcUrl, oracle.username, password).use { connection ->
            ResourceDatabasePopulator(ClassPathResource("testschema.sql")).populate(connection)
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE $SMOKE_TABLE (OPPDATERT TIMESTAMP NOT NULL)")
                statement.execute("CREATE INDEX IDX_CONTAINER_SMOKE_OPPDATERT ON $SMOKE_TABLE (OPPDATERT)")
            }
            connection.prepareStatement(
                "INSERT INTO replikering_status (SCHEMA_NAME, TABLE_NAME, READY) VALUES (?, ?, ?)"
            ).use { statement ->
                statement.setString(1, SMOKE_SCHEMA)
                statement.setString(2, SMOKE_TABLE)
                statement.setInt(3, 1)
                statement.executeUpdate()
            }
            insertSmokeTimestamp(connection, Instant.now().minus(Duration.ofMinutes(30)))
        }
    }

    private fun updateSmokeTimestamp(oracle: OracleContainer, password: String, timestamp: Instant) {
        DriverManager.getConnection(oracle.jdbcUrl, oracle.username, password).use { connection ->
            insertSmokeTimestamp(connection, timestamp)
        }
    }

    private fun insertSmokeTimestamp(connection: java.sql.Connection, timestamp: Instant) {
        connection.prepareStatement("INSERT INTO $SMOKE_TABLE (OPPDATERT) VALUES (?)").use { statement ->
            statement.setTimestamp(1, Timestamp.from(timestamp))
            statement.executeUpdate()
        }
    }

    private fun readOnly(content: String): Transferable =
        Transferable.of(content.toByteArray(Charsets.UTF_8), READ_ONLY_MODE)

    private fun request(http: HttpClient, url: String): HttpResponse<String> =
        http.send(
            HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )

    private fun metricValue(scrape: String, name: String, table: String? = null): Double? {
        val prefix = if (table == null) "$name " else "$name{tabell=\"$table\"} "
        return scrape.lineSequence()
            .firstOrNull { it.startsWith(prefix) }
            ?.substringAfterLast(' ')
            ?.toDoubleOrNull()
    }

    private fun await(description: String, timeout: Duration, condition: () -> String?) {
        val deadline = System.nanoTime() + timeout.toNanos()
        var lastFailure = "condition not observed"
        while (System.nanoTime() < deadline) {
            val failure = try {
                condition()
            } catch (exception: Exception) {
                exception.javaClass.simpleName
            }
            if (failure == null) return
            lastFailure = failure
            Thread.sleep(1_000)
        }
        throw AssertionError("$description timed out: $lastFailure")
    }

    private companion object {
        const val ORACLE_IMAGE = "gvenzl/oracle-free:23-slim-faststart"
        const val DATABASE = "FREEPDB1"
        const val DB_USERNAME = "test"
        const val ORACLE_ALIAS = "oracle"
        const val APP_PORT = 8080
        const val SMOKE_SCHEMA = "TEST"
        const val SMOKE_TABLE = "SMOKE_TABLE"
        const val TABLE_REF = "$SMOKE_SCHEMA.$SMOKE_TABLE"
        const val PASSWORD_PATH = "/var/run/secrets/oracle/creds/password"
        const val USERNAME_PATH = "/var/run/secrets/oracle/creds/username"
        const val CONFIG_DIRECTORY = "/var/run/secrets/oracle/config"
        const val READ_ONLY_MODE = 0b100_100_100
        const val SUCCESS_METRIC = "infotrygd_replikering_oppdatering_vellykket"
        const val DELAY_METRIC = "infotrygd_replikering_tabellforsinkelse"
        val READINESS_UP = Regex("\"status\"\\s*:\\s*\"UP\"")
    }
}
