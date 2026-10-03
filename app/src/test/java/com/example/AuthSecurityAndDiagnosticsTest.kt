package com.example

import com.example.data.local.SiteEntity
import com.example.data.remote.LoggingAuthenticator
import com.example.data.remote.WordPress401LogEntry
import com.example.data.remote.WordPress401LogStore
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthSecurityAndDiagnosticsTest {

    @Before
    fun setUp() {
        WordPress401LogStore.clearLogs()
    }

    @Test
    fun siteEntity_defaultsAreCleanAndSecure() {
        val site = SiteEntity(
            id = "test_site_1",
            name = "My Live WordPress Site",
            url = "https://example.com"
        )
        assertFalse("New site must not be authenticated by default", site.isAuthenticated)
        assertEquals("Total posts should default to 0", 0, site.totalPosts)
        assertEquals("Total pages should default to 0", 0, site.totalPages)
        assertEquals("Total categories should default to 0", 0, site.totalCategories)
        assertEquals("Total comments should default to 0", 0, site.totalComments)
        assertEquals("Total sales should default to 0.0", 0.0, site.totalSales, 0.001)
        assertEquals("Active theme should default to empty string", "", site.activeTheme)
        assertEquals("PHP version should default to empty string", "", site.phpVersion)
    }

    @Test
    fun wordPress401LogStore_recordsAndClearsLogsCorrectly() {
        assertEquals("Log store should be empty initially", 0, WordPress401LogStore.logs.value.size)

        val entry = WordPress401LogEntry(
            url = "https://example.com/wp-json/wp/v2/posts",
            method = "GET",
            requestHeaders = mapOf("User-Agent" to "WPHub/1.0"),
            responseCode = 401,
            responseMessage = "Unauthorized",
            responseHeaders = mapOf("Server" to "Apache"),
            responseBodyRaw = "{\"code\":\"rest_cannot_view\"}",
            wwwAuthenticateHeader = "Basic realm=\"WordPress\"",
            authorizationHeaderPresent = true,
            authorizationHeaderMasked = "Basic [PROTECTED_USER: admin, PASSWORD: ••••••••]",
            probableCause = "Application password revoked or invalid.",
            recommendedFix = "Generate a new Application Password in WordPress admin."
        )

        WordPress401LogStore.addLog(entry)
        assertEquals("Log store should have 1 entry", 1, WordPress401LogStore.logs.value.size)
        assertEquals(entry.url, WordPress401LogStore.logs.value.first().url)
        assertFalse(
            "Log should never leak raw base64 or plaintext passwords",
            WordPress401LogStore.logs.value.first().authorizationHeaderMasked?.contains("admin:password") == true
        )

        WordPress401LogStore.clearLogs()
        assertEquals("Log store should be cleared", 0, WordPress401LogStore.logs.value.size)
    }

    @Test
    fun loggingAuthenticator_intercepts401AndMasksCredentials() {
        val authenticator = LoggingAuthenticator()

        val request = Request.Builder()
            .url("https://myshop.com/wp-json/wp/v2/users/me")
            .header("Authorization", "Basic YWRtaW46c2VjcmV0cGFzc3dvcmQ=") // admin:secretpassword
            .header("X-WP-App", "WPMobileHub")
            .get()
            .build()

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .header("WWW-Authenticate", "Basic realm=\"WordPress\"")
            .header("Server", "LiteSpeed")
            .body("{\"code\":\"rest_cannot_view\",\"message\":\"Sorry, you are not allowed to access this resource.\"}".toResponseBody())
            .build()

        val nextRequest = authenticator.authenticate(null, response)

        assertNull("Authenticator should return null to prevent infinite 401 loops", nextRequest)
        assertEquals("One 401 event must be captured", 1, WordPress401LogStore.logs.value.size)

        val captured = WordPress401LogStore.logs.value.first()
        assertEquals("https://myshop.com/wp-json/wp/v2/users/me", captured.url)
        assertEquals(401, captured.responseCode)
        assertTrue(captured.authorizationHeaderPresent)

        // Ensure credentials were masked
        val maskedAuth = captured.authorizationHeaderMasked.orEmpty()
        assertTrue("Masked auth must show protected username", maskedAuth.contains("PROTECTED_USER: admin"))
        assertTrue("Masked auth must replace password with bullet mask", maskedAuth.contains("••••••••"))
        assertFalse("Masked auth must NEVER contain raw Base64", maskedAuth.contains("YWRtaW46c2VjcmV0cGFzc3dvcmQ="))
        assertFalse("Masked auth must NEVER contain raw secret password", maskedAuth.contains("secretpassword"))
    }
}
