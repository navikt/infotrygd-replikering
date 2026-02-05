package no.nav.historisk.innsyn.utils

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class TokenHelper(private val ctxHolder: TokenValidationContextHolder) {
    fun grupper(): List<String> {
        val oidcValidationContext: TokenValidationContext = ctxHolder.getTokenValidationContext()
        return oidcValidationContext.getClaims(AzureIssuer).getAsList("groups")?:
            throw IllegalStateException("Token mangler claim \"groups\"")
    }


    companion object {
        const val AzureIssuer = "azure"
        // https://docs.microsoft.com/en-us/azure/active-directory/develop/access-tokens#payload-claims
        private const val AzureV1ClientIdClaim = "appid"
        private const val AzureV2ClientIdClaim = "azp"
    }
}