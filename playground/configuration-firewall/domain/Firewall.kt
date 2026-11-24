package playground.firewall

sealed interface ServiceMode
data object ReadOnlyMode : ServiceMode
data object WriteMode : ServiceMode

data class ServiceConfiguration(
    val endpoint: String,
    val retryLimit: Int,
    val mode: ServiceMode
)

data class ConfigurationIssue(
    val code: String,
    val key: String
)

sealed interface ConfigurationLoad
data class ConfigurationLoaded(val value: ServiceConfiguration) : ConfigurationLoad
data class ConfigurationRejected(val issues: List<ConfigurationIssue>) : ConfigurationLoad

private fun parseEndpoint(raw: String?): Pair<String, List<ConfigurationIssue>> = when {
    raw == null -> "" to listOf(ConfigurationIssue("missing", "Service:Endpoint"))
    else -> {
        val trimmed = raw.trim()
        val hasHttp = trimmed.startsWith("https://") || trimmed.startsWith("http://")
        val authority = trimmed.removePrefix("https://").removePrefix("http://")
        if (!hasHttp || authority.isEmpty() || authority.startsWith("/")) {
            "" to listOf(ConfigurationIssue("invalid-uri", "Service:Endpoint"))
        } else {
            val normalized = if (authority.contains("/")) trimmed else "$trimmed/"
            normalized to emptyList()
        }
    }
}

private fun parseRetryLimit(raw: String?): Pair<Int, List<ConfigurationIssue>> = when {
    raw == null -> 0 to listOf(ConfigurationIssue("missing", "Service:RetryLimit"))
    else -> {
        val parsed = raw.trim().toIntOrNull()
        if (parsed == null || parsed < 1 || parsed > 10) {
            0 to listOf(ConfigurationIssue("invalid-range", "Service:RetryLimit"))
        } else {
            parsed to emptyList()
        }
    }
}

private fun parseMode(raw: String?): Pair<ServiceMode, List<ConfigurationIssue>> = when (raw) {
    null -> ReadOnlyMode to listOf(ConfigurationIssue("missing", "Service:Mode"))
    "read-only" -> ReadOnlyMode to emptyList()
    "write" -> WriteMode to emptyList()
    else -> ReadOnlyMode to listOf(ConfigurationIssue("invalid-mode", "Service:Mode"))
}

fun loadConfiguration(values: Map<String, String>): ConfigurationLoad {
    val (endpoint, endpointIssues) = parseEndpoint(values["Service:Endpoint"])
    val (retryLimit, limitIssues) = parseRetryLimit(values["Service:RetryLimit"])
    val (mode, modeIssues) = parseMode(values["Service:Mode"])
    val issues = endpointIssues + limitIssues + modeIssues
    return if (issues.isEmpty()) {
        ConfigurationLoaded(ServiceConfiguration(endpoint, retryLimit, mode))
    } else {
        ConfigurationRejected(issues)
    }
}

fun modeName(mode: ServiceMode): String = when (mode) {
    ReadOnlyMode -> "ReadOnly"
    WriteMode -> "Write"
}

fun canonicalConfiguration(result: ConfigurationLoad): String = when (result) {
    is ConfigurationLoaded -> "loaded|${result.value.endpoint}|${result.value.retryLimit}|${modeName(result.value.mode)}"
    is ConfigurationRejected -> "rejected|" + result.issues.joinToString(";") { "${it.code}:${it.key}" }
}
