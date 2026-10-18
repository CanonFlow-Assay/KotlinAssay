package kotlinassay.domain

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

data class Node(
    val kind: String,
    val text: String,
    val offset: Int,
    val name: String?,
    val flags: PersistentSet<String>,
    val children: PersistentList<Node>,
)

data class Source(val path: String, val text: String, val tree: Node)
data class Finding(val path: String, val line: Int, val column: Int, val rule: String, val message: String)
data class Rule(val id: String, val title: String)

val catalog: PersistentList<Rule> = persistentListOf(
    Rule("KFA001", "Mutable binding/property"),
    Rule("KFA002", "Assignment or increment/decrement"),
    Rule("KFA003", "Imperative loop or jump"),
    Rule("KFA004", "Behavioral OOP or non-ADT inheritance"),
    Rule("KFA005", "Member behavior, init, accessor or delegation"),
    Rule("KFA006", "Exception-based domain control"),
    Rule("KFA007", "Non-null assertion or cast"),
    Rule("KFA008", "Known ambient/effect operation"),
    Rule("KFA009", "Mutable factory/mutator or imperative scope API"),
    Rule("KFA010", "Non-persistent/mutable container type"),
    Rule("KFA011", "Unreviewed, aliased or wildcard import"),
    Rule("KFA012", "Infrastructure/interop in domain"),
    Rule("KFA013", "Else arm hiding subject-based matching"),
    Rule("KFA014", "Suppression or unreviewed annotation"),
    Rule("KFA015", "Domain reference to a boundary declaration"),
    Rule("KFA016", "Statement-bodied function"),
    Rule("KFA017", "Type alias outside the admitted syntax subset"),
    Rule("KFA018", "Erased/star/dynamic/reflection escape"),
)

val effectNames: PersistentSet<String> = kotlinx.collections.immutable.persistentSetOf(
    "print", "println", "readLine", "readln", "readlnOrNull", "readText", "readBytes", "writeText",
    "writeBytes", "appendText", "File", "Path", "ProcessBuilder", "exec", "exitProcess", "exit",
    "currentTimeMillis", "nanoTime", "now", "random", "nextInt", "nextDouble", "sleep", "Thread",
    "launch", "async", "delay", "runBlocking", "synchronized", "getenv", "getProperty", "require",
    "requireNotNull", "check", "checkNotNull", "error", "TODO", "runCatching", "use",
)

val mutableNames: PersistentSet<String> = kotlinx.collections.immutable.persistentSetOf(
    "mutableListOf", "mutableSetOf", "mutableMapOf", "arrayOf", "intArrayOf", "longArrayOf",
    "byteArrayOf", "doubleArrayOf", "booleanArrayOf", "Array", "ArrayList", "HashMap", "HashSet",
    "StringBuilder", "buildList", "buildSet", "buildMap", "builder", "toMutableList", "toMutableSet",
    "toMutableMap", "toTypedArray", "toIntArray", "add", "addAll", "remove", "removeAt", "removeAll",
    "retainAll", "clear", "put", "putAll", "set", "sort", "sortBy", "reverse", "fill", "shuffle",
    "forEach", "forEachIndexed", "onEach", "apply", "also",
)

val bannedTypes: PersistentSet<String> = kotlinx.collections.immutable.persistentSetOf(
    "List", "Set", "Map", "Collection", "Iterable", "Iterator", "Sequence", "MutableList",
    "MutableSet", "MutableMap", "MutableCollection", "MutableIterable", "MutableIterator", "Array",
    "IntArray", "LongArray", "ByteArray", "DoubleArray", "BooleanArray", "FloatArray", "CharArray",
    "ShortArray", "ArrayList", "HashMap", "HashSet", "StringBuilder",
)

fun descendants(node: Node): PersistentList<Node> =
    (persistentListOf(node) + node.children.flatMap(::descendants)).toPersistentList()

fun position(text: String, offset: Int): Pair<Int, Int> =
    text.take(offset).let { prefix ->
        (prefix.count { it == '\n' } + 1) to (offset - prefix.lastIndexOf('\n'))
    }

fun classShapeAllowed(node: Node, sealedTags: PersistentSet<String>): Boolean =
    when {
        "sealed-interface" in node.flags -> "nonempty-body" !in node.flags && "supertype" !in node.flags
        "data" in node.flags || "value" in node.flags ->
            "nonempty-body" !in node.flags && node.children.filter { it.kind == "Supertype" }
                .all { it.name in sealedTags && "constructor-super" !in it.flags }
        "enum" in node.flags -> "enum-behavior" !in node.flags
        else -> false
    }

fun issues(
    node: Node,
    sealedTags: PersistentSet<String>,
    domainImports: PersistentSet<String>,
    boundaryImports: PersistentSet<String>,
): PersistentList<String> = persistentListOf(
    "KFA001" to ("mutable" in node.flags),
    "KFA002" to ("assignment" in node.flags || "increment" in node.flags),
    "KFA003" to (node.kind in persistentListOf("KtForExpression", "KtWhileExpression", "KtDoWhileExpression", "KtBreakExpression", "KtContinueExpression", "KtReturnExpression")),
    "KFA004" to (
        (node.kind == "KtClass" && !classShapeAllowed(node, sealedTags)) ||
            (node.kind == "KtObjectDeclaration" && !("data" in node.flags && "companion" !in node.flags && "nonempty-body" !in node.flags && node.children.filter { it.kind == "Supertype" }.all { it.name in sealedTags && "constructor-super" !in it.flags })) ||
            node.kind == "KtObjectLiteralExpression"
        ),
    "KFA005" to ("member-behavior" in node.flags || node.kind in persistentListOf("KtAnonymousInitializer", "KtSecondaryConstructor", "KtPropertyAccessor", "KtPropertyDelegate", "KtDelegatedSuperTypeEntry")),
    "KFA006" to (node.kind in persistentListOf("KtThrowExpression", "KtTryExpression")),
    "KFA007" to ("non-null" in node.flags || "cast" in node.flags),
    "KFA008" to (node.kind == "KtCallExpression" && node.name in effectNames),
    "KFA009" to (node.kind == "KtCallExpression" && node.name in mutableNames),
    "KFA010" to (node.kind == "KtUserType" && node.name in bannedTypes),
    "KFA011" to (node.kind == "KtImportDirective" && (
        "wildcard" in node.flags || "alias" in node.flags ||
            !(node.name in domainImports || node.name?.startsWith("kotlin.math.") == true || node.name?.startsWith("kotlinx.collections.immutable.") == true)
        )),
    "KFA012" to (
        "suspend" in node.flags ||
            (node.kind in persistentListOf("KtDotQualifiedExpression", "KtUserType", "KtImportDirective") &&
                persistentListOf("java.", "javax.", "android.", "androidx.", "kotlin.io.", "kotlin.system.", "kotlin.random.", "kotlinx.coroutines.").any { node.text.startsWith(it) })
        ),
    "KFA013" to ("subject-when-else" in node.flags),
    "KFA014" to (node.kind == "KtAnnotationEntry" && node.name != "JvmInline"),
    "KFA015" to (node.kind == "KtImportDirective" && node.name in boundaryImports ||
        node.kind == "KtDotQualifiedExpression" && boundaryImports.any { node.text == it || node.text.startsWith("$it(") }),
    "KFA016" to (node.kind == "KtNamedFunction" && "block-body" in node.flags),
    "KFA017" to (node.kind == "KtTypeAlias"),
    "KFA018" to (node.kind in persistentListOf("KtClassLiteralExpression", "KtDynamicType") || "star" in node.flags || node.kind == "KtUserType" && node.name == "Any" || node.kind == "KtNameReferenceExpression" && node.name in persistentListOf("javaClass", "java", "getClass", "Class", "reflect")),
).filter { it.second }.map { it.first }.toPersistentList()

fun analyze(
    sources: PersistentList<Source>,
    sealedTags: PersistentSet<String>,
    domainImports: PersistentSet<String>,
    boundaryImports: PersistentSet<String>,
): PersistentList<Finding> = sources.flatMap { source ->
    descendants(source.tree).flatMap { node ->
        issues(node, sealedTags, domainImports, boundaryImports).map { id ->
            position(source.text, node.offset).let { pos ->
                Finding(source.path, pos.first, pos.second, id, catalog.first { it.id == id }.title)
            }
        }
    }
}.distinct().sortedWith(compareBy(Finding::path, Finding::line, Finding::column, Finding::rule)).toPersistentList()

fun verdict(findings: PersistentList<Finding>, complete: Boolean, commandStatuses: PersistentList<String>, snapshotUnchanged: Boolean): String =
    when {
        !snapshotUnchanged -> "Inconclusive"
        commandStatuses.any { it == "ToolFailure" } -> "ToolFailure"
        !complete -> "Inconclusive"
        findings.isNotEmpty() || commandStatuses.any { it == "Failed" } -> "Fail"
        commandStatuses.isEmpty() || commandStatuses.any { it != "Passed" } -> "Inconclusive"
        else -> "Pass"
    }
