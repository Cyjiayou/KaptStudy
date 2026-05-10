package com.cy.compiler.activity

import com.cy.compiler.activity.builder.ActivityClassBuilder
import com.cy.compiler.activity.entity.Field
import com.cy.compiler.utils.AptUtils
import java.util.TreeSet
import javax.lang.model.element.TypeElement

/**
 * Activity 类存储对象
 */
class ActivityClass(val element: TypeElement) {

    val name = element.simpleName.toString()

    val packageName = AptUtils.elementUtils.getPackageOf(element).toString()

    // 这里为什么要用 TreeSet，主要是想将 Optional 和 Required 标注的属性给区分开
    val fields = TreeSet<Field>()

    val builder = ActivityClassBuilder(this)

    override fun toString(): String {
        return "${packageName}.${name}[${fields.joinToString()}]"
    }

}