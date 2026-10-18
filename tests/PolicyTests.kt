package kotlinassay.tests

import kotlinassay.adapters.parseSources
import kotlinassay.adapters.withParser
import kotlinassay.domain.*
import kotlinx.collections.immutable.*

data class Specimen(val rule: String, val good: String, val bad: String)

val specimens = persistentListOf(
    Specimen("KFA001", "data class Sample(val minutes: Int)", "data class Sample(var minutes: Int)"),
    Specimen("KFA002", "fun add(x: Int) = x + 1", "fun add(x: Int): Int { var y = x; y += 1; return y }"),
    Specimen("KFA003", "fun sum(xs: PersistentList<Int>) = xs.fold(0) { a, b -> a + b }", "fun sum(xs: PersistentList<Int>): Int { for (x in xs) println(x); return 0 }"),
    Specimen("KFA004", "sealed interface State\ndata class Active(val minutes: Int): State", "open class Service { fun run() = 1 }"),
    Specimen("KFA005", "data class Sample(val minutes: Int)", "data class Sample(val minutes: Int) { fun value() = minutes }"),
    Specimen("KFA006", "fun valid(x: Int) = x > 0", "fun valid(x: Int) = if (x > 0) true else throw IllegalArgumentException()"),
    Specimen("KFA007", "fun size(x: String?) = x?.length ?: 0", "fun size(x: String?) = x!!.length"),
    Specimen("KFA008", "fun tick(now: Long) = now + 1", "fun tick() = System.currentTimeMillis()"),
    Specimen("KFA009", "fun scale(xs: PersistentList<Int>) = xs.map { it * 2 }.toPersistentList()", "fun scale(xs: PersistentList<Int>) = xs.forEach { println(it) }"),
    Specimen("KFA010", "data class Values(val xs: PersistentList<Int>)", "data class Values(val xs: List<Int>)"),
    Specimen("KFA011", "import kotlin.math.abs\nfun norm(x: Int) = abs(x)", "import kotlin.io.println as emit\nfun output() = emit(1)"),
    Specimen("KFA012", "fun stable(x: Int) = x", "suspend fun stable(x: Int) = x"),
    Specimen("KFA013", "sealed interface State\ndata object Ready: State\nfun name(s: State) = when(s) { Ready -> 1 }", "sealed interface State\ndata object Ready: State\nfun name(s: State) = when(s) { else -> 1 }"),
    Specimen("KFA014", "@JvmInline value class Minutes(val value: Int)", "@Suppress(\"ALL\") fun unchecked() = 1"),
    Specimen("KFA015", "fun pure(x: Int) = x + 1", "import example.adapters.clock\nfun tick() = clock()"),
    Specimen("KFA016", "fun pure(x: Int) = x + 1", "fun pure(x: Int): Int { return x + 1 }"),
    Specimen("KFA017", "data class Values(val n: Int)", "typealias Values = MutableList<Int>"),
    Specimen("KFA018", "fun length(x: String) = x.length", "fun identity(x: Any) = x::class"),
)

fun findings(factory: org.jetbrains.kotlin.psi.KtPsiFactory, code: String): PersistentList<Finding> =
    parseSources(factory, persistentListOf("specimen.kt" to ("import kotlinx.collections.immutable.PersistentList\nimport kotlinx.collections.immutable.toPersistentList\n" + code))).let { sources ->
        analyze(sources, sources.flatMap { descendants(it.tree) }.filter { "sealed-interface" in it.flags }.mapNotNull { it.name }.toPersistentSet(), persistentSetOf(), persistentSetOf("example.adapters.clock"))
    }

fun main(): Unit = withParser { factory ->
    val checks = specimens.flatMap { specimen ->
        persistentListOf(
            "${specimen.rule} accepts good shape" to findings(factory, specimen.good).none { it.rule == specimen.rule },
            "${specimen.rule} rejects bad shape" to findings(factory, specimen.bad).any { it.rule == specimen.rule },
        )
    } + persistentListOf(
        "comments and strings are not violations" to findings(factory, "// var println throw while\nfun text() = \"var println throw while\"").isEmpty(),
        "nested mutation is detected" to findings(factory, "fun f(xs: PersistentList<Int>) = xs.map { var n = it; n++; n }").any { it.rule == "KFA001" || it.rule == "KFA002" },
        "fully-qualified effects are detected" to findings(factory, "fun f() = kotlin.io.println(1)").any { it.rule == "KFA008" || it.rule == "KFA012" },
        "fully-qualified boundary calls are detected" to findings(factory, "fun f() = example.adapters.clock()").any { it.rule == "KFA015" },
        "class companion is rejected" to findings(factory, "data class Value(val x: Int) { companion object { fun unsafe() = 1 } }").any { it.rule == "KFA004" || it.rule == "KFA005" },
        "immutable global constant accepted" to findings(factory, "val cadence = 35\nfun next(now: Int) = now + cadence").isEmpty(),
        "missing evidence remains inconclusive" to (verdict(persistentListOf(), true, persistentListOf("NotRun"), true) == "Inconclusive"),
        "failed command fails" to (verdict(persistentListOf(), true, persistentListOf("Failed"), true) == "Fail"),
        "source drift remains inconclusive" to (verdict(persistentListOf(), true, persistentListOf("Passed"), false) == "Inconclusive"),
        "complete named evidence passes" to (verdict(persistentListOf(), true, persistentListOf("Passed"), true) == "Pass"),
        "missing scope stays inconclusive" to (verdict(persistentListOf(), false, persistentListOf("Passed"), true) == "Inconclusive"),
        "tool failure stays distinct" to (verdict(persistentListOf(), true, persistentListOf("ToolFailure"), true) == "ToolFailure"),
    )
    val failures = checks.filterNot { it.second }
    failures.forEach { System.err.println("FAILED: ${it.first}") }
    println("Policy assertions: ${checks.size}; failed: ${failures.size}")
    check(failures.isEmpty()) { "Assay policy regression" }
}
