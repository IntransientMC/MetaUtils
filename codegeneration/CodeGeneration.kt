package metautils.codegeneration

import codegeneration.ClassAccess
import codegeneration.Expression
import codegeneration.Statement
import codegeneration.Visibility
import metautils.api.*
import metautils.signature.TypeArgumentDeclaration
import metautils.util.PackageName
import java.nio.file.Path

class ClassInfo(
    val shortName: String,
    val visibility: Visibility,
    /**
     * Interfaces are NOT considered abstract
     */
    val access: ClassAccess,
    val typeArguments: List<TypeArgumentDeclaration>,
    val superClass: JavaClassType?,
    val superInterfaces: List<JavaClassType>,
    val annotations: List<JavaAnnotation>,
    val body: GeneratedClass.() -> Unit
)

data class MethodInfo(
    val visibility: Visibility,
    val parameters: List<Pair<String, AnyJavaType>>,
    val throws: List<JavaThrowableType>,
    val body: GeneratedMethod.() -> Unit
)

interface CodeGenerator {
    fun writeClass(
        info: ClassInfo,
        packageName: PackageName?,
        srcRoot: Path
    )
}

interface GeneratedClass {
    fun addMethod(
        methodInfo: MethodInfo,
        isStatic: Boolean, isFinal: Boolean, isAbstract: Boolean,
        typeArguments: List<TypeArgumentDeclaration>,
        name: String,
        returnType: JavaReturnType
    )

    fun addConstructor(info: MethodInfo)
    fun addInnerClass(info: ClassInfo, isStatic: Boolean)
    fun addField(
        name: String,
        type: AnyJavaType,
        visibility: Visibility,
        isStatic: Boolean,
        isFinal: Boolean,
        initializer: Expression?
    )
    fun addJavadoc(comment: String)
}

interface GeneratedMethod {
    fun addStatement(statement: Statement)
    fun addComment(comment: String)
}


fun GeneratedClass.addMethod(
    visibility: Visibility,
    parameters: List<Pair<String, AnyJavaType>>,
    throws: List<JavaThrowableType>,
    static: Boolean,
    final: Boolean,
    abstract: Boolean,
    typeArguments: List<TypeArgumentDeclaration>,
    name: String,
    returnType: JavaReturnType,
    body: GeneratedMethod.() -> Unit
): Unit = addMethod(
    MethodInfo(visibility, parameters, throws, body),
    static, final, abstract, typeArguments, name, returnType
)