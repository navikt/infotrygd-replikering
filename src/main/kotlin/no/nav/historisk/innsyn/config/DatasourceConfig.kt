package no.nav.historisk.innsyn.config

import jakarta.validation.constraints.NotEmpty
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import java.nio.file.Files
import java.nio.file.Paths
import javax.sql.DataSource
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import kotlin.collections.contains

@Configuration
@EnableConfigurationProperties(DatasourceConfiguration::class)
class DatasourceConfig(private val environment: Environment) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun isTestEnvironment(): Boolean {
        val activeProfiles = environment.activeProfiles
        return activeProfiles.contains("test") || activeProfiles.contains("local") || activeProfiles.contains("dev")
    }

    @Bean
    fun vaultDatasourceUsername(
        @Value("\${vault.username:}") filePath: String,
    ): String {
        val path = Paths.get(filePath)

        // If vault path is not configured, try to use empty (will be overridden in tests)
        if (filePath.isBlank()) {
            if (isTestEnvironment()) {
                logger.debug("vault.username not configured in test environment, relying on Spring properties")
                return ""
            } else {
                throw IllegalStateException(
                    "vault.username is not configured in production environment. " +
                            "Please set vault.username to the path of the vault credentials file."
                )
            }
        }

        // If vault path is configured but file doesn't exist, fail fast in production
        if (!Files.exists(path)) {
            if (isTestEnvironment()) {
                logger.debug("vault.username path does not exist in test environment, relying on Spring properties")
                return ""
            } else {
                throw IllegalStateException(
                    "vault.username path '$filePath' does not exist in production environment. " +
                            "Vault credentials file is not mounted correctly. Check NAIS vault configuration."
                )
            }
        }

        return try {
            Files.readString(path).trim()
        } catch (e: Exception) {
            if (isTestEnvironment()) {
                logger.debug("Could not read vault.username file in test environment", e)
                ""
            } else {
                throw IllegalStateException(
                    "Cannot read vault.username from '$filePath' in production environment. " +
                            "Check file permissions and vault mount configuration.",
                    e
                )
            }
        }
    }

    @Bean
    fun vaultDatasourcePassword(
        @Value("\${vault.password:}") filePath: String,
    ): String {
        val path = Paths.get(filePath)

        if (filePath.isBlank()) {
            if (isTestEnvironment()) {
                logger.debug("vault.password not configured in test environment, relying on Spring properties")
                return ""
            } else {
                throw IllegalStateException(
                    "vault.password is not configured in production environment. " +
                            "Please set vault.password to the path of the vault credentials file."
                )
            }
        }

        if (!Files.exists(path)) {
            if (isTestEnvironment()) {
                logger.debug("vault.password path does not exist in test environment, relying on Spring properties")
                return ""
            } else {
                throw IllegalStateException(
                    "vault.password path '$filePath' does not exist in production environment. " +
                            "Vault credentials file is not mounted correctly. Check NAIS vault configuration."
                )
            }
        }

        return try {
            Files.readString(path).trim()
        } catch (e: Exception) {
            if (isTestEnvironment()) {
                logger.debug("Could not read vault.password file in test environment", e)
                ""
            } else {
                throw IllegalStateException(
                    "Cannot read vault.password from '$filePath' in production environment. " +
                            "Check file permissions and vault mount configuration.",
                    e
                )
            }
        }
    }

    @Bean
    fun vaultDatasourceUrl(
        @Value("\${vault.config:}") configPath: String,
        @Value("\${vault.configKey:jdbcUrl}") configKey: String,
    ): String {
        val path = Paths.get(configPath)

        if (configPath.isBlank()) {
            if (isTestEnvironment()) {
                logger.debug("vault.config not configured in test environment, relying on Spring properties")
                return ""
            } else {
                throw IllegalStateException(
                    "vault.config is not configured in production environment. " +
                            "Please set vault.config to the path of the vault config directory."
                )
            }
        }

        if (!Files.exists(path)) {
            if (isTestEnvironment()) {
                logger.debug("vault.config path does not exist in test environment, relying on Spring properties")
                return ""
            } else {
                throw IllegalStateException(
                    "vault.config path '$configPath' does not exist in production environment. " +
                            "Vault config directory is not mounted correctly. Check NAIS vault configuration."
                )
            }
        }

        val filePath = if (Files.isDirectory(path)) {
            val primary = path.resolve(configKey)
            if (Files.exists(primary)) {
                primary
            } else {
                val fallbackKey = if (configKey == "jdbcUrl") "url" else "jdbcUrl"
                val fallback = path.resolve(fallbackKey)
                if (Files.exists(fallback)) {
                    fallback
                } else {
                    if (isTestEnvironment()) {
                        logger.debug("vault.config directory '$configPath' has no key files in test environment, relying on Spring properties")
                        return ""
                    } else {
                        throw IllegalStateException(
                            "vault.config directory '$configPath' does not contain key files '$configKey' or '$fallbackKey' in production environment. " +
                                    "Check vault mount configuration and NAIS secret structure."
                        )
                    }
                }
            }
        } else {
            path
        }

        return try {
            val content = Files.readString(filePath).trim()
            if (content.isEmpty()) {
                if (isTestEnvironment()) {
                    logger.debug("vault.config file is empty in test environment, relying on Spring properties")
                    ""
                } else {
                    throw IllegalStateException(
                        "vault.config file at '$filePath' is empty in production environment. " +
                                "Check vault secret configuration."
                    )
                }
            } else {
                logger.debug("Successfully read datasource URL from vault")
                content
            }
        } catch (e: Exception) {
            if (isTestEnvironment()) {
                logger.debug("Could not read vault.config file in test environment", e)
                ""
            } else {
                throw IllegalStateException(
                    "Cannot read vault.config from '$filePath' in production environment. " +
                            "Check file permissions and vault mount configuration.",
                    e
                )
            }
        }
    }

    @Bean
    @Primary
    fun datasource(
        datasourceConfiguration: DatasourceConfiguration,
        vaultDatasourceUsername: String,
        vaultDatasourcePassword: String,
        vaultDatasourceUrl: String,
        @Value("\${spring.datasource.url:}") springUrl: String,
        @Value("\${spring.datasource.username:}") springUsername: String,
        @Value("\${spring.datasource.password:}") springPassword: String,
    ): DataSource {
        // Prefer spring.datasource properties if available (tests via TestPropertyValues)
        // Fall back to vault-based values for production
        val url = if (springUrl.isNotBlank()) springUrl else vaultDatasourceUrl
        val username = if (springUsername.isNotBlank()) springUsername else vaultDatasourceUsername
        val password = if (springPassword.isNotBlank()) springPassword else vaultDatasourcePassword

        require(url.isNotBlank()) {
            "Datasource URL must be configured via spring.datasource.url or Vault (url/jdbcUrl file)."
        }
        require(username.isNotBlank()) {
            "Datasource username must be configured via spring.datasource.username or Vault (username file)."
        }
        require(password.isNotBlank()) {
            "Datasource password must be configured via spring.datasource.password or Vault (password file)."
        }
        // Validate final credentials - fail fast if datasource cannot be configured
        if (url.isBlank()) {
            throw IllegalStateException(
                "No datasource URL available. Neither spring.datasource.url nor vault.config are configured. " +
                        "Please configure at least one credential source."
            )
        }
        if (username.isBlank()) {
            throw IllegalStateException(
                "No datasource username available. Neither spring.datasource.username nor vault.username are configured. " +
                        "Please configure at least one credential source."
            )
        }
        if (password.isBlank()) {
            throw IllegalStateException(
                "No datasource password available. Neither spring.datasource.password nor vault.password are configured. " +
                        "Please configure at least one credential source."
            )
        }

        logger.info("Datasource configured with URL: $url, Username: $username")

        val driverClassName = requireNotNull(datasourceConfiguration.driverClassName) { "spring.datasource.driverClassName is null" }
        val dataSourceBuilder = DataSourceBuilder.create()
        dataSourceBuilder.driverClassName(driverClassName)
        dataSourceBuilder.url(url)
        dataSourceBuilder.username(username)
        dataSourceBuilder.password(password)
        return dataSourceBuilder.build()
    }
}

@ConfigurationProperties(prefix = "spring.datasource")
@Validated
data class DatasourceConfiguration(
    @NotEmpty
    var driverClassName: String? = null,
)