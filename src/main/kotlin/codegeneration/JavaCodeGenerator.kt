package codegeneration


import api.AnyJavaType
import api.JavaAnnotation
import api.JavaClassType
import api.JavaReturnType
import com.squareup.javapoet.*
import signature.ThrowableType
import signature.TypeArgumentDeclaration
import util.PackageName
import java.nio.file.Path
import javax.lang.model.element.Modifier

@DslMarker
annotation class CodeGeneratorDsl

data class ClassInfo(
    val shortName: String,
    val visibility: Visibility,
    /**
     * Interfaces are NOT considered abstract
     */
    val isAbstract: Boolean,
    val isInterface: Boolean,
    val typeArguments: List<TypeArgumentDeclaration>,
    val superClass: JavaClassType?,
    val superInterfaces: List<JavaClassType>,
    val annotations: List<JavaAnnotation>,
    val body: JavaGeneratedClass.() -> Unit
)

@CodeGeneratorDsl
object JavaCodeGenerator {

    fun writeClass(
        info: ClassInfo,
        packageName: PackageName?,
        writeTo: Path
    ) {
        val generatedClass = generateClass(info)
        JavaFile.builder(
            packageName?.toDotQualified() ?: "",
            generatedClass.build()
        ).skipJavaLangImports(true).build().writeTo(writeTo)
    }


}

private fun generateClass(info: ClassInfo): TypeSpec.Builder = with(info) {
    val builder = if (isInterface) TypeSpec.interfaceBuilder(shortName) else TypeSpec.classBuilder(shortName)
    builder.apply {
        visibility.toModifier()?.let { addModifiers(it) }
        if (isAbstract) addModifiers(Modifier.ABSTRACT)
        if (superClass != null) superclass(superClass.toTypeName())
        for (superInterface in superInterfaces) addSuperinterface(superInterface.toTypeName())
        addTypeVariables(typeArguments.map { it.toTypeName() })
        addAnnotations(this@with.annotations.map { it.toAnnotationSpec() })
    }
    JavaGeneratedClass(builder, isInterface).body()
    return builder
}


@CodeGeneratorDsl
class JavaGeneratedClass(
    private val typeSpec: TypeSpec.Builder,
    private val isInterface: Boolean
) {
    fun addMethod(
        visibility: Visibility,
        static: Boolean,
        final: Boolean,
        abstract: Boolean,
        typeArguments: List<TypeArgumentDeclaration>,
        name: String,
        parameters: Map<String, AnyJavaType>,
        returnType: JavaReturnType?,
        throws: List<ThrowableType>,
        body: JavaGeneratedMethod.() -> Unit
    ) {
        addMethodImpl(visibility, parameters, typeArguments, MethodSpec.methodBuilder(name), internalConfig = {
            if (returnType != null) {
                returns(returnType.toTypeName())
                addAnnotations(returnType.annotations.map { it.toAnnotationSpec() })
            }

            if (abstract) addModifiers(Modifier.ABSTRACT)
            else if (static) addModifiers(Modifier.STATIC)
            else if (isInterface) addModifiers(Modifier.DEFAULT)

            if (final) addModifiers(Modifier.FINAL)
            addExceptions(throws.map { it.toTypeName() })
        }, userConfig = body)

    }


    fun addInnerClass(info: ClassInfo, isStatic: Boolean) {
        val generatedClass = generateClass(info)
        typeSpec.addType(generatedClass.apply {
            if (isStatic) addModifiers(Modifier.STATIC)
        }.build())
    }


    fun addConstructor(
        visibility: Visibility,
        parameters: Map<String, AnyJavaType>,
        init: JavaGeneratedMethod.() -> Unit
    ) {
        require(!isInterface) { "Interfaces don't have constructors" }
        addMethodImpl(
            visibility,
            parameters,
            listOf(),
            MethodSpec.constructorBuilder(),
            internalConfig = {},
            userConfig = init
        )
    }

    private fun addMethodImpl(
        visibility: Visibility,
        parameters: Map<String, AnyJavaType>,
        typeArguments: List<TypeArgumentDeclaration>,
        builder: MethodSpec.Builder,
        internalConfig: MethodSpec.Builder.() -> Unit,
        userConfig: JavaGeneratedMethod.() -> Unit
    ) {
        typeSpec.addMethod(
            JavaGeneratedMethod(builder
                .apply {
                    addParameters(parameters.map { (name, type) ->
                        ParameterSpec.builder(type.toTypeName(), name).apply {
                            addAnnotations(type.annotations.map { it.toAnnotationSpec() })
                        }.build()
                    })
                    visibility.toModifier()?.let { addModifiers(it) }
                    internalConfig()
                    addTypeVariables(typeArguments.map { it.toTypeName() })
                }).apply(userConfig)
                .build()
        )
    }


    fun addField(
        name: String,
        type: AnyJavaType,
        visibility: Visibility,
        static: Boolean,
        final: Boolean,
        initializer: Expression?
    ) {
        typeSpec.addField(FieldSpec.builder(type.toTypeName(), name)
            .apply {
                visibility.toModifier()?.let { addModifiers(it) }
                if (static) addModifiers(Modifier.STATIC)
                if (final) addModifiers(Modifier.FINAL)
                if (initializer != null) {
                    val (format, arguments) = JavaCodeWriter().write(initializer)
                    initializer(format, *arguments.toTypedArray())
                }
            }
            .build()
        )
    }

    fun build(): TypeSpec = typeSpec.build()


}


@CodeGeneratorDsl
class JavaGeneratedMethod(private val methodSpec: MethodSpec.Builder) {

    fun addStatement(statement: Statement) {
        val (format, arguments) = JavaCodeWriter().write(statement)
        methodSpec.addStatement(format, *arguments.toTypedArray())
    }

    fun addComment(comment: String) {
        methodSpec.addComment(comment)
    }

    fun build(): MethodSpec = methodSpec.build()
}


