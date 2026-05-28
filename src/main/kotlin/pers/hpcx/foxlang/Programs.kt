package pers.hpcx.foxlang

fun generateMethods(
    methods: Map<FoxMethodSignature, FoxMethodImplementation>,
    identifier: FoxMethodIdentifier,
    result: MutableMap<FoxMethodIdentifier, Pair<FoxMethodSignature, FoxMethodImplementation>> = mutableMapOf(),
) {
    if (identifier in result) return
    
    val filteredMethods = methods.filterKeys {
        it.name == identifier.name &&
            it.generics.keys == identifier.generics.keys &&
            it.parameters.keys == identifier.parameters.keys
    }
    
    val thisType = identifier.thisType as FoxConcreteType
    val generics = identifier.generics.mapValues { it.value as FoxConcreteType }
    val parameters = identifier.parameters.mapValues { it.value as FoxConcreteType }
    
    val filteredMethods2 = filteredMethods.filterKeys {
        it.generics.all { (name, constraint) -> constraint.isSatisfiedBy(generics.getValue(name)) } &&
            it.thisType.replaceGenerics(generics) == thisType &&
            it.parameters.all { (name, type) -> type.replaceGenerics(generics) == parameters.getValue(name) }
    }
    
    if (filteredMethods2.isEmpty()) error("No method found for $identifier")
    if (filteredMethods2.size > 1) error("Multiple methods found for $identifier")
    
    val signature = filteredMethods2.keys.first()
    val implementation = filteredMethods2.values.first()
    val newSignature = signature.replaceGenerics(generics)
    val newImplementation = implementation.replaceGenerics(generics)
    result[identifier] = newSignature to newImplementation
    
    newImplementation.dependencies().forEach { generateMethods(methods, it, result) }
}

fun FoxGenericConstraint.isSatisfiedBy(type: FoxConcreteType): Boolean = when (this) {
    is FoxAnyConstraint -> true
    is FoxExactMatchConstraint -> type == this.type
}

fun FoxMethodSignature.replaceGenerics(replacements: Map<String, FoxConcreteType>) = FoxMethodSignature(
    name = name,
    generics = generics.mapValues { FoxExactMatchConstraint(replacements.getValue(it.key)) },
    thisType = thisType.replaceGenerics(replacements),
    parameters = parameters.mapValues { it.value.replaceGenerics(replacements) },
    returnType = returnType.replaceGenerics(replacements),
    isInline = isInline,
)

fun FoxMethodImplementation.replaceGenerics(replacements: Map<String, FoxConcreteType>) = when (this) {
    is FoxNativeMethodImplementation -> this
    is FoxBuiltInMethodImplementation -> this
    is FoxCustomizedMethodImplementation -> FoxCustomizedMethodImplementation(
        startBlock = startBlock,
        blocks = blocks.mapValues { block ->
            FoxInstBlock(
                instructions = block.value.instructions.map { inst -> inst.replaceGenerics(replacements) },
                jump = block.value.jump,
            )
        },
    )
}

fun FoxInst.replaceGenerics(replacements: Map<String, FoxConcreteType>) = when (this) {
    is InstLoad -> this
    is InstCopy -> this
    is InstCall -> InstCall(
        target = target,
        params = params,
        method = method.replaceGenerics(replacements),
    )
    is InstLambdaCall -> this
}

fun FoxMethodIdentifier.replaceGenerics(replacements: Map<String, FoxConcreteType>) = FoxMethodIdentifier(
    name = name,
    generics = generics.mapValues { it.value.replaceGenerics(replacements) },
    thisType = thisType.replaceGenerics(replacements),
    parameters = parameters.mapValues { it.value.replaceGenerics(replacements) },
)

fun FoxMethodImplementation.dependencies(result: MutableSet<FoxMethodIdentifier> = mutableSetOf()): Set<FoxMethodIdentifier> {
    when (this) {
        is FoxNativeMethodImplementation -> {}
        is FoxBuiltInMethodImplementation -> {}
        is FoxCustomizedMethodImplementation -> {
            blocks.values.forEach { block ->
                block.instructions.forEach { inst ->
                    when (inst) {
                        is InstLoad -> {}
                        is InstCopy -> {}
                        is InstCall -> result += inst.method
                        is InstLambdaCall -> {}
                    }
                }
            }
        }
    }
    return result
}
