package no.nav.historisk.innsyn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class LogbackConfigurationTest {
    @Test
    fun `debug for no nav er begrenset til dev og test`() {
        val xml = requireNotNull(javaClass.getResourceAsStream("/logback-spring.xml"))
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val document = xml.use { factory.newDocumentBuilder().parse(it) }
        val loggers = document.getElementsByTagName("logger")
        val navLogger = (0 until loggers.length)
            .map { loggers.item(it) as Element }
            .single { it.getAttribute("name") == "no.nav" }

        assertThat(navLogger.getAttribute("level")).isEqualTo("DEBUG")
        assertThat((navLogger.parentNode as Element).getAttribute("name")).isEqualTo("dev | test")
    }
}
