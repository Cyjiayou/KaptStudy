package com.cy.compiler.utils

import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

object AptUtils {

    lateinit var messager: Messager
    lateinit var typeUtils: Types
    lateinit var elementUtils: Elements
    lateinit var filer: Filer

    fun init(env: ProcessingEnvironment) {
        messager = env.messager
        typeUtils = env.typeUtils
        elementUtils = env.elementUtils
        filer = env.filer
    }

    fun isSubclassOf(subElement: Element, className: String): Boolean {
        val element = elementUtils.getTypeElement(className)
        return typeUtils.isSubtype(subElement.asType(), element.asType())
    }
}