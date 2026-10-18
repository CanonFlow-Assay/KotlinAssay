package kotlinassay.adapters

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import kotlinassay.domain.Node
import kotlinassay.domain.Source
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.FrontendConfigurationKeys
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

fun snapshot(element: PsiElement): Node = Node(
    kind = element.javaClass.simpleName,
    text = element.text,
    offset = element.textRange.startOffset,
    name = when (element) {
        is KtImportDirective -> element.importedFqName?.asString()
        is KtCallExpression -> element.calleeExpression?.text?.substringAfterLast('.')
        is KtUserType -> element.referencedName
        is KtAnnotationEntry -> element.shortName?.asString()
        is KtNamedDeclaration -> element.name
        is KtNameReferenceExpression -> element.getReferencedName()
        else -> null
    },
    flags = listOfNotNull(
        "mutable".takeIf { element is KtProperty && element.isVar || element is KtParameter && element.isMutable },
        "assignment".takeIf { element is KtBinaryExpression && element.operationToken in KtTokens.ALL_ASSIGNMENTS },
        "increment".takeIf { element is KtUnaryExpression && element.operationToken in setOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS) },
        "non-null".takeIf { element is KtPostfixExpression && element.operationToken == KtTokens.EXCLEXCL },
        "cast".takeIf { element is KtBinaryExpressionWithTypeRHS },
        "sealed-interface".takeIf { element is KtClass && element.isInterface() && element.hasModifier(KtTokens.SEALED_KEYWORD) },
        "data".takeIf { element is KtClassOrObject && element.hasModifier(KtTokens.DATA_KEYWORD) },
        "value".takeIf { element is KtClass && element.hasModifier(KtTokens.VALUE_KEYWORD) },
        "enum".takeIf { element is KtClass && element.isEnum() },
        "nonempty-body".takeIf { element is KtClassOrObject && element.body?.declarations?.isNotEmpty() == true },
        "supertype".takeIf { element is KtClassOrObject && element.superTypeListEntries.isNotEmpty() },
        "enum-behavior".takeIf { element is KtClass && element.isEnum() && (element.primaryConstructorParameters.isNotEmpty() || element.body?.declarations?.any { it !is KtEnumEntry || it.body != null || it.superTypeListEntries.isNotEmpty() } == true) },
        "companion".takeIf { element is KtObjectDeclaration && element.isCompanion() },
        "member-behavior".takeIf { element is KtNamedFunction && element.parent is KtClassBody },
        "suspend".takeIf { element is KtModifierListOwner && element.hasModifier(KtTokens.SUSPEND_KEYWORD) },
        "block-body".takeIf { element is KtNamedFunction && element.hasBlockBody() },
        "subject-when-else".takeIf { element is KtWhenExpression && element.subjectExpression != null && element.entries.any { it.isElse } },
        "wildcard".takeIf { element is KtImportDirective && element.isAllUnder },
        "alias".takeIf { element is KtImportDirective && element.aliasName != null },
        "star".takeIf { element is KtTypeProjection && element.text == "*" },
    ).toPersistentSet(),
    children = (element.children.map(::snapshot) +
        if (element is KtClassOrObject) element.superTypeListEntries.map { entry ->
            Node("Supertype", entry.text, entry.textRange.startOffset, entry.typeReference?.text,
                if (entry is KtSuperTypeCallEntry) persistentSetOf("constructor-super") else persistentSetOf(),
                kotlinx.collections.immutable.persistentListOf())
        } else emptyList()).toPersistentList(),
)

@OptIn(org.jetbrains.kotlin.CoreEnvironmentDeprecation::class, CompilerConfiguration.Internals::class, org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
fun <T> withParser(action: (KtPsiFactory) -> T): T =
    Disposer.newDisposable("KotlinAssay PSI").let { disposable ->
        try {
            CompilerConfiguration().let { configuration ->
                configuration.put(FrontendConfigurationKeys.EXTENSIONS_STORAGE, CompilerPluginRegistrar.ExtensionStorage())
                KotlinCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
                    .let { environment -> action(KtPsiFactory(environment.project, false)) }
            }
        } finally {
            Disposer.dispose(disposable)
        }
    }

fun parseSources(factory: KtPsiFactory, entries: PersistentList<Pair<String, String>>): PersistentList<Source> =
    entries.map { (path, text) -> Source(path, text, snapshot(factory.createFile(path.substringAfterLast('/'), text))) }.toPersistentList()

fun declarations(source: Source): PersistentList<String> =
    kotlinassay.domain.descendants(source.tree).filter { it.kind in listOf("KtClass", "KtObjectDeclaration", "KtNamedFunction", "KtProperty", "KtTypeAlias") }
        .mapNotNull { node -> node.name?.let { name -> source.tree.children.firstOrNull { it.kind == "KtPackageDirective" }?.text?.removePrefix("package ")?.trim()?.let { "$it.$name" } } }
        .toPersistentList()
