package com.cy.ksp.compiler.entity

import com.cy.ksp.compiler.findAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class OptionalField(property: KSPropertyDeclaration): Field(property) {

    var defaultValue: Any? = null
        private set

    init {
        val annotation = property.findAnnotation("com.cy.annotation.Optional")
        val arguments = annotation?.arguments?.associate { argument ->
            argument.name?.asString() to argument.value
        }
        when (property.type.resolve().declaration.qualifiedName?.asString()) {
            "kotlin.Boolean" -> defaultValue = arguments?.get("booleanValue")
            "kotlin.Byte",
            "kotlin.Short",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Char" -> defaultValue = arguments?.get("intValue")
            "kotlin.Float",
            "kotlin.Double" -> defaultValue = arguments?.get("floatValue")
            "kotlin.String" -> defaultValue = arguments?.get("stringValue")
        }
    }

    override fun compareTo(other: Field): Int {
        if (other is OptionalField) {
            return super.compareTo(other)
        }
        return 1
    }

    override fun toString(): String {
        return "$name:$type, value = $defaultValue"
    }
}
