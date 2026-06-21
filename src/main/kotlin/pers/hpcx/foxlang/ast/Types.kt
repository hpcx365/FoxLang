package pers.hpcx.foxlang.ast

import pers.hpcx.foxlang.utils.OrderedMap
import pers.hpcx.foxlang.utils.OrderedSet

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
data class FoxAnyOfType(val types: List<FoxType>) : FoxWildcardType
data class FoxAllOfType(val types: List<FoxType>) : FoxWildcardType
data class FoxNoneOfType(val types: List<FoxType>) : FoxWildcardType
object FoxAnyTupleType : FoxWildcardType
data class FoxAnyTupleOfType(val component: FoxType) : FoxWildcardType
object FoxAnyStructType : FoxWildcardType
data class FoxAnyStructOfType(val fields: List<FoxType>) : FoxWildcardType
object FoxAnyObjectType : FoxWildcardType
object FoxAnyEnumType : FoxWildcardType

sealed interface FoxBuiltInType : FoxType
data class FoxTupleType(val components: List<FoxType>) : FoxBuiltInType
data class FoxStructType(val fields: OrderedMap<String, FoxType>) : FoxBuiltInType
data class FoxObjectType(val members: Map<String, FoxType>) : FoxBuiltInType
data class FoxEnumType(val items: Map<String, FoxType>) : FoxBuiltInType
data class FoxArrayType(val element: FoxType) : FoxBuiltInType
data class FoxRefType(val referent: FoxType) : FoxBuiltInType
data class FoxMethodType(val `this`: FoxType, val parameters: OrderedMap<String, FoxType>, val `return`: FoxType) : FoxBuiltInType

sealed interface FoxTransformType : FoxType
data class FoxTupleComponentAtType(val type: FoxType, val index: Int) : FoxTransformType
data class FoxTupleLastComponentAtType(val type: FoxType, val index: Int) : FoxTransformType
data class FoxTupleFirstComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleExactFirstComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleLastComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleExactLastComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleDropFirstComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleExactDropFirstComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleDropLastComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleExactDropLastComponentsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxTupleMergeComponentsOfType(val types: List<FoxType>) : FoxTransformType
data class FoxStructFieldOfType(val type: FoxType, val name: String) : FoxTransformType
data class FoxStructFieldAtType(val type: FoxType, val index: Int) : FoxTransformType
data class FoxStructLastFieldAtType(val type: FoxType, val index: Int) : FoxTransformType
data class FoxStructFirstFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructExactFirstFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructLastFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructExactLastFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructDropFirstFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructExactDropFirstFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructDropLastFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructExactDropLastFieldsOfType(val type: FoxType, val count: Int) : FoxTransformType
data class FoxStructFieldsOfType(val type: FoxType, val names: OrderedSet<String>) : FoxTransformType
data class FoxStructDropFieldsOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class FoxStructMergeFieldsOfType(val types: List<FoxType>) : FoxTransformType
data class FoxObjectMemberOfType(val type: FoxType, val name: String) : FoxTransformType
data class FoxObjectMembersOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class FoxObjectDropMembersOfType(val type: FoxType, val names: Set<String>) : FoxTransformType
data class FoxObjectMergeMembersOfType(val types: List<FoxType>) : FoxTransformType
data class FoxEnumItemOfType(val type: FoxType, val name: String) : FoxTransformType
data class FoxEnumItemsOfType(val type: FoxType, val names: List<String>) : FoxTransformType
data class FoxEnumDropItemsOfType(val type: FoxType, val names: List<String>) : FoxTransformType
data class FoxEnumMergeItemsOfType(val types: List<FoxType>) : FoxTransformType
data class FoxArrayElementOfType(val type: FoxType) : FoxTransformType
data class FoxRefReferentOfType(val type: FoxType) : FoxTransformType
data class FoxMethodOfType(val `this`: FoxType, val parameters: FoxType, val `return`: FoxType) : FoxTransformType
data class FoxMethodThisOfType(val type: FoxType) : FoxTransformType
data class FoxMethodParametersOfType(val type: FoxType) : FoxTransformType
data class FoxMethodReturnOfType(val type: FoxType) : FoxTransformType

data class FoxUnresolvedType(val name: String, val parameters: List<FoxType>?) : FoxType

interface FoxPlaceholderType : FoxType
