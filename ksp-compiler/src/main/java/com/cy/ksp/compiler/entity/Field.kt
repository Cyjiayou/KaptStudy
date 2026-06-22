package com.cy.ksp.compiler.entity

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName

open class Field(private val property: KSPropertyDeclaration): Comparable<Field> {
    val name = property.simpleName.asString()

    val type = property.type

    val typeName: TypeName = property.type.resolve().toTypeName()

    override fun compareTo(other: Field): Int {
        return name.compareTo(other.name)
    }

    override fun toString(): String {
        return "$name:$type"
    }
}
