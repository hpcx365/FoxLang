package pers.hpcx.foxlang.ast

import java.util.*

sealed interface FoxType

sealed interface FoxPrimitiveType : FoxType
object FoxVoidType : FoxPrimitiveType
object FoxUnitType : FoxPrimitiveType
object FoxBoolType : FoxPrimitiveType
object FoxByteType : FoxPrimitiveType
object FoxShortType : FoxPrimitiveType
object FoxIntType : FoxPrimitiveType
object FoxLongType : FoxPrimitiveType
object FoxFloatType : FoxPrimitiveType
object FoxDoubleType : FoxPrimitiveType
object FoxCharType : FoxPrimitiveType
object FoxStringType : FoxPrimitiveType

sealed interface FoxWildcardType : FoxType
object FoxAnyType : FoxWildcardType
object FoxAnyTupleType : FoxWildcardType
object FoxAnyStructType : FoxWildcardType
object FoxAnyEnumType : FoxWildcardType
object FoxAnyArrayType : FoxWildcardType
object FoxAnyRefType : FoxWildcardType
object FoxAnyLambdaType : FoxWildcardType

sealed interface FoxBuiltInType : FoxType
data class FoxTupleType(val parts: List<FoxType>) : FoxBuiltInType
data class FoxStructType(val fields: SequencedMap<String, FoxType>) : FoxBuiltInType
data class FoxEnumType(val items: SequencedMap<String, FoxType>) : FoxBuiltInType
data class FoxArrayType(val element: FoxType) : FoxBuiltInType
data class FoxRefType(val referent: FoxType) : FoxBuiltInType
data class FoxLambdaType(val `this`: FoxType, val parameters: List<FoxType>, val `return`: FoxType) : FoxBuiltInType

sealed interface FoxTransformType : FoxType

data class FoxTuplePartOfType(val type: FoxType, val index: Int) : FoxTransformType
data class FoxTupleFirstPartsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleLastPartsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleDropFirstPartsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleDropLastPartsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleMergePartsOfType(val types: List<FoxType>) : FoxTransformType

data class FoxStructFieldOfType(val type: FoxType, val name: String) : FoxTransformType
data class FoxStructFieldsOfType(val type: FoxType, val names: List<String>) : FoxTransformType
data class FoxStructDropFieldsOfType(val type: FoxType, val names: List<String>) : FoxTransformType
data class FoxStructMergeFieldsOfType(val types: List<FoxType>) : FoxTransformType

data class FoxEnumItemOfType(val type: FoxType, val name: String) : FoxTransformType
data class FoxEnumItemsOfType(val type: FoxType, val names: List<String>) : FoxTransformType
data class FoxEnumDropItemsOfType(val type: FoxType, val names: List<String>) : FoxTransformType
data class FoxEnumMergeItemsOfType(val types: List<FoxType>) : FoxTransformType

data class FoxArrayElementOfType(val type: FoxType) : FoxTransformType
data class FoxRefReferentOfType(val type: FoxType) : FoxTransformType
data class FoxLambdaThisOfType(val type: FoxType) : FoxTransformType
data class FoxLambdaParametersOfType(val type: FoxType) : FoxTransformType
data class FoxLambdaReturnOfType(val type: FoxType) : FoxTransformType

data class FoxCustomizedType(val name: String, val parameters: List<Pair<String?, FoxType>>) : FoxType
