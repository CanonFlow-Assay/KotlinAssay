package playground.firewall

fun main() {
    val cases = listOf(
        Triple(
            "valid",
            mapOf("Service:Endpoint" to "https://api.example.test", "Service:RetryLimit" to "3", "Service:Mode" to "read-only"),
            "loaded|https://api.example.test/|3|ReadOnly"
        ),
        Triple(
            "missing",
            mapOf("Service:Endpoint" to "https://api.example.test", "Service:RetryLimit" to "3"),
            "rejected|missing:Service:Mode"
        ),
        Triple(
            "bad-uri",
            mapOf("Service:Endpoint" to "not-a-uri", "Service:RetryLimit" to "3", "Service:Mode" to "write"),
            "rejected|invalid-uri:Service:Endpoint"
        ),
        Triple(
            "bad-limit",
            mapOf("Service:Endpoint" to "https://api.example.test", "Service:RetryLimit" to "0", "Service:Mode" to "write"),
            "rejected|invalid-range:Service:RetryLimit"
        ),
        Triple(
            "bad-mode",
            mapOf("Service:Endpoint" to "https://api.example.test", "Service:RetryLimit" to "3", "Service:Mode" to "admin"),
            "rejected|invalid-mode:Service:Mode"
        ),
        Triple(
            "multiple-issues",
            mapOf("Service:Endpoint" to "ftp://bad.example.test", "Service:RetryLimit" to "99"),
            "rejected|invalid-uri:Service:Endpoint;invalid-range:Service:RetryLimit;missing:Service:Mode"
        )
    )
    for ((name, values, expected) in cases) {
        val actual = canonicalConfiguration(loadConfiguration(values))
        if (actual != expected) {
            System.err.println("$name: expected $expected, got $actual")
            kotlin.system.exitProcess(1)
        }
    }
    println("Kotlin configuration firewall: ${cases.size} cases passed.")
}
