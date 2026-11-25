package no.nav.historisk.innsyn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class ImageScanCheckTest {

    @TempDir
    lateinit var tempDir: Path

    private val script = Path.of("scripts/check-image-scan.sh").toAbsolutePath()

    @Test
    fun `accepts known OS with package inventory and no targeted CVE`() {
        val result = checkReport(
            """
                {
                  "Metadata": {"OS": {"Family": "alpine"}},
                  "Results": [{"Class": "os-pkgs", "Packages": [{"Name": "libc"}]}]
                }
            """.trimIndent(),
        )

        assertThat(result.exitCode).isZero()
        assertThat(result.output).contains("OK:")
    }

    @Test
    fun `rejects report containing the targeted CVE`() {
        val result = checkReport(
            """
                {
                  "Metadata": {"OS": {"Family": "alpine"}},
                  "Results": [{
                    "Class": "os-pkgs",
                    "Packages": [{"Name": "libc"}],
                    "Vulnerabilities": [{"VulnerabilityID": "CVE-2026-115506"}]
                  }]
                }
            """.trimIndent(),
        )

        assertThat(result.exitCode).isNotZero()
        assertThat(result.output).contains("CVE-2026-115506")
    }

    @Test
    fun `rejects missing OS metadata result or package inventory`() {
        listOf(
            """{"Results": [{"Class": "os-pkgs", "Packages": [{"Name": "libc"}]}]}""",
            """{"Metadata": {"OS": {"Family": "alpine"}}}""",
            """{"Metadata": {"OS": {"Family": "alpine"}}, "Results": [{"Class": "os-pkgs", "Packages": []}]}""",
        ).forEach { report ->
            val result = checkReport(report)

            assertThat(result.exitCode).isNotZero()
            assertThat(result.output).contains("komplett OS-pakkeinventar")
        }
    }

    private fun checkReport(report: String): CheckResult {
        val reportPath = tempDir.resolve("image-scan.json")
        Files.writeString(reportPath, report)
        val process = ProcessBuilder("bash", script.toString(), reportPath.toString())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        return CheckResult(process.waitFor(), output)
    }

    private data class CheckResult(val exitCode: Int, val output: String)
}
