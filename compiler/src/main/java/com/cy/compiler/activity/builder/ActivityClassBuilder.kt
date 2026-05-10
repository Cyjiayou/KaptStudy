package com.cy.compiler.activity.builder

import com.cy.compiler.activity.ActivityClass
import com.cy.compiler.activity.builder.method.InjectMethodBuilder
import com.cy.compiler.activity.builder.method.SaveStateMethodBuilder
import com.cy.compiler.activity.builder.method.StartMethodBuilder
import com.palantir.javapoet.JavaFile
import com.palantir.javapoet.TypeSpec
import javax.annotation.processing.Filer
import javax.lang.model.element.Modifier

/**
 * 生成 ActivityBuilder 文件
 */
class ActivityClassBuilder(private val activityClass: ActivityClass) {

    companion object {
        const val POSIX  = "Builder"
    }


    fun build(filer: Filer) {
        // 构造 class
        val classBuilder = TypeSpec.classBuilder("${activityClass.name}${POSIX}")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)

        // 构造方法
        StartMethodBuilder(activityClass).build(classBuilder)
        InjectMethodBuilder(activityClass).build(classBuilder)
        SaveStateMethodBuilder(activityClass).build(classBuilder)
        writeToFile(filer, classBuilder)
    }

    private fun writeToFile(filer: Filer, classBuilder: TypeSpec.Builder) {
        try {
            val javaFile = JavaFile.builder(activityClass.packageName, classBuilder.build()).build()
            javaFile.writeTo(filer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
