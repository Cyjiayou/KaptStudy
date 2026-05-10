package com.cy.compiler.types

import com.cy.compiler.utils.AptUtils
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.TypeName
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

object Types {

    fun getContextTypeName(): TypeName {
        return ClassName.get("android.content", "Context")
    }


    fun getIntentTypeName(): TypeName {
        return ClassName.get("android.content", "Intent")
    }

    fun getActivityTypeName(): TypeName {
        return ClassName.get("android.app", "Activity")
    }

    fun getBundleTypeName(): TypeName {
        return ClassName.get("android.os", "Bundle")
    }

    fun getBundleUtilsTypeName(): TypeName {
        return ClassName.get("com.cy.runtime.utils", "BundleUtils")
    }

    fun isSameTypeOf(element: Element, name: String): Boolean {
        return AptUtils.typeUtils.isSameType(
            element.asType(),
            AptUtils.elementUtils.getTypeElement(name).asType()
        )
    }
}