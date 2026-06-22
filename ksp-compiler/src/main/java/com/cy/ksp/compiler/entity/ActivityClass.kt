package com.cy.ksp.compiler.entity

import com.cy.ksp.compiler.build.ActivityClassBuilder
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ksp.toClassName
import java.util.TreeSet

class ActivityClass(private val clazz: KSClassDeclaration) {

    val fields = TreeSet<Field>()

    val name = clazz.simpleName.asString()

    val packageName = clazz.packageName.asString()

    val containingFile = clazz.containingFile

    val builder = ActivityClassBuilder(this)

    val typeName = clazz.toClassName()

    override fun toString(): String {
        return "${packageName}.${name}[${fields.joinToString()}]"
    }
}
