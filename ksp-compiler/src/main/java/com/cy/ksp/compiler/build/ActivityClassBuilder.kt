package com.cy.ksp.compiler.build

import com.cy.ksp.compiler.capitalizeFirstLetter
import com.cy.ksp.compiler.entity.ActivityClass
import com.cy.ksp.compiler.entity.Field
import com.cy.ksp.compiler.entity.OptionalField
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.writeTo

/** Generates the Kotlin counterpart of the Builder classes produced by the KAPT processor. */
class ActivityClassBuilder(private val activityClass: ActivityClass) {

    companion object {
        const val POSIX = "Builder"

        private val contextType = ClassName("android.content", "Context")
        private val intentType = ClassName("android.content", "Intent")
        private val activityType = ClassName("android.app", "Activity")
        private val bundleType = ClassName("android.os", "Bundle")
        private val activityBuilderType = ClassName("com.cy.runtime", "ActivityBuilder")
        private val bundleUtilsType = ClassName("com.cy.runtime.utils", "BundleUtils")
        private val jvmStaticType = ClassName("kotlin.jvm", "JvmStatic")
    }

    fun build(codeGenerator: CodeGenerator) {
        val builder = TypeSpec.classBuilder("${activityClass.name}$POSIX")
        val companion = TypeSpec.companionObjectBuilder()
        val optionalFields = optionalFields()

        companion.addFunction(startMethod("start", optionalFields))
        companion.addFunction(startMethod("startWithoutOptional", emptyList()))

        if (optionalFields.size <= 2) {
            optionalFields.forEach { field ->
                companion.addFunction(
                    startMethod("startWithOptional${field.name.capitalizeFirstLetter()}", listOf(field))
                )
            }
        } else {
            optionalFields.forEach { field ->
                builder.addProperty(
                    PropertySpec.builder(field.name, field.typeName.copy(nullable = true), KModifier.PRIVATE)
                        .mutable(true)
                        .initializer("null")
                        .build()
                )
                builder.addFunction(setMethod(field))
            }
            builder.addFunction(fillOptionsMethod(optionalFields))
            builder.addFunction(startWithOptionalsMethod())
        }

        companion.addFunction(injectMethod())
        companion.addFunction(saveStateMethod())
        builder.addType(companion.build())

        val file = FileSpec.builder(activityClass.packageName, "${activityClass.name}$POSIX")
            .addType(builder.build())
            .build()
        val dependencies = activityClass.containingFile?.let { Dependencies(false, it) } ?: Dependencies(false)
        file.writeTo(codeGenerator, dependencies)
    }

    private fun startMethod(name: String, selectedOptionalFields: List<Field>): FunSpec {
        val method = FunSpec.builder(name)
            .addAnnotation(jvmStaticAnnotation())
            .addParameter("context", contextType)
            .returns(UNIT)

        addRequiredParameters(method)
        selectedOptionalFields.forEach { field -> method.addParameter(field.name, field.typeName) }
        addIntentAndRequiredExtras(method)
        selectedOptionalFields.forEach { field ->
            method.addStatement("intent.putExtra(%S, %N)", field.name, field.name)
        }
        method.addStatement("%T.INSTANCE.startActivity(context, intent)", activityBuilderType)
        return method.build()
    }

    private fun startWithOptionalsMethod(): FunSpec {
        val method = FunSpec.builder("startWithOptionals")
            .addParameter("context", contextType)
            .returns(UNIT)
        addRequiredParameters(method)
        addIntentAndRequiredExtras(method)
        // Fill extras before starting the Activity. The KAPT implementation currently does this in
        // the reverse order; keeping the KSP output correct makes this path usable as a reference.
        method.addStatement("fillOptions(intent)")
        method.addStatement("%T.INSTANCE.startActivity(context, intent)", activityBuilderType)
        return method.build()
    }

    private fun setMethod(field: Field): FunSpec = FunSpec.builder("set${field.name.capitalizeFirstLetter()}")
        .addParameter(field.name, field.typeName)
        .returns(ClassName(activityClass.packageName, "${activityClass.name}$POSIX"))
        .addStatement("this.%N = %N", field.name, field.name)
        .addStatement("return this")
        .build()

    private fun fillOptionsMethod(fields: List<Field>): FunSpec {
        val method = FunSpec.builder("fillOptions")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("intent", intentType)
            .returns(UNIT)
        fields.forEach { field ->
            method.beginControlFlow("if (%N != null)", field.name)
                .addStatement("intent.putExtra(%S, %N)", field.name, field.name)
                .endControlFlow()
        }
        return method.build()
    }

    private fun injectMethod(): FunSpec {
        val method = FunSpec.builder("inject")
            .addAnnotation(jvmStaticAnnotation())
            .addParameter("activity", activityType)
            .addParameter("bundle", bundleType.copy(nullable = true))
            .returns(UNIT)

        method.beginControlFlow("if (activity is %T)", activityClass.typeName)
            .addStatement("val extras = bundle ?: activity.intent.extras")
            .beginControlFlow("if (extras != null)")

        activityClass.fields.forEach { field ->
            val valueName = "${field.name}Value"
            val defaultArgument = if (field is OptionalField) {
                CodeBlock.of(", %L", defaultValueCode(field))
            } else {
                CodeBlock.of("")
            }
            method.addStatement(
                "val %N = %T.get<%T>(extras, %S%L)",
                valueName,
                bundleUtilsType,
                field.typeName,
                field.name,
                defaultArgument
            )
            method.addStatement("activity.%N = %N", field.name, valueName)
        }
        method.endControlFlow().endControlFlow()
        return method.build()
    }

    private fun saveStateMethod(): FunSpec {
        val method = FunSpec.builder("saveState")
            .addAnnotation(jvmStaticAnnotation())
            .addParameter("activity", activityType)
            .addParameter("outState", bundleType)
            .returns(UNIT)

        method.beginControlFlow("if (activity is %T)", activityClass.typeName)
            .addStatement("val intent = %T()", intentType)
        activityClass.fields.forEach { field ->
            method.addStatement("intent.putExtra(%S, activity.%N)", field.name, field.name)
        }
        method.addStatement("outState.putAll(intent.extras)")
            .endControlFlow()
        return method.build()
    }

    private fun addRequiredParameters(method: FunSpec.Builder) {
        requiredFields().forEach { field -> method.addParameter(field.name, field.typeName) }
    }

    private fun addIntentAndRequiredExtras(method: FunSpec.Builder) {
        method.addStatement("val intent = %T(context, %T::class.java)", intentType, activityClass.typeName)
        requiredFields().forEach { field ->
            method.addStatement("intent.putExtra(%S, %N)", field.name, field.name)
        }
    }

    private fun defaultValueCode(field: OptionalField): CodeBlock {
        return when (val value = field.defaultValue) {
            null -> CodeBlock.of("null")
            is String -> CodeBlock.of("%S", value)
            is Boolean -> CodeBlock.of("%L", value)
            is Number -> when (field.type.resolve().declaration.qualifiedName?.asString()) {
                "kotlin.Byte" -> CodeBlock.of("%L.toByte()", value)
                "kotlin.Short" -> CodeBlock.of("%L.toShort()", value)
                "kotlin.Int" -> CodeBlock.of("%L", value)
                "kotlin.Long" -> CodeBlock.of("%L.toLong()", value)
                "kotlin.Char" -> CodeBlock.of("%L.toInt().toChar()", value)
                "kotlin.Float" -> CodeBlock.of("%LF", value)
                "kotlin.Double" -> CodeBlock.of("%L.toDouble()", value)
                else -> CodeBlock.of("%L", value)
            }
            else -> CodeBlock.of("%L", value)
        }
    }

    private fun optionalFields(): List<Field> = activityClass.fields.filterIsInstance<OptionalField>()

    private fun requiredFields(): List<Field> = activityClass.fields.filterNot { it is OptionalField }

    private fun jvmStaticAnnotation(): AnnotationSpec = AnnotationSpec.builder(jvmStaticType).build()
}
