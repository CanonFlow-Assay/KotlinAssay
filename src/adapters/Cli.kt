package kotlinassay.adapters

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinassay.domain.*
import kotlinx.collections.immutable.*
import kotlinx.serialization.json.*

data class Policy(
    val domains: PersistentList<String>,
    val boundaries: PersistentList<String>,
    val commands: PersistentList<PersistentList<String>>,
)

fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
fun json(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Map<*, *> -> JsonObject(value.entries.sortedBy { it.key.toString() }.associate { it.key.toString() to json(it.value) })
    is Iterable<*> -> JsonArray(value.map(::json))
    else -> JsonPrimitive(value.toString())
}

fun parsePolicy(file: File): Policy = Json.parseToJsonElement(file.readText()).jsonObject.let { data ->
    require(data.keys == setOf("schema", "profile", "domainRoots", "boundaryRoots", "requiredCommands")) { "Policy keys must match the versioned schema exactly" }
    require(data.getValue("schema").jsonPrimitive.content == "kotlin-assay-policy/0.1") { "Unsupported policy schema" }
    require(data.getValue("profile").jsonPrimitive.content == "strict-syntax/0.1") { "Unsupported profile" }
    Policy(
        data.getValue("domainRoots").jsonArray.map { it.jsonPrimitive.content }.toPersistentList(),
        data.getValue("boundaryRoots").jsonArray.map { it.jsonPrimitive.content }.toPersistentList(),
        data.getValue("requiredCommands").jsonArray.map { command -> command.jsonArray.map { it.jsonPrimitive.content }.toPersistentList() }.toPersistentList(),
    ).also { policy ->
        require(policy.domains.isNotEmpty()) { "At least one domain root is required" }
        require((policy.domains + policy.boundaries).all { it.isNotBlank() && !File(it).isAbsolute && it.split('/').none { part -> part == ".." || part == "." || part.isBlank() } }) { "Roots must be explicit relative directories" }
        require((policy.domains + policy.boundaries).let { roots -> roots.distinct().size == roots.size && roots.none { a -> roots.any { b -> a != b && b.startsWith("$a/") } } }) { "Overlapping roots are not allowed" }
        require(policy.commands.all { it.isNotEmpty() && it.all(String::isNotBlank) }) { "Commands need nonempty literal argument vectors" }
    }
}

fun classified(path: String, roots: PersistentList<String>): Boolean = roots.any { path.startsWith("$it/") }
fun files(root: File): PersistentList<File> = root.walkTopDown().onEnter { it == root || it.name !in setOf(".git", "build", ".kotlinassay") }
    .filter { it.isFile && it.extension in setOf("kt", "kts") }.sortedBy { it.relativeTo(root).invariantSeparatorsPath }.toList().toPersistentList()

fun identity(file: File): Map<String, String> = mapOf("path" to file.path, "sha256" to digest(file.readBytes()))

fun runCommand(root: File, argv: PersistentList<String>, index: Int): Map<String, Any?> =
    runCatching {
        File(root, ".kotlinassay/command-$index.log").let { log ->
            ProcessBuilder(argv).directory(root).redirectErrorStream(true).redirectOutput(log).start().let { process ->
                if (!process.waitFor(45, TimeUnit.SECONDS)) {
                    process.descendants().forEach { it.destroyForcibly() }
                    process.destroyForcibly()
                    mapOf("argv" to argv, "status" to "Unavailable", "exitCode" to null, "outputSha256" to digest(log.readBytes()), "reason" to "45-second timeout")
                } else {
                    mapOf("argv" to argv, "status" to if (process.exitValue() == 0) "Passed" else "Failed", "exitCode" to process.exitValue(), "outputSha256" to digest(log.readBytes()))
                }
            }
        }
    }.getOrElse { failure -> mapOf("argv" to argv, "status" to "Unavailable", "exitCode" to null, "outputSha256" to null, "reason" to failure.javaClass.simpleName) }

fun inspect(mode: String, root: File): Pair<Int, JsonElement> =
    File(root, ".kotlinassay.json").let { config ->
        if (!config.isFile) 2 to json(mapOf("verdict" to "Inconclusive", "reason" to "Missing .kotlinassay.json"))
        else {
            val policy = parsePolicy(config)
            val policyDigest = digest(config.readBytes())
            val sources = files(root)
            val paths = sources.map { it.relativeTo(root).invariantSeparatorsPath }
            val snapshots = sources.associate { it.relativeTo(root).invariantSeparatorsPath to digest(it.readBytes()) }
            val domain = sources.filter { classified(it.relativeTo(root).invariantSeparatorsPath, policy.domains) }
            val boundary = sources.filter { classified(it.relativeTo(root).invariantSeparatorsPath, policy.boundaries) }
            val unclassified = paths.filter { !classified(it, policy.domains) && !classified(it, policy.boundaries) }
            val compilerOk = KotlinVersion.CURRENT.toString() == "2.4.20"
            val symlinksOk = sources.all { it.canonicalFile.toPath().startsWith(root.canonicalFile.toPath()) }
            val completeScope = domain.isNotEmpty() && unclassified.isEmpty() && sources.none { it.extension == "kts" } && compilerOk && symlinksOk && (policy.domains + policy.boundaries).all { File(root, it).isDirectory }
            val analysis = withParser { factory ->
                val parsedDomain = parseSources(factory, domain.map { it.relativeTo(root).invariantSeparatorsPath to it.readText() }.toPersistentList())
                val parsedBoundary = parseSources(factory, boundary.map { it.relativeTo(root).invariantSeparatorsPath to it.readText() }.toPersistentList())
                val allNodes = (parsedDomain + parsedBoundary).flatMap { descendants(it.tree) }
                val tags = parsedDomain.flatMap { descendants(it.tree) }.filter { "sealed-interface" in it.flags }.mapNotNull { it.name }.toPersistentSet()
                val imports = parsedDomain.flatMap(::declarations).toPersistentSet()
                val boundaryImports = parsedBoundary.flatMap(::declarations).toPersistentSet()
                analyze(parsedDomain, tags, imports, boundaryImports) to allNodes.none { it.kind == "PsiErrorElementImpl" || it.kind.contains("ErrorElement") }
            }
            File(root, ".kotlinassay").mkdirs()
            val compiler = System.getenv("KOTLIN_ASSAY_COMPILER")
            val compileArgs: PersistentList<String> = (listOf(compiler ?: "missing-pinned-kotlinc") + sources.map { it.absolutePath } + listOf(
                "-classpath", System.getProperty("java.class.path"), "-jvm-target", "21", "-d", File(root, ".kotlinassay/compiled.jar").path,
            )).toPersistentList()
            val compileEvidence = if (mode == "verify" && completeScope && analysis.second && compiler != null) runCommand(root, compileArgs.toPersistentList(), -1)
                else mapOf("argv" to compileArgs, "status" to "NotRun", "exitCode" to null, "outputSha256" to null)
            val commands = policy.commands.mapIndexed { index, argv ->
                if (mode == "verify" && completeScope && analysis.second && compileEvidence["status"] == "Passed") runCommand(root, argv, index)
                else mapOf("argv" to argv, "status" to "NotRun", "exitCode" to null, "outputSha256" to null)
            }
            val afterFiles = files(root)
            val unchanged = snapshots == afterFiles.associate { it.relativeTo(root).invariantSeparatorsPath to digest(it.readBytes()) } && policyDigest == digest(config.readBytes())
            val statuses = (persistentListOf(compileEvidence["status"].toString()) + commands.map { it["status"].toString() } +
                if (policy.commands.isEmpty()) persistentListOf("Unavailable") else persistentListOf()).toPersistentList()
            val outcome = verdict(analysis.first, completeScope && analysis.second, statuses, unchanged)
            val receipt = json(mapOf(
                "schema" to "kotlin-assay-receipt/0.1", "toolVersion" to "0.1.0", "profile" to "strict-syntax/0.1",
                "mode" to mode, "verdict" to outcome, "authority" to "PSI syntax rules plus configured command evidence only",
                "compilerVersion" to KotlinVersion.CURRENT.toString(), "javaVersion" to System.getProperty("java.version"),
                "toolchainLockSha256" to System.getenv("KOTLIN_ASSAY_TOOLCHAIN_SHA256"),
                "policySha256" to policyDigest, "source" to snapshots, "rules" to catalog.map { it.id },
                "findings" to analysis.first.map { mapOf("path" to it.path, "line" to it.line, "column" to it.column, "rule" to it.rule, "message" to it.message) },
                "domainPaths" to domain.map { it.relativeTo(root).invariantSeparatorsPath }, "boundaryPaths" to boundary.map { it.relativeTo(root).invariantSeparatorsPath },
                "unclassifiedPaths" to unclassified, "scopeComplete" to completeScope, "parseComplete" to analysis.second,
                "sourceUnchanged" to unchanged, "compilation" to compileEvidence, "commands" to commands,
                "toolJarSha256" to File(object {}.javaClass.protectionDomain.codeSource.location.toURI()).let { if (it.isFile) digest(it.readBytes()) else null },
                "nonclaims" to listOf("No semantic symbol resolution or universal effect/purity proof", "No arbitrary deep-immutability proof", "Boundary code is not certified pure", "No Android alarm/delivery or health validation"),
            ))
            File(root, ".kotlinassay/$mode.json").writeText(receipt.toString() + "\n")
            when (outcome) { "Pass" -> 0; "Fail" -> 1; "Inconclusive" -> 2; else -> 3 } to receipt
        }
    }

fun main(args: Array<String>): Unit =
    if (args.size != 2 || args[0] !in setOf("scan", "verify", "doctor")) {
        System.err.println("Usage: kotlin-assay <doctor|scan|verify> <target>")
        kotlin.system.exitProcess(64)
    } else {
        val root = File(args[1]).canonicalFile
        val result = runCatching {
            if (args[0] == "doctor") 0 to json(mapOf("command" to "doctor", "kotlinVersion" to KotlinVersion.CURRENT.toString(), "targetExists" to root.isDirectory, "profile" to "strict-syntax/0.1", "authority" to "readiness only"))
            else if (!root.isDirectory) 2 to json(mapOf("verdict" to "Inconclusive", "reason" to "Missing target"))
            else inspect(args[0], root)
        }.getOrElse { error -> 3 to json(mapOf("verdict" to "ToolFailure", "reason" to (error.message ?: error.javaClass.simpleName))) }
        println(result.second)
        kotlin.system.exitProcess(result.first)
    }
