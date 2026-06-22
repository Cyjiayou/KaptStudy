package com.cy.ksp.compiler

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

fun KSPropertyDeclaration.findAnnotation(annotationName: String): KSAnnotation? {
    return annotations.firstOrNull { annotation ->
        annotation.annotationType
            .resolve()
            .declaration
            .qualifiedName
            ?.asString() == annotationName
    }
}

fun String.capitalizeFirstLetter(): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
