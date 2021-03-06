package metautils.types.jvm

import metautils.util.*

// Comes directly from the spec https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.2
typealias FieldDescriptor = FieldType
typealias JvmType = FieldType

sealed class Descriptor(val classFileName: String) : Tree {
    override fun equals(other: Any?) = other is Descriptor && other.classFileName == classFileName
    override fun hashCode() = classFileName.hashCode()
}

data class MethodDescriptor internal constructor(
    val parameterDescriptors: List<ParameterDescriptor>,
    val returnDescriptor: ReturnDescriptor
) : Descriptor("(${parameterDescriptors.joinToString("") { it.classFileName }})${returnDescriptor.classFileName}"),
    Tree by branches(parameterDescriptors, returnDescriptor) {
    companion object {
        fun fromDescriptorString(descriptor: String) = methodDescriptorFromDescriptorString(descriptor)
    }
    override fun toString() = "(${parameterDescriptors.joinToString(", ")}): $returnDescriptor"
}


sealed class ReturnDescriptor(classFileName: String) : Descriptor(classFileName) {
    object Void : ReturnDescriptor("V"), Leaf {
        override fun toString() = "void"
    }
}

sealed class FieldType(classFileName: String) : ReturnDescriptor(classFileName) {
    companion object {
        fun fromDescriptorString(descriptor: String) = jvmTypeFromDescriptorString(descriptor)
    }

}

sealed class JvmPrimitiveType(classFileName: String) : FieldType(classFileName), Leaf {
    object Byte : JvmPrimitiveType("B") {
        override fun toString() = "byte"
    }

    object Char : JvmPrimitiveType("C") {
        override fun toString() = "char"
    }

    object Double : JvmPrimitiveType("D") {
        override fun toString() = "double"
    }

    object Float : JvmPrimitiveType("F") {
        override fun toString() = "float"
    }

    object Int : JvmPrimitiveType("I") {
        override fun toString() = "int"
    }

    object Long : JvmPrimitiveType("J") {
        override fun toString() = "long"
    }

    object Short : JvmPrimitiveType("S") {
        override fun toString() = "short"
    }

    object Boolean : JvmPrimitiveType("Z") {
        override fun toString() = "boolean"
    }
}


data class ObjectType(val fullClassName: QualifiedName) :
    FieldType("L${fullClassName.toSlashQualifiedString()};"), Tree by branch(fullClassName) {
    override fun toString() = fullClassName.shortName.toDotQualifiedString()

    constructor(qualifiedName: String, dotQualified: Boolean) : this(qualifiedName.toQualifiedName(dotQualified))
}

data class ArrayType(val componentType: FieldType) : FieldType("[" + componentType.classFileName),
    Tree by branch(componentType) {
    override fun toString() = "$componentType[]"
}

typealias ParameterDescriptor = FieldType

