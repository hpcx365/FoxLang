package pers.hpcx.foxlang.frontend.fox

import pers.hpcx.foxlang.ast.*
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
    
    fun PrintWriter.printFile(file: FoxFile) {
        file.elements.forEachIndexed { index, element ->
            if (index > 0) print(fileSeparator)
            printFileElement(element)
            print('\n')
        }
    }
    
    fun PrintWriter.printFileElement(element: FoxFileElement) {
        when (element) {
            is FoxTypeAlias -> printTypeAlias(element)
            is FoxMethodDefinition -> printMethodDefinition(element)
        }
    }
    
    fun PrintWriter.printType(type: FoxType) {
        when (type) {
            is FoxPrimitiveType -> printPrimitiveType(type)
            is FoxWildcardType -> printWildcardType(type)
            is FoxBuiltInType -> printBuiltInType(type)
            is FoxTransformType -> printTransformType(type)
            is FoxUnresolvedType -> printUnresolvedType(type)
            is FoxPlaceholderType -> error("Placeholder type should not be printed")
        }
    }
    
    fun PrintWriter.printStatement(statement: FoxStatement) {
        printStatement(statement, 0, Standalone)
    }
    
    fun PrintWriter.printTypeAlias(element: FoxTypeAlias) {
        print("type ")
        print(element.name)
        printTypeParameterNames(element.generics)
        print(" = ")
        printType(element.alias)
    }
    
    fun PrintWriter.printMethodDefinition(element: FoxMethodDefinition) {
        print("def ")
        if (canonical || element.generics.isNotEmpty()) {
            printFormalGenerics(element.generics)
            print(' ')
        }
        if (canonical || element.thisType != FoxUnitType) {
            printType(element.thisType)
            print('.')
        }
        print(element.name)
        printFormalParameters(element.parameters)
        if (canonical || element.returnType != FoxUnitType) {
            print(": ")
            printType(element.returnType)
        }
        print(' ')
        printMethodBody(element.body)
    }
    
    private fun PrintWriter.printPrimitiveType(type: FoxPrimitiveType) {
        when (type) {
            FoxVoidType -> print("Void")
            FoxUnitType -> print("Unit")
            FoxBoolType -> print("Bool")
            FoxByteType -> print("Byte")
            FoxShortType -> print("Short")
            FoxIntType -> print("Int")
            FoxLongType -> print("Long")
            FoxFloatType -> print("Float")
            FoxDoubleType -> print("Double")
            FoxCharType -> print("Char")
            FoxStringType -> print("String")
        }
    }
    
    private fun PrintWriter.printWildcardType(type: FoxWildcardType) {
        when (type) {
            FoxAnyType -> print("Any")
            FoxAnyTupleType -> print("AnyTuple")
            FoxAnyStructType -> print("AnyStruct")
            FoxAnyObjectType -> print("AnyObject")
            FoxAnyEnumType -> print("AnyEnum")
            is FoxAnyOfType -> printTypeListArgument("AnyOf", type.types)
            is FoxAllOfType -> printTypeListArgument("AllOf", type.types)
            is FoxNoneOfType -> printTypeListArgument("NoneOf", type.types)
            is FoxAnyTupleOfType -> printSingleTypeArgument("AnyTupleOf", type.component)
            is FoxAnyStructOfType -> printTypeListArgument("AnyStructOf", type.fields)
        }
    }
    
    private fun PrintWriter.printBuiltInType(type: FoxBuiltInType) {
        when (type) {
            is FoxTupleType -> printTupleType(type)
            is FoxStructType -> printStructType(type)
            is FoxObjectType -> printObjectType(type)
            is FoxEnumType -> printEnumType(type)
            is FoxArrayType -> printSingleTypeArgument("Array", type.element)
            is FoxRefType -> printSingleTypeArgument("Ref", type.referent)
            is FoxMethodType -> printMethodType(type)
        }
    }
    
    private fun PrintWriter.printTransformType(type: FoxTransformType) {
        when (type) {
            is FoxTupleComponentAtType -> printTypeAndIntArgument("ComponentAt", type.type, type.index)
            is FoxTupleLastComponentAtType -> printTypeAndIntArgument("LastComponentAt", type.type, type.index)
            is FoxTupleFirstComponentsOfType -> printTypeAndIntArgument("FirstComponentsOf", type.type, type.count)
            is FoxTupleExactFirstComponentsOfType -> printTypeAndIntArgument("ExactFirstComponentsOf", type.type, type.count)
            is FoxTupleLastComponentsOfType -> printTypeAndIntArgument("LastComponentsOf", type.type, type.count)
            is FoxTupleExactLastComponentsOfType -> printTypeAndIntArgument("ExactLastComponentsOf", type.type, type.count)
            is FoxTupleDropFirstComponentsOfType -> printTypeAndIntArgument("DropFirstComponentsOf", type.type, type.count)
            is FoxTupleExactDropFirstComponentsOfType -> printTypeAndIntArgument("ExactDropFirstComponentsOf", type.type, type.count)
            is FoxTupleDropLastComponentsOfType -> printTypeAndIntArgument("DropLastComponentsOf", type.type, type.count)
            is FoxTupleExactDropLastComponentsOfType -> printTypeAndIntArgument("ExactDropLastComponentsOf", type.type, type.count)
            is FoxTupleMergeComponentsOfType -> printTypeListArgument("MergeComponentsOf", type.types)
            is FoxStructFieldOfType -> printTypeAndNameArgument("FieldOf", type.type, type.name)
            is FoxStructFieldAtType -> printTypeAndIntArgument("FieldAt", type.type, type.index)
            is FoxStructLastFieldAtType -> printTypeAndIntArgument("LastFieldAt", type.type, type.index)
            is FoxStructFirstFieldsOfType -> printTypeAndIntArgument("FirstFieldsOf", type.type, type.count)
            is FoxStructExactFirstFieldsOfType -> printTypeAndIntArgument("ExactFirstFieldsOf", type.type, type.count)
            is FoxStructLastFieldsOfType -> printTypeAndIntArgument("LastFieldsOf", type.type, type.count)
            is FoxStructExactLastFieldsOfType -> printTypeAndIntArgument("ExactLastFieldsOf", type.type, type.count)
            is FoxStructDropFirstFieldsOfType -> printTypeAndIntArgument("DropFirstFieldsOf", type.type, type.count)
            is FoxStructExactDropFirstFieldsOfType -> printTypeAndIntArgument("ExactDropFirstFieldsOf", type.type, type.count)
            is FoxStructDropLastFieldsOfType -> printTypeAndIntArgument("DropLastFieldsOf", type.type, type.count)
            is FoxStructExactDropLastFieldsOfType -> printTypeAndIntArgument("ExactDropLastFieldsOf", type.type, type.count)
            is FoxStructFieldsOfType -> printTypeAndNamesArgument("FieldsOf", type.type, type.names)
            is FoxStructDropFieldsOfType -> printTypeAndNamesArgument("DropFieldsOf", type.type, type.names)
            is FoxStructMergeFieldsOfType -> printTypeListArgument("MergeFieldsOf", type.types)
            is FoxObjectMemberOfType -> printTypeAndNameArgument("MemberOf", type.type, type.name)
            is FoxObjectMembersOfType -> printTypeAndNamesArgument("MembersOf", type.type, type.names)
            is FoxObjectDropMembersOfType -> printTypeAndNamesArgument("DropMembersOf", type.type, type.names)
            is FoxObjectMergeMembersOfType -> printTypeListArgument("MergeMembersOf", type.types)
            is FoxEnumEntryOfType -> printTypeAndNameArgument("EntryOf", type.type, type.name)
            is FoxEnumEntriesOfType -> printTypeAndNamesArgument("EntriesOf", type.type, type.names)
            is FoxEnumDropEntriesOfType -> printTypeAndNamesArgument("DropEntriesOf", type.type, type.names)
            is FoxEnumMergeEntriesOfType -> printTypeListArgument("MergeEntriesOf", type.types)
            is FoxArrayElementOfType -> printSingleTypeArgument("ElementOf", type.type)
            is FoxRefReferentOfType -> printSingleTypeArgument("ReferentOf", type.type)
            is FoxMethodOfType -> printTypeListArgument("MethodOf", listOf(type.`this`, type.parameters, type.`return`))
            is FoxMethodThisOfType -> printSingleTypeArgument("ThisOf", type.type)
            is FoxMethodParametersOfType -> printSingleTypeArgument("ParametersOf", type.type)
            is FoxMethodReturnOfType -> printSingleTypeArgument("ReturnOf", type.type)
        }
    }
    
    private fun PrintWriter.printTupleType(type: FoxTupleType) {
        print("Tuple<")
        var first = true
        var current: FoxType? = null
        var count = 0
        fun flushCurrent() {
            val value = current ?: return
            if (!first) print(", ")
            first = false
            printType(value)
            if (count > 1) {
                print(':')
                print(count)
            }
        }
        type.components.forEach { component ->
            if (current == component) {
                count++
            } else {
                flushCurrent()
                current = component
                count = 1
            }
        }
        flushCurrent()
        print('>')
    }
    
    private fun PrintWriter.printStructType(type: FoxStructType) {
        print("Struct<")
        printCommaSeparated(type.fields.entries) { field ->
            print(field.key)
            print(": ")
            printType(field.value)
        }
        print('>')
    }
    
    private fun PrintWriter.printObjectType(type: FoxObjectType) {
        print("Object<")
        printCommaSeparated(type.members.entries) { member ->
            print(member.key)
            print(": ")
            printType(member.value)
        }
        print('>')
    }
    
    private fun PrintWriter.printEnumType(type: FoxEnumType) {
        print("Enum<")
        printCommaSeparated(type.entries.entries) { item ->
            print(item.key)
            print(" = ")
            printType(item.value)
        }
        print('>')
    }
    
    private fun PrintWriter.printMethodType(type: FoxMethodType) {
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
    
    private fun PrintWriter.printUnresolvedType(type: FoxUnresolvedType) {
        print(type.name)
        type.parameters?.let { parameters ->
            print('<')
            printCommaSeparated(parameters) { parameter ->
                printType(parameter)
            }
            print('>')
        }
    }
    
    private fun PrintWriter.printSingleTypeArgument(keyword: String, type: FoxType) {
        print(keyword)
        print('<')
        printType(type)
        print('>')
    }
    
    private fun PrintWriter.printTypeAndIntArgument(keyword: String, type: FoxType, value: Int) {
        print(keyword)
        print('<')
        printType(type)
        print(", ")
        print(value)
        print('>')
    }
    
    private fun PrintWriter.printTypeAndNameArgument(keyword: String, type: FoxType, name: String) {
        print(keyword)
        print('<')
        printType(type)
        print(", ")
        print(name)
        print('>')
    }
    
    private fun PrintWriter.printTypeAndNamesArgument(keyword: String, type: FoxType, names: Iterable<String>) {
        print(keyword)
        print('<')
        printType(type)
        print(", ")
        printCommaSeparated(names) { name ->
            print(name)
        }
        print('>')
    }
    
    private fun PrintWriter.printTypeListArgument(keyword: String, types: Iterable<FoxType>) {
        print(keyword)
        print('<')
        printCommaSeparated(types) { type ->
            printType(type)
        }
        print('>')
    }
    
    private fun PrintWriter.printStatement(statement: FoxStatement, indentLevel: Int, position: StatementPosition) {
        val precedence = precedenceOf(statement)
        if (needsParentheses(statement, precedence, position)) print('(')
        printRawStatement(statement, indentLevel)
        if (needsParentheses(statement, precedence, position)) print(')')
    }
    
    private fun PrintWriter.printRawStatement(statement: FoxStatement, indentLevel: Int) {
        when (statement) {
            FoxThis -> print("this")
            is FoxSymbol -> print(statement.name)
            is FoxEntityStatement -> printEntity(statement.value)
            is FoxFormattedString -> printFormattedString(statement)
            is FoxLambda -> printLambda(statement, indentLevel)
            is FoxBlock -> printBlock(statement, indentLevel)
            is FoxUnary -> printUnary(statement, indentLevel)
            is FoxBinary -> printBinary(statement, indentLevel)
            is FoxAssign -> printAssign(statement, indentLevel)
            is FoxTypeBinding -> {
                print(statement.name)
                print(": ")
                printType(statement.type)
            }
            is FoxFieldAccess -> {
                printStatement(statement.target, indentLevel, PostfixTarget)
                print('.')
                print(statement.name)
            }
            is FoxIndexAccess -> {
                printStatement(statement.target, indentLevel, PostfixTarget)
                print('[')
                printCommaSeparated(statement.indices) {
                    printStatement(it, indentLevel, Standalone)
                }
                print(']')
            }
            is FoxCall -> printCall(statement, indentLevel)
            is FoxConstruct -> {
                printType(statement.type)
                printActualParametersWithTrailingLambda(statement.parameters, indentLevel)
            }
            is FoxIndirectCall -> printIndirectCall(statement, indentLevel)
            is FoxIf -> printIf(statement, indentLevel)
            is FoxWhen -> printWhen(statement, indentLevel)
            is FoxWhile -> printWhile(statement, indentLevel)
            is FoxDoWhile -> printDoWhile(statement, indentLevel)
            is FoxBreak -> {
                print("break")
                statement.label?.let {
                    print(" #")
                    print(it)
                }
            }
            is FoxContinue -> {
                print("continue")
                statement.label?.let {
                    print(" #")
                    print(it)
                }
            }
            is FoxYield -> {
                print("yield")
                statement.label?.let {
                    print(" #")
                    print(it)
                }
                print(' ')
                printStatement(statement.value, indentLevel, Standalone)
            }
            is FoxReturn -> {
                print("return")
                statement.value?.let {
                    print(' ')
                    printStatement(it, indentLevel, Standalone)
                }
            }
        }
    }
    
    private fun PrintWriter.printBlock(block: FoxBlock, indentLevel: Int) {
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
    
    private fun PrintWriter.printUnary(statement: FoxUnary, indentLevel: Int) {
        print(unaryOperatorText(statement.operator))
        printStatement(statement.right, indentLevel, UnaryOperand)
    }
    
    private fun PrintWriter.printBinary(statement: FoxBinary, indentLevel: Int) {
        val precedence = binaryPrecedence(statement.operator)
        printStatement(statement.left, indentLevel, BinaryLeft(precedence))
        print(' ')
        print(binaryOperatorText(statement.operator))
        print(' ')
        printStatement(statement.right, indentLevel, BinaryRight(precedence))
    }
    
    private fun PrintWriter.printAssign(statement: FoxAssign, indentLevel: Int) {
        printStatement(statement.left, indentLevel, AssignLeft)
        print(' ')
        print(assignOperatorText(statement.operator))
        print(' ')
        printStatement(statement.right, indentLevel, AssignRight)
    }
    
    private fun PrintWriter.printCall(statement: FoxCall, indentLevel: Int) {
        if (canonical || statement.target != FoxEntityStatement(FoxUnit)) {
            printStatement(statement.target, indentLevel, PostfixTarget)
            print('.')
        }
        print(statement.name)
        statement.generics?.let { printActualTypeArguments(it) }
        printActualParametersWithTrailingLambda(statement.parameters, indentLevel)
    }
    
    private fun PrintWriter.printIndirectCall(statement: FoxIndirectCall, indentLevel: Int) {
        if (canonical || statement.target != FoxEntityStatement(FoxUnit)) {
            printStatement(statement.target, indentLevel, PostfixTarget)
            print('.')
        }
        print('(')
        printStatement(statement.method, indentLevel, Standalone)
        print(')')
        printActualParametersWithTrailingLambda(statement.parameters, indentLevel)
    }
    
    private fun PrintWriter.printIf(statement: FoxIf, indentLevel: Int) {
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
    
    private fun PrintWriter.printWhen(statement: FoxWhen, indentLevel: Int) {
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
    
    private fun PrintWriter.printWhile(statement: FoxWhile, indentLevel: Int) {
        printLabelPrefix(statement.label)
        print("while (")
        printStatement(statement.condition, indentLevel, Standalone)
        print(") ")
        printControlBody(statement.body, indentLevel)
    }
    
    private fun PrintWriter.printDoWhile(statement: FoxDoWhile, indentLevel: Int) {
        printLabelPrefix(statement.label)
        print("do ")
        printControlBody(statement.body, indentLevel)
        print(" while (")
        printStatement(statement.condition, indentLevel, Standalone)
        print(')')
    }
    
    private fun PrintWriter.printMethodBody(statement: FoxStatement) {
        if (statement is FoxBlock) printBlock(statement, 0)
        else printBlockBody(statement, 0)
    }
    
    private fun PrintWriter.printControlBody(statement: FoxStatement, indentLevel: Int, allowElseIfChain: Boolean = false) {
        when {
            canonical && statement is FoxIf && allowElseIfChain ->
                printStatement(statement, indentLevel, Standalone)
            
            canonical -> printBlockBody(statement, indentLevel)
            else -> printStatement(statement, indentLevel, Standalone)
        }
    }
    
    private fun PrintWriter.printBlockBody(statement: FoxStatement, indentLevel: Int) {
        if (statement is FoxBlock) {
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
    
    private fun PrintWriter.printFormalGenerics(generics: OrderedMap<String, FoxType>) {
        print('<')
        printCommaSeparated(generics.entries) { (name, pattern) ->
            print(name)
            print(" = ")
            printType(pattern)
        }
        print('>')
    }
    
    private fun PrintWriter.printFormalParameters(parameters: OrderedMap<String, FoxType>) {
        print('(')
        printCommaSeparated(parameters.entries) { parameter ->
            print(parameter.key)
            print(": ")
            printType(parameter.value)
        }
        print(')')
    }
    
    private fun PrintWriter.printActualParameters(parameters: List<Pair<String?, FoxStatement>>, indentLevel: Int) {
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
        parameters: List<Pair<String?, FoxStatement>>,
        indentLevel: Int,
    ) {
        val trailingLambda = parameters.lastOrNull()
            ?.takeIf { !canonical && it.first == null && it.second is FoxLambda }
            ?.second as FoxLambda?
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
    
    private fun PrintWriter.printActualTypeArguments(generics: List<Pair<String?, FoxType>>) {
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
    
    private fun PrintWriter.printFormattedString(statement: FoxFormattedString) {
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
    
    private fun PrintWriter.printLambda(statement: FoxLambda, indentLevel: Int) {
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
            is FoxBlock -> {
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
    
    private fun unaryOperatorText(operator: FoxUnaryOperator) = when (operator) {
        FoxNotOperator -> "!"
        FoxNegOperator -> "-"
    }
    
    private fun binaryOperatorText(operator: FoxBinaryOperator) = when (operator) {
        FoxAddOperator -> "+"
        FoxSubOperator -> "-"
        FoxMulOperator -> "*"
        FoxDivOperator -> "/"
        FoxRemOperator -> "%"
        FoxAndOperator -> "&"
        FoxOrOperator -> "|"
        FoxXorOperator -> "^"
        FoxShlOperator -> "<<"
        FoxShrOperator -> ">>"
        FoxUshrOperator -> ">>>"
        FoxEqOperator -> "=="
        FoxNeqOperator -> "!="
        FoxLtOperator -> "<"
        FoxGtOperator -> ">"
        FoxLeqOperator -> "<="
        FoxGeqOperator -> ">="
        FoxAndAndOperator -> "&&"
        FoxOrOrOperator -> "||"
    }
    
    private fun assignOperatorText(operator: FoxAssignOperator) = when (operator) {
        FoxPlainAssignOperator -> "="
        FoxDefAssignOperator -> ":="
        FoxAddAssignOperator -> "+="
        FoxSubAssignOperator -> "-="
        FoxMulAssignOperator -> "*="
        FoxDivAssignOperator -> "/="
        FoxRemAssignOperator -> "%="
        FoxAndAssignOperator -> "&="
        FoxOrAssignOperator -> "|="
        FoxXorAssignOperator -> "^="
        FoxShlAssignOperator -> "<<="
        FoxShrAssignOperator -> ">>="
        FoxUshrAssignOperator -> ">>>="
        FoxAndAndAssignOperator -> "&&="
        FoxOrOrAssignOperator -> "||="
    }
    
    private fun precedenceOf(statement: FoxStatement): Int = when (statement) {
        is FoxIf,
        is FoxWhen,
        is FoxWhile,
        is FoxDoWhile,
        is FoxBreak,
        is FoxContinue,
        is FoxYield,
        is FoxReturn,
        is FoxBlock,
        is FoxTypeBinding,
            -> 0
        
        is FoxLambda -> 5
        is FoxAssign -> 10
        is FoxBinary -> binaryPrecedence(statement.operator)
        is FoxUnary -> 120
        is FoxFieldAccess,
        is FoxIndexAccess,
        is FoxCall,
        is FoxConstruct,
        is FoxIndirectCall,
            -> 130
        
        FoxThis,
        is FoxSymbol,
        is FoxFormattedString,
        is FoxEntityStatement,
            -> 140
    }
    
    private fun binaryPrecedence(operator: FoxBinaryOperator): Int = when (operator) {
        FoxOrOrOperator -> 20
        FoxAndAndOperator -> 30
        FoxOrOperator -> 40
        FoxXorOperator -> 50
        FoxAndOperator -> 60
        FoxEqOperator, FoxNeqOperator -> 70
        FoxLtOperator, FoxGtOperator, FoxLeqOperator, FoxGeqOperator -> 80
        FoxShlOperator, FoxShrOperator, FoxUshrOperator -> 90
        FoxAddOperator, FoxSubOperator -> 100
        FoxMulOperator, FoxDivOperator, FoxRemOperator -> 110
    }
    
    private fun needsParentheses(
        statement: FoxStatement,
        precedence: Int,
        position: StatementPosition,
    ): Boolean {
        val parentPrecedence = position.parentPrecedence
        if (precedence < parentPrecedence) return true
        if (statement is FoxBinary && position is BinaryRight && precedence == parentPrecedence) return true
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
