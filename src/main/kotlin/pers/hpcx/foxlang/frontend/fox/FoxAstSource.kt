package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.ir.*
import pers.hpcx.foxlang.runtime.*
import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet
import java.io.PrintWriter

data class AstSourceOptions(
    val indent: String = "    ",
    val style: AstSourceStyle = AstSourceStyle.DEFAULT,
    val compact: Boolean = false,
)

enum class AstSourceStyle {
    DEFAULT,
    CANONICAL,
}

class AstSourceContext(options: AstSourceOptions = AstSourceOptions()) {
    
    private val indentUnit = options.indent
    private val canonical = options.style == AstSourceStyle.CANONICAL
    private val fileSeparator = if (options.compact) "\n" else "\n\n"
    
    fun PrintWriter.printFile(file: SurfaceFile) {
        file.elements.forEachIndexed { index, element ->
            if (index > 0) print(fileSeparator)
            printFileElement(element)
            print('\n')
        }
    }
    
    fun PrintWriter.printFileElement(element: SurfaceFileElement) {
        when (element) {
            is SurfaceTypeAlias -> printTypeAlias(element)
            is SurfaceMethodDefinition -> printMethodDefinition(element)
        }
    }
    
    fun PrintWriter.printType(type: SurfaceType) {
        when (type) {
            is SurfacePrimitiveType -> printPrimitiveType(type)
            is SurfaceWildcardType -> printWildcardType(type)
            is SurfaceBuiltInType -> printBuiltInType(type)
            is SurfaceTransformType -> printTransformType(type)
            is SurfaceUnresolvedType -> printUnresolvedType(type)
            is SurfacePlaceholderType -> error("Placeholder type should not be printed")
        }
    }
    
    fun PrintWriter.printStatement(statement: SurfaceStatement) {
        printStatement(statement, 0, Standalone)
    }
    
    fun PrintWriter.printTypeAlias(element: SurfaceTypeAlias) {
        print("type ")
        print(element.name)
        printTypeParameterNames(element.generics)
        print(" = ")
        printType(element.alias)
    }
    
    fun PrintWriter.printMethodDefinition(element: SurfaceMethodDefinition) {
        print("def ")
        if (canonical || element.generics.isNotEmpty()) {
            printFormalGenerics(element.generics)
            print(' ')
        }
        if (canonical || element.thisType.isNotUnit()) {
            printType(element.thisType)
            print('.')
        }
        print(element.name)
        printFormalParameters(element.parameters)
        if (canonical || element.returnType.isNotUnit()) {
            print(": ")
            printType(element.returnType)
        }
        print(' ')
        printMethodBody(element.body)
    }
    
    private fun SurfaceType.isNotUnit() = this !is SurfacePrimitiveType || type != PrimitiveTypeEnum.Unit
    
    private fun PrintWriter.printPrimitiveType(type: SurfacePrimitiveType) = when (type.type) {
        PrimitiveTypeEnum.Void -> print("Void")
        PrimitiveTypeEnum.Unit -> print("Unit")
        PrimitiveTypeEnum.Bool -> print("Bool")
        PrimitiveTypeEnum.Byte -> print("Byte")
        PrimitiveTypeEnum.Short -> print("Short")
        PrimitiveTypeEnum.Int -> print("Int")
        PrimitiveTypeEnum.Long -> print("Long")
        PrimitiveTypeEnum.Float -> print("Float")
        PrimitiveTypeEnum.Double -> print("Double")
        PrimitiveTypeEnum.Char -> print("Char")
        PrimitiveTypeEnum.String -> print("String")
    }
    
    private fun PrintWriter.printWildcardType(type: SurfaceWildcardType) = when (type) {
        is SurfaceAnyType -> print("Any")
        is SurfaceAnyTupleType -> print("AnyTuple")
        is SurfaceAnyStructType -> print("AnyStruct")
        is SurfaceAnyObjectType -> print("AnyObject")
        is SurfaceAnyEnumType -> print("AnyEnum")
        is SurfaceAnyOfType -> printTypeListArgument("AnyOf", type.types)
        is SurfaceAllOfType -> printTypeListArgument("AllOf", type.types)
        is SurfaceNoneOfType -> printTypeListArgument("NoneOf", type.types)
        is SurfaceAnyTupleOfType -> printSingleTypeArgument("AnyTupleOf", type.component)
        is SurfaceAnyStructOfType -> printTypeListArgument("AnyStructOf", type.fields)
    }
    
    private fun PrintWriter.printBuiltInType(type: SurfaceBuiltInType) = when (type) {
        is SurfaceTupleType -> printTupleType(type)
        is SurfaceStructType -> printStructType(type)
        is SurfaceObjectType -> printObjectType(type)
        is SurfaceEnumType -> printEnumType(type)
        is SurfaceArrayType -> printSingleTypeArgument("Array", type.element)
        is SurfaceRefType -> printSingleTypeArgument("Ref", type.referent)
        is SurfaceMethodType -> printMethodType(type)
    }
    
    private fun PrintWriter.printTransformType(type: SurfaceTransformType) = when (type) {
        is SurfaceTupleGetComponentType -> printTypeAndIntArgument("GetComponent", type.type, type.index)
        is SurfaceTupleGetComponentBackType -> printTypeAndIntArgument("GetComponentBack", type.type, type.index)
        is SurfaceTupleGetFirstComponentsType -> printTypeAndIntArgument("GetFirstComponents", type.type, type.count)
        is SurfaceTupleGetFirstComponentsExactType -> printTypeAndIntArgument("GetFirstComponentsExact", type.type, type.count)
        is SurfaceTupleGetLastComponentsType -> printTypeAndIntArgument("GetLastComponents", type.type, type.count)
        is SurfaceTupleGetLastComponentsExactType -> printTypeAndIntArgument("GetLastComponentsExact", type.type, type.count)
        is SurfaceTupleDropFirstComponentsType -> printTypeAndIntArgument("DropFirstComponents", type.type, type.count)
        is SurfaceTupleDropFirstComponentsExactType -> printTypeAndIntArgument("DropFirstComponentsExact", type.type, type.count)
        is SurfaceTupleDropLastComponentsType -> printTypeAndIntArgument("DropLastComponents", type.type, type.count)
        is SurfaceTupleDropLastComponentsExactType -> printTypeAndIntArgument("DropLastComponentsExact", type.type, type.count)
        is SurfaceTupleMergeTuplesType -> printTypeListArgument("MergeTuples", type.types)
        is SurfaceStructGetFieldTypeByNameType -> printTypeAndNameArgument("GetFieldTypeByName", type.type, type.name)
        is SurfaceStructGetFieldTypeByIndexType -> printTypeAndIntArgument("GetFieldTypeByIndex", type.type, type.index)
        is SurfaceStructGetFieldTypeByIndexBackType -> printTypeAndIntArgument("GetFieldTypeByIndexBack", type.type, type.index)
        is SurfaceStructGetFirstFieldsType -> printTypeAndIntArgument("GetFirstFields", type.type, type.count)
        is SurfaceStructGetFirstFieldsExactType -> printTypeAndIntArgument("GetFirstFieldsExact", type.type, type.count)
        is SurfaceStructGetLastFieldsType -> printTypeAndIntArgument("GetLastFields", type.type, type.count)
        is SurfaceStructGetLastFieldsExactType -> printTypeAndIntArgument("GetLastFieldsExact", type.type, type.count)
        is SurfaceStructDropFirstFieldsType -> printTypeAndIntArgument("DropFirstFields", type.type, type.count)
        is SurfaceStructDropFirstFieldsExactType -> printTypeAndIntArgument("DropFirstFieldsExact", type.type, type.count)
        is SurfaceStructDropLastFieldsType -> printTypeAndIntArgument("DropLastFields", type.type, type.count)
        is SurfaceStructDropLastFieldsExactType -> printTypeAndIntArgument("DropLastFieldsExact", type.type, type.count)
        is SurfaceStructSelectFieldsType -> printTypeAndNamesArgument("SelectFields", type.type, type.names)
        is SurfaceStructSelectFieldsExactType -> printTypeAndNamesArgument("SelectFieldsExact", type.type, type.names)
        is SurfaceStructDropFieldsType -> printTypeAndNamesArgument("DropFields", type.type, type.names)
        is SurfaceStructDropFieldsExactType -> printTypeAndNamesArgument("DropFieldsExact", type.type, type.names)
        is SurfaceStructExtractFieldTypesType -> printSingleTypeArgument("ExtractFieldTypes", type.type)
        is SurfaceStructMergeStructsType -> printTypeListArgument("MergeStructs", type.types)
        is SurfaceObjectGetMemberTypeType -> printTypeAndNameArgument("GetMemberType", type.type, type.name)
        is SurfaceObjectSelectMembersType -> printTypeAndNamesArgument("SelectMembers", type.type, type.names)
        is SurfaceObjectSelectMembersExactType -> printTypeAndNamesArgument("SelectMembersExact", type.type, type.names)
        is SurfaceObjectDropMembersType -> printTypeAndNamesArgument("DropMembers", type.type, type.names)
        is SurfaceObjectDropMembersExactType -> printTypeAndNamesArgument("DropMembersExact", type.type, type.names)
        is SurfaceObjectMergeObjectsType -> printTypeListArgument("MergeObjects", type.types)
        is SurfaceEnumGetEntryTypeType -> printTypeAndNameArgument("GetEntryType", type.type, type.name)
        is SurfaceEnumSelectEntriesType -> printTypeAndNamesArgument("SelectEntries", type.type, type.names)
        is SurfaceEnumSelectEntriesExactType -> printTypeAndNamesArgument("SelectEntriesExact", type.type, type.names)
        is SurfaceEnumDropEntriesType -> printTypeAndNamesArgument("DropEntries", type.type, type.names)
        is SurfaceEnumDropEntriesExactType -> printTypeAndNamesArgument("DropEntriesExact", type.type, type.names)
        is SurfaceEnumMergeEnumsType -> printTypeListArgument("MergeEnums", type.types)
        is SurfaceArrayGetElementTypeType -> printSingleTypeArgument("GetElementType", type.type)
        is SurfaceRefGetReferentTypeType -> printSingleTypeArgument("GetReferentType", type.type)
        is SurfaceMethodGetThisTypeType -> printSingleTypeArgument("GetThisType", type.type)
        is SurfaceMethodGetParameterStructType -> printSingleTypeArgument("GetParameterStruct", type.type)
        is SurfaceMethodGetReturnTypeType -> printSingleTypeArgument("GetReturnType", type.type)
        is SurfaceMethodOfType -> printTypeListArgument("MethodOf", listOf(type.`this`, type.parameters, type.`return`))
    }
    
    private fun PrintWriter.printTupleType(type: SurfaceTupleType) {
        print("Tuple<")
        printCommaSeparated(type.components) { component ->
            printType(component)
        }
        print('>')
    }
    
    private fun PrintWriter.printStructType(type: SurfaceStructType) {
        print("Struct<")
        printCommaSeparated(type.fields.entries) { field ->
            print(field.key)
            print(": ")
            printType(field.value)
        }
        print('>')
    }
    
    private fun PrintWriter.printObjectType(type: SurfaceObjectType) {
        print("Object<")
        printCommaSeparated(type.members.entries) { member ->
            print(member.key)
            print(": ")
            printType(member.value)
        }
        print('>')
    }
    
    private fun PrintWriter.printEnumType(type: SurfaceEnumType) {
        print("Enum<")
        printCommaSeparated(type.entries.entries) { entry ->
            print(entry.key)
            print(" = ")
            printType(entry.value)
        }
        print('>')
    }
    
    private fun PrintWriter.printMethodType(type: SurfaceMethodType) {
        print("Method<")
        print("this")
        print(": ")
        printType(type.`this`)
        print(", ")
        type.parameters.forEach {
            print(it.key)
            print(": ")
            printType(it.value)
            print(", ")
        }
        print("return")
        print(": ")
        printType(type.`return`)
        print('>')
    }
    
    private fun PrintWriter.printUnresolvedType(type: SurfaceUnresolvedType) {
        print(type.name)
        type.parameters?.let { parameters ->
            print('<')
            printCommaSeparated(parameters) { parameter ->
                printType(parameter)
            }
            print('>')
        }
    }
    
    private fun PrintWriter.printSingleTypeArgument(keyword: String, type: SurfaceType) {
        print(keyword)
        print('<')
        printType(type)
        print('>')
    }
    
    private fun PrintWriter.printTypeAndIntArgument(keyword: String, type: SurfaceType, value: Int) {
        print(keyword)
        print('<')
        printType(type)
        print(", ")
        print(value)
        print('>')
    }
    
    private fun PrintWriter.printTypeAndNameArgument(keyword: String, type: SurfaceType, name: String) {
        print(keyword)
        print('<')
        printType(type)
        print(", ")
        print(name)
        print('>')
    }
    
    private fun PrintWriter.printTypeAndNamesArgument(keyword: String, type: SurfaceType, names: Iterable<String>) {
        print(keyword)
        print('<')
        printType(type)
        print(", ")
        printCommaSeparated(names) { name ->
            print(name)
        }
        print('>')
    }
    
    private fun PrintWriter.printTypeListArgument(keyword: String, types: Iterable<SurfaceType>) {
        print(keyword)
        print('<')
        printCommaSeparated(types) { type ->
            printType(type)
        }
        print('>')
    }
    
    private fun PrintWriter.printStatement(statement: SurfaceStatement, indentLevel: Int, position: StatementPosition) {
        val precedence = precedenceOf(statement)
        if (needsParentheses(statement, precedence, position)) print('(')
        printRawStatement(statement, indentLevel)
        if (needsParentheses(statement, precedence, position)) print(')')
    }
    
    private fun PrintWriter.printRawStatement(statement: SurfaceStatement, indentLevel: Int) {
        when (statement) {
            SurfaceThis -> print("this")
            is SurfaceUnresolvedSymbol -> print(statement.name)
            is SurfaceEntityStatement -> printEntity(statement.value)
            is SurfaceFormattedString -> printFormattedString(statement)
            is SurfaceLambda -> printLambda(statement, indentLevel)
            is SurfaceBlock -> printBlock(statement, indentLevel)
            is SurfaceUnary -> printUnary(statement, indentLevel)
            is SurfaceBinary -> printBinary(statement, indentLevel)
            is SurfaceAssign -> printAssign(statement, indentLevel)
            is SurfaceTypeBinding -> {
                print(statement.name)
                print(": ")
                printType(statement.type)
            }
            is SurfaceFieldAccess -> {
                printStatement(statement.target, indentLevel, PostfixTarget)
                print('.')
                print(statement.name)
            }
            is SurfaceIndexAccess -> {
                printStatement(statement.target, indentLevel, PostfixTarget)
                print('[')
                printCommaSeparated(statement.indices) {
                    printStatement(it, indentLevel, Standalone)
                }
                print(']')
            }
            is SurfaceCall -> printCall(statement, indentLevel)
            is SurfaceConstruct -> {
                printType(statement.type)
                printActualParametersWithTrailingLambda(statement.parameters, indentLevel)
            }
            is SurfaceIndirectCall -> printIndirectCall(statement, indentLevel)
            is SurfaceIf -> printIf(statement, indentLevel)
            is SurfaceWhen -> printWhen(statement, indentLevel)
            is SurfaceWhile -> printWhile(statement, indentLevel)
            is SurfaceDoWhile -> printDoWhile(statement, indentLevel)
            is SurfaceBreak -> {
                print("break")
                statement.label?.let {
                    print(" #")
                    print(it)
                }
            }
            is SurfaceContinue -> {
                print("continue")
                statement.label?.let {
                    print(" #")
                    print(it)
                }
            }
            is SurfaceYield -> {
                print("yield")
                statement.label?.let {
                    print(" #")
                    print(it)
                }
                print(' ')
                printStatement(statement.value, indentLevel, Standalone)
            }
            is SurfaceReturn -> {
                print("return")
                statement.value?.let {
                    print(' ')
                    printStatement(it, indentLevel, Standalone)
                }
            }
        }
    }
    
    private fun PrintWriter.printBlock(block: SurfaceBlock, indentLevel: Int) {
        printLabelPrefix(block.label)
        if (block.statements.isEmpty()) {
            print("{}")
            return
        }
        print("{\n")
        block.statements.forEachIndexed { index, statement ->
            print(indent(indentLevel + 1))
            printStatement(statement, indentLevel + 1, Standalone)
            if (index != block.statements.lastIndex) print('\n')
        }
        print('\n')
        print(indent(indentLevel))
        print('}')
    }
    
    private fun PrintWriter.printUnary(statement: SurfaceUnary, indentLevel: Int) {
        print(unaryOperatorText(statement.operator))
        printStatement(statement.right, indentLevel, UnaryOperand)
    }
    
    private fun PrintWriter.printBinary(statement: SurfaceBinary, indentLevel: Int) {
        val precedence = binaryPrecedence(statement.operator)
        printStatement(statement.left, indentLevel, BinaryLeft(precedence))
        print(' ')
        print(binaryOperatorText(statement.operator))
        print(' ')
        printStatement(statement.right, indentLevel, BinaryRight(precedence))
    }
    
    private fun PrintWriter.printAssign(statement: SurfaceAssign, indentLevel: Int) {
        printStatement(statement.left, indentLevel, AssignLeft)
        print(' ')
        print(assignOperatorText(statement.operator))
        print(' ')
        printStatement(statement.right, indentLevel, AssignRight)
    }
    
    private fun PrintWriter.printCall(statement: SurfaceCall, indentLevel: Int) {
        if (canonical || statement.target != SurfaceEntityStatement(FoxUnit)) {
            printStatement(statement.target, indentLevel, PostfixTarget)
            print('.')
        }
        print(statement.name)
        statement.generics?.let { printActualTypeArguments(it) }
        printActualParametersWithTrailingLambda(statement.parameters, indentLevel)
    }
    
    private fun PrintWriter.printIndirectCall(statement: SurfaceIndirectCall, indentLevel: Int) {
        if (canonical || statement.target != SurfaceEntityStatement(FoxUnit)) {
            printStatement(statement.target, indentLevel, PostfixTarget)
            print('.')
        }
        print('(')
        printStatement(statement.method, indentLevel, Standalone)
        print(')')
        printActualParametersWithTrailingLambda(statement.parameters, indentLevel)
    }
    
    private fun PrintWriter.printIf(statement: SurfaceIf, indentLevel: Int) {
        printLabelPrefix(statement.label)
        print("if (")
        printStatement(statement.condition, indentLevel, Standalone)
        print(") ")
        printControlBody(statement.thenBody, indentLevel)
        statement.elseBody?.let {
            print(" else ")
            printControlBody(it, indentLevel, allowElseIfChain = true)
        }
    }
    
    private fun PrintWriter.printWhen(statement: SurfaceWhen, indentLevel: Int) {
        printLabelPrefix(statement.label)
        print("when")
        statement.value?.let {
            print(" (")
            printStatement(it, indentLevel, Standalone)
            print(')')
        }
        if (statement.cases.isEmpty()) {
            print(" {}")
            return
        }
        print(" {\n")
        statement.cases.forEachIndexed { index, case ->
            print(indent(indentLevel + 1))
            if (case.conditions == null) {
                print("else")
            } else {
                printCommaSeparated(case.conditions) { condition ->
                    printStatement(condition, indentLevel + 1, Standalone)
                }
            }
            print(" -> ")
            printControlBody(case.body, indentLevel + 1)
            if (index != statement.cases.lastIndex) print('\n')
        }
        print('\n')
        print(indent(indentLevel))
        print('}')
    }
    
    private fun PrintWriter.printWhile(statement: SurfaceWhile, indentLevel: Int) {
        printLabelPrefix(statement.label)
        print("while (")
        printStatement(statement.condition, indentLevel, Standalone)
        print(") ")
        printControlBody(statement.body, indentLevel)
    }
    
    private fun PrintWriter.printDoWhile(statement: SurfaceDoWhile, indentLevel: Int) {
        printLabelPrefix(statement.label)
        print("do ")
        printControlBody(statement.body, indentLevel)
        print(" while (")
        printStatement(statement.condition, indentLevel, Standalone)
        print(')')
    }
    
    private fun PrintWriter.printMethodBody(statement: SurfaceStatement) {
        if (statement is SurfaceBlock) printBlock(statement, 0)
        else printBlockBody(statement, 0)
    }
    
    private fun PrintWriter.printControlBody(statement: SurfaceStatement, indentLevel: Int, allowElseIfChain: Boolean = false) {
        when {
            canonical && statement is SurfaceIf && allowElseIfChain ->
                printStatement(statement, indentLevel, Standalone)
            
            canonical -> printBlockBody(statement, indentLevel)
            else -> printStatement(statement, indentLevel, Standalone)
        }
    }
    
    private fun PrintWriter.printBlockBody(statement: SurfaceStatement, indentLevel: Int) {
        if (statement is SurfaceBlock) {
            printBlock(statement, indentLevel)
            return
        }
        print("{\n")
        print(indent(indentLevel + 1))
        printStatement(statement, indentLevel + 1, Standalone)
        print('\n')
        print(indent(indentLevel))
        print('}')
    }
    
    private fun PrintWriter.printLabelPrefix(label: String?) {
        label?.let {
            print('#')
            print(it)
            print(' ')
        }
    }
    
    private fun PrintWriter.printTypeParameterNames(generics: OrderedSet<String>) {
        if (canonical || generics.isNotEmpty()) {
            print('<')
            printCommaSeparated(generics) { name -> print(name) }
            print('>')
        }
    }
    
    private fun PrintWriter.printFormalGenerics(generics: OrderedMap<String, SurfaceType>) {
        print('<')
        printCommaSeparated(generics.entries) { (name, pattern) ->
            print(name)
            print(" = ")
            printType(pattern)
        }
        print('>')
    }
    
    private fun PrintWriter.printFormalParameters(parameters: OrderedMap<String, SurfaceType>) {
        print('(')
        printCommaSeparated(parameters.entries) { parameter ->
            print(parameter.key)
            print(": ")
            printType(parameter.value)
        }
        print(')')
    }
    
    private fun PrintWriter.printActualParameters(parameters: List<Pair<String?, SurfaceStatement>>, indentLevel: Int) {
        print('(')
        printCommaSeparated(parameters) { (name, value) ->
            if (name != null) {
                print(name)
                print(" = ")
            }
            printStatement(value, indentLevel, Standalone)
        }
        print(')')
    }
    
    private fun PrintWriter.printActualParametersWithTrailingLambda(
        parameters: List<Pair<String?, SurfaceStatement>>,
        indentLevel: Int,
    ) {
        val trailingLambda = parameters.lastOrNull()
            ?.takeIf { !canonical && it.first == null && it.second is SurfaceLambda }
            ?.second as SurfaceLambda?
        if (trailingLambda == null) {
            printActualParameters(parameters, indentLevel)
            return
        }
        
        val regularParameters = parameters.dropLast(1)
        if (regularParameters.isNotEmpty()) {
            printActualParameters(regularParameters, indentLevel)
            print(' ')
        } else {
            print(' ')
        }
        printLambda(trailingLambda, indentLevel)
    }
    
    private fun PrintWriter.printActualTypeArguments(generics: List<Pair<String?, SurfaceType>>) {
        if (generics.isEmpty()) return
        print('<')
        printCommaSeparated(generics) { (name, type) ->
            if (name != null) {
                print(name)
                print(" = ")
            }
            printType(type)
        }
        print('>')
    }
    
    private fun PrintWriter.printFormattedString(statement: SurfaceFormattedString) {
        val isRaw = false
        print(if (isRaw) "rf\"" else "f\"")
        statement.parts.forEach { part ->
            when (part) {
                is FoxFormattedText ->
                    print(if (isRaw) escapeRawFormattedText(part.text) else escapeFormattedText(part.text))
                
                is FoxFormattedExpression -> {
                    print('{')
                    printStatement(part.expression, 0, Standalone)
                    print('}')
                }
            }
        }
        print('"')
    }
    
    private fun PrintWriter.printLambda(statement: SurfaceLambda, indentLevel: Int) {
        print('{')
        statement.parameters?.let { parameters ->
            if (parameters.isNotEmpty()) {
                print(' ')
                printCommaSeparated(parameters) { (name, type) ->
                    print(name)
                    type?.let { type ->
                        print(": ")
                        printType(type)
                    }
                }
            }
        }
        print(" ->")
        when (val body = statement.body) {
            is SurfaceBlock -> {
                if (body.statements.isEmpty()) {
                    if (statement.parameters != null) print(' ')
                    print('}')
                    return
                }
                print('\n')
                body.statements.forEachIndexed { index, bodyStatement ->
                    print(indent(indentLevel + 1))
                    printStatement(bodyStatement, indentLevel + 1, Standalone)
                    if (index != body.statements.lastIndex) print('\n')
                }
                print('\n')
                print(indent(indentLevel))
            }
            else -> {
                print(' ')
                printStatement(body, indentLevel, Standalone)
                print(' ')
            }
        }
        print('}')
    }
    
    private fun PrintWriter.printEntity(entity: FoxEntity) {
        when (entity) {
            FoxUnit -> print("unit")
            is FoxBool -> print(entity.value)
            is FoxByte -> print(entity.value)
            is FoxShort -> print(entity.value)
            is FoxInt -> print(entity.value)
            is FoxLong -> {
                print(entity.value)
                print('L')
            }
            is FoxFloat -> {
                print(entity.value)
                print('f')
            }
            is FoxDouble -> print(entity.value)
            is FoxChar -> {
                print('\'')
                print(escapeChar(entity.value))
                print('\'')
            }
            is FoxString -> {
                print('"')
                print(escapeString(entity.value))
                print('"')
            }
            is FoxArray -> TODO()
            is FoxTuple -> TODO()
            is FoxStruct -> TODO()
            is FoxObject -> TODO()
            is FoxEnum -> TODO()
            is FoxRef -> TODO()
            is FoxMethod -> TODO()
        }
    }
    
    private fun unaryOperatorText(operator: FoxUnaryOperator) = when (operator.operator) {
        UnaryOperatorEnum.Not -> "!"
        UnaryOperatorEnum.Neg -> "-"
    }
    
    private fun binaryOperatorText(operator: FoxBinaryOperator) = when (operator.operator) {
        BinaryOperatorEnum.Add -> "+"
        BinaryOperatorEnum.Sub -> "-"
        BinaryOperatorEnum.Mul -> "*"
        BinaryOperatorEnum.Div -> "/"
        BinaryOperatorEnum.Rem -> "%"
        BinaryOperatorEnum.And -> "&"
        BinaryOperatorEnum.Or -> "|"
        BinaryOperatorEnum.Xor -> "^"
        BinaryOperatorEnum.Shl -> "<<"
        BinaryOperatorEnum.Shr -> ">>"
        BinaryOperatorEnum.Ushr -> ">>>"
        BinaryOperatorEnum.Eq -> "=="
        BinaryOperatorEnum.Neq -> "!="
        BinaryOperatorEnum.Lt -> "<"
        BinaryOperatorEnum.Gt -> ">"
        BinaryOperatorEnum.Leq -> "<="
        BinaryOperatorEnum.Geq -> ">="
        BinaryOperatorEnum.AndAnd -> "&&"
        BinaryOperatorEnum.OrOr -> "||"
    }
    
    private fun assignOperatorText(operator: FoxAssignOperator) = when (operator.operator) {
        AssignOperatorEnum.Plain -> "="
        AssignOperatorEnum.Def -> ":="
        AssignOperatorEnum.Add -> "+="
        AssignOperatorEnum.Sub -> "-="
        AssignOperatorEnum.Mul -> "*="
        AssignOperatorEnum.Div -> "/="
        AssignOperatorEnum.Rem -> "%="
        AssignOperatorEnum.And -> "&="
        AssignOperatorEnum.Or -> "|="
        AssignOperatorEnum.Xor -> "^="
        AssignOperatorEnum.Shl -> "<<="
        AssignOperatorEnum.Shr -> ">>="
        AssignOperatorEnum.Ushr -> ">>>="
        AssignOperatorEnum.AndAnd -> "&&="
        AssignOperatorEnum.OrOr -> "||="
    }
    
    private fun precedenceOf(statement: SurfaceStatement): Int = when (statement) {
        is SurfaceIf,
        is SurfaceWhen,
        is SurfaceWhile,
        is SurfaceDoWhile,
        is SurfaceBreak,
        is SurfaceContinue,
        is SurfaceYield,
        is SurfaceReturn,
        is SurfaceBlock,
        is SurfaceTypeBinding,
            -> 0
        
        is SurfaceLambda -> 5
        is SurfaceAssign -> 10
        is SurfaceBinary -> binaryPrecedence(statement.operator)
        is SurfaceUnary -> 120
        is SurfaceFieldAccess,
        is SurfaceIndexAccess,
        is SurfaceCall,
        is SurfaceConstruct,
        is SurfaceIndirectCall,
            -> 130
        
        SurfaceThis,
        is SurfaceUnresolvedSymbol,
        is SurfaceFormattedString,
        is SurfaceEntityStatement,
            -> 140
    }
    
    private fun binaryPrecedence(operator: FoxBinaryOperator): Int = when (operator.operator) {
        BinaryOperatorEnum.OrOr -> 20
        BinaryOperatorEnum.AndAnd -> 30
        BinaryOperatorEnum.Or -> 40
        BinaryOperatorEnum.Xor -> 50
        BinaryOperatorEnum.And -> 60
        BinaryOperatorEnum.Eq, BinaryOperatorEnum.Neq -> 70
        BinaryOperatorEnum.Lt, BinaryOperatorEnum.Gt, BinaryOperatorEnum.Leq, BinaryOperatorEnum.Geq -> 80
        BinaryOperatorEnum.Shl, BinaryOperatorEnum.Shr, BinaryOperatorEnum.Ushr -> 90
        BinaryOperatorEnum.Add, BinaryOperatorEnum.Sub -> 100
        BinaryOperatorEnum.Mul, BinaryOperatorEnum.Div, BinaryOperatorEnum.Rem -> 110
    }
    
    private fun needsParentheses(
        statement: SurfaceStatement,
        precedence: Int,
        position: StatementPosition,
    ): Boolean {
        val parentPrecedence = position.parentPrecedence
        if (precedence < parentPrecedence) return true
        if (statement is SurfaceBinary && position is BinaryRight && precedence == parentPrecedence) return true
        return false
    }
    
    private fun escapeChar(char: Char) = when (char) {
        '\b' -> "\\b"
        '\t' -> "\\t"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\\' -> "\\\\"
        '\'' -> "\\'"
        else -> if (char.isISOControl()) "\\u%04x".format(char.code) else char.toString()
    }
    
    private fun escapeString(string: String) = buildString {
        string.forEach { char ->
            append(
                when (char) {
                    '\b' -> "\\b"
                    '\t' -> "\\t"
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '\\' -> "\\\\"
                    '"' -> "\\\""
                    else -> if (char.isISOControl()) "\\u%04x".format(char.code) else char.toString()
                },
            )
        }
    }
    
    private fun escapeFormattedText(string: String) = buildString {
        var escapedBraceDepth = 0
        string.forEach { char ->
            when (char) {
                '\b' -> append("\\b")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '{' -> {
                    append("\\{")
                    escapedBraceDepth++
                }
                '}' -> {
                    if (escapedBraceDepth > 0) {
                        append('}')
                        escapedBraceDepth--
                    } else {
                        append("\\}")
                    }
                }
                else -> append(if (char.isISOControl()) "\\u%04x".format(char.code) else char.toString())
            }
        }
    }
    
    private fun escapeRawFormattedText(string: String) = buildString {
        var escapedBraceDepth = 0
        string.forEachIndexed { index, char ->
            when (char) {
                '"' -> append("\\\"")
                '{' -> {
                    append("\\{")
                    escapedBraceDepth++
                }
                '}' -> {
                    if (escapedBraceDepth > 0) {
                        append('}')
                        escapedBraceDepth--
                    } else {
                        append("\\}")
                    }
                }
                '\\' -> {
                    if (index == string.lastIndex) append("\\\\")
                    else append('\\')
                }
                else -> append(char)
            }
        }
    }
    
    private fun <T> PrintWriter.printCommaSeparated(values: Iterable<T>, block: PrintWriter.(T) -> Unit) {
        var first = true
        values.forEach { value ->
            if (!first) print(", ")
            first = false
            block(value)
        }
    }
    
    private fun indent(level: Int) = indentUnit.repeat(level)
}

private sealed class StatementPosition(val parentPrecedence: Int)
private data object Standalone : StatementPosition(0)
private data object UnaryOperand : StatementPosition(120)
private data object PostfixTarget : StatementPosition(130)
private data object AssignLeft : StatementPosition(11)
private data object AssignRight : StatementPosition(10)
private data class BinaryLeft(val precedence: Int) : StatementPosition(precedence)
private data class BinaryRight(val precedence: Int) : StatementPosition(precedence)
