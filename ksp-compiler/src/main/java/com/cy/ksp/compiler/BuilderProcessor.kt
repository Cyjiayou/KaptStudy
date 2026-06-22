package com.cy.ksp.compiler

import com.cy.ksp.compiler.build.ActivityClassBuilder.Companion.POSIX
import com.cy.ksp.compiler.entity.ActivityClass
import com.cy.ksp.compiler.entity.Field
import com.cy.ksp.compiler.entity.OptionalField
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate

class BuilderProcessor(private val logger: KSPLogger, private val codeGenerator: CodeGenerator) :
    SymbolProcessor {


    override fun process(resolver: Resolver): List<KSAnnotated> {

        var annotationName = "com.cy.annotation.Builder"

        var symbols = resolver.getSymbolsWithAnnotation(annotationName)

        val activityClasses = hashMapOf<KSClassDeclaration, ActivityClass>()

        symbols.filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .forEach { clazz ->
                logger.warn("class name is ${clazz.packageName.asString()} + ${clazz.simpleName.asString()}")
                val isActivity = clazz.getAllSuperTypes().any {
                    it.declaration.qualifiedName?.asString() == "android.app.Activity"
                }
                if (isActivity) {
                    activityClasses[clazz] = ActivityClass(clazz)
                } else {
                    logger.error("Unsupported clazz: ${clazz.simpleName.asString()}")
                }
            }

        annotationName = "com.cy.annotation.Required"

        symbols = resolver.getSymbolsWithAnnotation(annotationName)

        symbols.filter { it.validate() }
            .filterIsInstance<KSPropertyDeclaration>()
            .forEach { property ->
                logger.warn("properties name is ${property.qualifiedName?.asString()}")
                val clazz = property.parentDeclaration
                activityClasses[clazz]?.fields?.add(Field(property))
                    ?: logger.error("Field $property annotated as Required while ${property.parentDeclaration} not annotated")
            }

        annotationName = "com.cy.annotation.Optional"

        symbols = resolver.getSymbolsWithAnnotation(annotationName)

        symbols.filter { it.validate() }
            .filterIsInstance<KSPropertyDeclaration>()
            .forEach { property ->
                logger.warn("properties name is ${property.qualifiedName?.asString()}")
                val clazz = property.parentDeclaration
                activityClasses[clazz]?.fields?.add(OptionalField(property))
                    ?: logger.error("Field $property annotated as Optional while ${property.parentDeclaration} not annotated")
            }

        activityClasses.values.forEach {
            logger.warn(it.toString())
            it.builder.build(codeGenerator)
        }

        return emptyList()
    }
}