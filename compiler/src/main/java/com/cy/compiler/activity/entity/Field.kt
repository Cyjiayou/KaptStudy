package com.cy.compiler.activity.entity

import com.cy.compiler.utils.AptUtils
import com.palantir.javapoet.TypeName
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror


open class Field(val element: VariableElement): Comparable<Field> {

    val name: String = element.simpleName.toString()

    val type: TypeMirror = element.asType()

    override fun compareTo(other: Field): Int {
        return name.compareTo(other.name)
    }

    override fun toString(): String {
        return "$name:${element.asType()}"
    }
}