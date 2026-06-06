package pers.hpcx.foxlang.sema

import pers.hpcx.foxlang.frontend.*
import pers.hpcx.foxlang.runtime.FoxMethodSignature
import pers.hpcx.foxlang.runtime.isSatisfiedBy
import pers.hpcx.foxlang.types.*
import java.util.*

data class FoxSemanticPlaceholderReport(
    val typeAliases: List<FoxTypeAliasPlaceholder> = emptyList(),
    val methods: List<FoxMethodPlaceholder> = emptyList(),
    val callSites: List<FoxCallSitePlaceholder> = emptyList(),
    val diagnostics: List<FoxPlaceholderDiagnostic> = emptyList(),
)

data class FoxDeclarationLoweringReport(
    val typeAliases: List<FoxTypeAliasPlaceholder>,
    val methods: List<FoxLoweredMethodSignature>,
    val diagnostics: List<FoxPlaceholderDiagnostic>,
)

data class FoxTypeAliasPlaceholder(
    val name: String,
    val generics: Set<String>,
    val alias: FoxType,
)

data class FoxMethodPlaceholder(
    val name: String,
    val generics: Map<String, FoxGenericConstraint>,
    val thisType: FoxType?,
    val parameters: List<FoxMethodParameterPlaceholder>,
    val normalizedParameters: NormalizationResult<List<NormalizedMethodParameter>>?,
    val returnType: FoxType?,
    val placeholders: List<FoxSemanticPlaceholder>,
)

sealed interface FoxMethodParameterPlaceholder

data class FoxNamedMethodParameterPlaceholder(
    val name: String,
    val type: FoxType,
) : FoxMethodParameterPlaceholder

data class FoxSplatMethodParameterPlaceholder(
    val type: FoxType,
) : FoxMethodParameterPlaceholder

data class FoxPlaceholderDiagnostic(
    val message: String,
)

data class FoxCallSitePlaceholder(
    val kind: FoxCallSiteKind,
    val targetName: String,
    val targetType: FoxType?,
    val argumentShape: FoxArgumentShape,
    val arguments: List<FoxCallArgumentPlaceholder>,
)

enum class FoxCallSiteKind {
    METHOD_CALL,
    CONSTRUCT,
}

enum class FoxArgumentShape {
    POSITIONAL,
    NAMED,
    MIXED,
    EMPTY,
}

data class FoxCallArgumentPlaceholder(
    val name: String?,
    val source: String,
)

data class FoxLoweredMethodSignature(
    val signature: FoxMethodSignature,
)

fun NodeFile.collectSemanticPlaceholders(): FoxSemanticPlaceholderReport {
    val typeAliases = mutableListOf<FoxTypeAliasPlaceholder>()
    val methods = mutableListOf<FoxMethodPlaceholder>()
    val callSites = mutableListOf<FoxCallSitePlaceholder>()
    val diagnostics = mutableListOf<FoxPlaceholderDiagnostic>()
    for (element in elements) {
        when (element) {
            is NodeTypeAlias -> {
                typeAliases += FoxTypeAliasPlaceholder(
                    name = element.name,
                    generics = element.generics?.toSet().orEmpty(),
                    alias = element.alias.toPlaceholderFoxType(diagnostics),
                )
            }
            is NodeMethodDefinition -> {
                methods += element.toPlaceholderMethod(callSites, diagnostics)
            }
        }
    }
    return FoxSemanticPlaceholderReport(typeAliases, methods, callSites, diagnostics)
}

fun NodeFile.lowerDeclarations(
    isInline: Boolean = false,
): FoxDeclarationLoweringReport {
    val placeholders = collectSemanticPlaceholders()
    val loweredMethods = mutableListOf<FoxLoweredMethodSignature>()
    val diagnostics = placeholders.diagnostics.toMutableList()
    placeholders.methods.forEach { method ->
        when (val lowered = method.toMethodSignature(isInline)) {
            is Normalized -> loweredMethods += lowered.value
            is NormalizationError -> diagnostics += FoxPlaceholderDiagnostic(
                "Method '${method.name}' could not be lowered: ${lowered.message}",
            )
        }
    }
    return FoxDeclarationLoweringReport(
        typeAliases = placeholders.typeAliases,
        methods = loweredMethods,
        diagnostics = diagnostics,
    )
}

fun NodeFile.lowerMethodDeclaration(
    methodName: String,
    genericReplacements: SequencedMap<String, FoxConcreteType>,
    isInline: Boolean = false,
): NormalizationResult<FoxLoweredMethodSignature> {
    val placeholders = collectSemanticPlaceholders()
    val matched = placeholders.methods.filter { it.name == methodName }
    if (matched.isEmpty()) return NormalizationError("Method '$methodName' not found")
    if (matched.size > 1) return NormalizationError("Multiple method placeholders named '$methodName' found")
    return matched.single().toMethodSignature(genericReplacements, isInline)
}

private fun NodeMethodDefinition.toPlaceholderMethod(
    callSites: MutableList<FoxCallSitePlaceholder>,
    diagnostics: MutableList<FoxPlaceholderDiagnostic>,
): FoxMethodPlaceholder {
    val placeholders = mutableListOf<FoxSemanticPlaceholder>()
    collectStatementPlaceholders(body, placeholders, callSites, diagnostics)
    val loweredParameters = parameters.map { parameter ->
        when (parameter) {
            is NodeNamedFormalParameter -> FoxNamedMethodParameterPlaceholder(
                name = parameter.name,
                type = parameter.type.toPlaceholderFoxType(diagnostics),
            )
            is NodeSplatFormalParameter -> {
                val type = parameter.type.toPlaceholderFoxType(diagnostics)
                placeholders += FoxSplatParameterPlaceholder(type)
                diagnostics += FoxPlaceholderDiagnostic(
                    "Splat parameter '*${parameter.type.toSource()}' requires structural expansion and is not lowered yet",
                )
                FoxSplatMethodParameterPlaceholder(type)
            }
        }
    }
    return FoxMethodPlaceholder(
        name = name,
        generics = generics.orEmpty().mapValues { (_, constraint) -> constraint.toPlaceholderConstraint(diagnostics) },
        thisType = thisType?.toPlaceholderFoxType(diagnostics),
        parameters = loweredParameters,
        normalizedParameters = normalizeMethodParameters(loweredParameters),
        returnType = returnType?.toPlaceholderFoxType(diagnostics),
        placeholders = placeholders,
    )
}

fun normalizeMethodParameters(
    parameters: List<FoxMethodParameterPlaceholder>,
): NormalizationResult<List<NormalizedMethodParameter>> {
    val result = mutableListOf<NormalizedMethodParameter>()
    val names = LinkedHashSet<String>()
    for (parameter in parameters) {
        when (parameter) {
            is FoxNamedMethodParameterPlaceholder -> {
                if (!names.add(parameter.name)) {
                    return NormalizationError("Duplicate method parameter '${parameter.name}' after expansion")
                }
                result += NormalizedMethodParameter(parameter.name, parameter.type)
            }
            is FoxSplatMethodParameterPlaceholder -> {
                when (val row = parameter.type.asParameterRow()) {
                    is Normalized -> {
                        for (expanded in row.value) {
                            if (!names.add(expanded.name)) {
                                return NormalizationError("Duplicate method parameter '${expanded.name}' after expansion")
                            }
                            result += expanded
                        }
                    }
                    is NormalizationError -> return row
                }
            }
        }
    }
    return Normalized(result)
}

fun FoxMethodPlaceholder.toMethodSignature(
    isInline: Boolean = false,
): NormalizationResult<FoxLoweredMethodSignature> {
    val normalizedGenerics = LinkedHashMap<String, FoxGenericConstraint>()
    for ((name, constraint) in generics) {
        when (val normalized = constraint.normalizeConstraint()) {
            is Normalized -> normalizedGenerics[name] = normalized.value
            is NormalizationError -> return normalized
        }
    }
    
    val normalizedThisType = when (val thisType = thisType) {
        null -> null
        else -> when (val normalized = thisType.normalizeType()) {
            is Normalized -> normalized.value
            is NormalizationError -> return normalized
        }
    }
    
    val normalizedParameters = when (val normalized = normalizedParameters ?: normalizeMethodParameters(parameters)) {
        is Normalized -> normalized.value
        is NormalizationError -> return normalized
    }
    
    val loweredParameters = LinkedHashMap<String, FoxType>()
    normalizedParameters.forEach { parameter ->
        loweredParameters[parameter.name] = parameter.type
    }
    
    val normalizedReturnType = when (val returnType = returnType) {
        null -> FoxUnitType
        else -> when (val normalized = returnType.normalizeType()) {
            is Normalized -> normalized.value
            is NormalizationError -> return normalized
        }
    }
    
    return Normalized(
        FoxLoweredMethodSignature(
            FoxMethodSignature(
                name = name,
                generics = normalizedGenerics,
                thisType = normalizedThisType ?: FoxUnitType,
                parameters = loweredParameters,
                returnType = normalizedReturnType,
                isInline = isInline,
            ),
        ),
    )
}

fun FoxMethodPlaceholder.toMethodSignature(
    genericReplacements: SequencedMap<String, FoxConcreteType>,
    isInline: Boolean = false,
): NormalizationResult<FoxLoweredMethodSignature> {
    val instantiated = instantiate(genericReplacements)
    for ((genericName, constraint) in generics) {
        genericReplacements[genericName]?.let { concrete ->
            if (!constraint.isSatisfiedBy(concrete)) {
                return NormalizationError(
                    "Generic argument '$genericName = $concrete' does not satisfy constraint '$constraint' for method '${this.name}'",
                )
            }
        }
    }
    return instantiated.toMethodSignature(isInline)
}

fun FoxMethodPlaceholder.instantiate(
    genericReplacements: SequencedMap<String, FoxConcreteType>,
): FoxMethodPlaceholder = FoxMethodPlaceholder(
    name = name,
    generics = generics.mapValues { (_, constraint) ->
        when (constraint) {
            is FoxExactMatchConstraint -> {
                val normalized = constraint.type.replaceGenerics(genericReplacements)
                if (normalized is FoxConcreteType) FoxExactMatchConstraint(normalized) else constraint
            }
            else -> constraint
        }
    },
    thisType = thisType?.replaceGenerics(genericReplacements),
    parameters = parameters.map { parameter ->
        when (parameter) {
            is FoxNamedMethodParameterPlaceholder -> FoxNamedMethodParameterPlaceholder(
                parameter.name,
                parameter.type.replaceGenerics(genericReplacements),
            )
            is FoxSplatMethodParameterPlaceholder -> FoxSplatMethodParameterPlaceholder(
                parameter.type.replaceGenerics(genericReplacements),
            )
        }
    },
    normalizedParameters = null,
    returnType = returnType?.replaceGenerics(genericReplacements),
    placeholders = placeholders.map { placeholder ->
        when (placeholder) {
            is FoxSplatParameterPlaceholder -> FoxSplatParameterPlaceholder(
                placeholder.type.replaceGenerics(genericReplacements),
            )
            is FoxGenForPlaceholder -> FoxGenForPlaceholder(
                placeholder.valueName,
                placeholder.typeName,
                placeholder.targetType.replaceGenerics(genericReplacements),
            )
        }
    },
)

private fun collectStatementPlaceholders(
    statement: NodeStatement,
    placeholders: MutableList<FoxSemanticPlaceholder>,
    callSites: MutableList<FoxCallSitePlaceholder>,
    diagnostics: MutableList<FoxPlaceholderDiagnostic>,
) {
    when (statement) {
        is NodeBlock -> statement.statements.forEach { collectStatementPlaceholders(it, placeholders, callSites, diagnostics) }
        is NodeIf -> {
            collectStatementPlaceholders(statement.thenBody, placeholders, callSites, diagnostics)
            statement.elseBody?.let { collectStatementPlaceholders(it, placeholders, callSites, diagnostics) }
        }
        is NodeWhen -> statement.cases.forEach { collectStatementPlaceholders(it.body, placeholders, callSites, diagnostics) }
        is NodeWhile -> collectStatementPlaceholders(statement.body, placeholders, callSites, diagnostics)
        is NodeDoWhile -> collectStatementPlaceholders(statement.body, placeholders, callSites, diagnostics)
        is NodeCall -> callSites += statement.toPlaceholderCallSite(diagnostics)
        is NodeConstruct -> callSites += statement.toPlaceholderCallSite(diagnostics)
        is NodeGenFor -> {
            val targetType = statement.targetType.toPlaceholderFoxType(diagnostics)
            placeholders += FoxGenForPlaceholder(
                valueName = statement.valueName,
                typeName = statement.typeName,
                targetType = targetType,
            )
            diagnostics += FoxPlaceholderDiagnostic(
                "genfor over '${statement.targetType.toSource()}' requires compile-time structural expansion and is not lowered yet",
            )
            collectStatementPlaceholders(statement.body, placeholders, callSites, diagnostics)
        }
        else -> {}
    }
}

private fun NodeCall.toPlaceholderCallSite(
    diagnostics: MutableList<FoxPlaceholderDiagnostic>,
): FoxCallSitePlaceholder = FoxCallSitePlaceholder(
    kind = FoxCallSiteKind.METHOD_CALL,
    targetName = name,
    targetType = null,
    argumentShape = parameters.argumentShape(diagnostics),
    arguments = parameters.map { (argName, value) -> FoxCallArgumentPlaceholder(argName, value.toSource()) },
)

private fun NodeConstruct.toPlaceholderCallSite(
    diagnostics: MutableList<FoxPlaceholderDiagnostic>,
): FoxCallSitePlaceholder = FoxCallSitePlaceholder(
    kind = FoxCallSiteKind.CONSTRUCT,
    targetName = type.toSource(),
    targetType = type.toPlaceholderFoxType(diagnostics),
    argumentShape = parameters.argumentShape(diagnostics),
    arguments = parameters.map { (argName, value) -> FoxCallArgumentPlaceholder(argName, value.toSource()) },
)

private fun List<Pair<String?, NodeStatement>>.argumentShape(
    diagnostics: MutableList<FoxPlaceholderDiagnostic>,
): FoxArgumentShape {
    if (isEmpty()) return FoxArgumentShape.EMPTY
    val hasNamed = any { it.first != null }
    val hasPositional = any { it.first == null }
    return when {
        hasNamed && hasPositional -> {
            diagnostics += FoxPlaceholderDiagnostic(
                "Mixed positional and named arguments are not lowered yet; keep call arguments fully positional or fully named",
            )
            FoxArgumentShape.MIXED
        }
        hasNamed -> FoxArgumentShape.NAMED
        else -> FoxArgumentShape.POSITIONAL
    }
}

private fun NodeGenericConstraint.toPlaceholderConstraint(
    diagnostics: MutableList<FoxPlaceholderDiagnostic>,
): FoxGenericConstraint = when (val match = match) {
    null -> FoxAnyConstraint
    is NodeStructWildcardType -> FoxStructWildcardConstraint
    else -> {
        val lowered = match.toPlaceholderFoxType(diagnostics)
        if (lowered is FoxConcreteType) FoxExactMatchConstraint(lowered)
        else {
            diagnostics += FoxPlaceholderDiagnostic(
                "Generic constraint '${match.toSource()}' is not a concrete exact-match type in placeholder lowering",
            )
            FoxAnyConstraint
        }
    }
}

private fun NodeType.toPlaceholderFoxType(
    diagnostics: MutableList<FoxPlaceholderDiagnostic>,
): FoxType = when (this) {
    is NodePrimitiveType -> type
    is NodeNamedType -> {
        if (generics.isEmpty()) FoxGenericType(name)
        else {
            diagnostics += FoxPlaceholderDiagnostic(
                "Named type '${toSource()}' requires alias/type resolution and is kept opaque in placeholder lowering",
            )
            FoxGenericType(name)
        }
    }
    is NodeArrayType -> FoxGenericArrayType(elementType.toPlaceholderFoxType(diagnostics))
    is NodeTupleType -> FoxTupleTemplateType(
        items.map { item ->
            when (item) {
                is NodeTupleTypeItem -> FoxTupleTypeTemplateItem(item.type.toPlaceholderFoxType(diagnostics))
                is NodeTupleSpreadItem -> FoxTupleSpreadTemplateItem(item.type.toPlaceholderFoxType(diagnostics))
            }
        },
    )
    is NodeNamedProjectionType -> FoxNamedProjectionType(baseType.toPlaceholderFoxType(diagnostics))
    is NodeStructWildcardType -> {
        diagnostics += FoxPlaceholderDiagnostic(
            "Standalone type '${toSource()}' is only supported as a generic constraint in placeholder lowering",
        )
        FoxGenericStructType(emptyMap())
    }
    is NodeStructType -> FoxStructTemplateType(
        items.map { item ->
            when (item) {
                is NodeStructFieldItem -> FoxStructFieldTemplateItem(item.name, item.type.toPlaceholderFoxType(diagnostics))
                is NodeStructSpreadItem -> FoxStructSpreadTemplateItem(item.type.toPlaceholderFoxType(diagnostics))
            }
        },
    )
    is NodeDenamedProjectionType -> FoxDenamedProjectionType(baseType.toPlaceholderFoxType(diagnostics))
    is NodeEnumType -> FoxGenericEnumType(items.mapValues { (_, value) -> value.toPlaceholderFoxType(diagnostics) })
    is NodeRefType -> FoxGenericRefType(referentType.toPlaceholderFoxType(diagnostics))
    is NodeLambdaType -> FoxGenericLambdaType(
        thisType = thisType.toPlaceholderFoxType(diagnostics),
        parameters = parameters.map { it.toPlaceholderFoxType(diagnostics) },
        returnType = returnType.toPlaceholderFoxType(diagnostics),
    )
}
