package com.cy.compiler.activity.entity

import com.cy.annotation.Optional
import com.cy.compiler.types.Types
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeKind

class OptionalField(element: VariableElement): Field(element) {

    var defaultValue: Any? = null
        private set

    init {
        val optional = element.getAnnotation(Optional::class.java)
        when (element.asType().kind) {
            TypeKind.BOOLEAN -> defaultValue = optional?.booleanValue
            TypeKind.BYTE,
            TypeKind.SHORT,
            TypeKind.INT,
            TypeKind.LONG,
            TypeKind.CHAR -> defaultValue = optional?.intValue
            TypeKind.FLOAT,
            TypeKind.DOUBLE -> defaultValue = optional?.floatValue
            else -> {
                if (Types.isSameTypeOf(element, "java.lang.String")) {
                    defaultValue = """"${optional?.stringValue}""""
                }
            }
        }
    }

    /**
     * 如果比较的两个都是 OptionalField，则正常排序；否则 RequiredField 排在 OptionalField 前面
     */
    override fun compareTo(other: Field): Int {
        if (other is OptionalField) {
            return super.compareTo(other)
        }
        return 1
    }
}