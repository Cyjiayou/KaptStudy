package com.cy.compiler

import com.cy.annotation.Builder
import com.cy.annotation.Optional
import com.cy.annotation.Required
import com.cy.compiler.activity.ActivityClass
import com.cy.compiler.activity.entity.Field
import com.cy.compiler.activity.entity.OptionalField
import com.cy.compiler.utils.AptUtils
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic

/**
 * 注解处理器
 */
class BuilderProcessor: AbstractProcessor() {

    private lateinit var messager: Messager

    private lateinit var filer: Filer

    private val annotations = setOf(Builder::class.java, Optional::class.java, Required::class.java)

    override fun init(env: ProcessingEnvironment) {
        messager = env.messager
        filer = env.filer
        AptUtils.init(env)
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return annotations.map { it.canonicalName }.toSet()
    }


    override fun process(
        annotations: Set<TypeElement?>?,
        env: RoundEnvironment
    ): Boolean {
        messager.printMessage(Diagnostic.Kind.WARNING, "start process")

        // 用于保存注解信息
        val activityClasses = HashMap<Element, ActivityClass>()

        // 从 env 中获取 Builder 注解标注的对象
        env.getElementsAnnotatedWith(Builder::class.java)
            .filter { it.kind == ElementKind.CLASS }
            .forEach { element ->
                if (AptUtils.isSubclassOf(element, "android.app.Activity")) {
                    activityClasses[element] = ActivityClass(element as TypeElement)
                } else {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Unsupported typeElement: ${element.simpleName}")
                }
            }

        // 从 env 中获取 Required 注解标注的对象，由于 Required 修饰的是 field，添加到对应的 Class 中
        env.getElementsAnnotatedWith(Required::class.java)
            .filter { it.kind == ElementKind.FIELD }
            .forEach { element ->
                // enclosingElement 用于寻找当前元素的上级
                val enclosingElement = element.enclosingElement
                activityClasses[enclosingElement]?.fields?.add(Field(element as VariableElement))
                    ?:  messager.printMessage(Diagnostic.Kind.ERROR, "Field $element annotated as Required while ${element.enclosingElement} not annotated")
            }

        env.getElementsAnnotatedWith(Optional::class.java)
            .filter { it.kind == ElementKind.FIELD }
            .forEach { element ->
                val enclosingElement = element.enclosingElement
                activityClasses[enclosingElement]?.fields?.add(OptionalField(element as VariableElement))
                    ?:  messager.printMessage(Diagnostic.Kind.ERROR, "Field $element annotated as Optional while ${element.enclosingElement} not annotated")
            }

        // 输出 activityClasses 的结果，重写 toString
        activityClasses.values.forEach {
            messager.printMessage(Diagnostic.Kind.WARNING, it.toString())
            // 每个 ActivityClass 构建文件
            it.builder.build(filer)
        }

        return true
    }
}