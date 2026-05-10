package com.cy.compiler.activity.builder.method

import com.cy.compiler.activity.ActivityClass
import com.cy.compiler.types.Types
import com.cy.compiler.utils.capitalizeFirstLetter
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class SaveStateMethodBuilder(private val activityClass: ActivityClass) {

    fun build(classBuilder: TypeSpec.Builder) {
        val methodBuilder = MethodSpec.methodBuilder("saveState")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(Types.getActivityTypeName(), "activity")
            .addParameter(Types.getBundleTypeName(), "outState")
            .returns(TypeName.VOID)
            .beginControlFlow("if (activity instanceof \$T)", activityClass.element.asType())
            .addStatement("\$T typedActivity = (\$T) activity", activityClass.element.asType(), activityClass.element.asType())
            .addStatement("\$T intent = new \$T()", Types.getIntentTypeName(), Types.getIntentTypeName())

        activityClass.fields.forEach { field ->
            methodBuilder.addStatement("intent.putExtra(\$S, typedActivity.get${field.name.capitalizeFirstLetter()}())", field.name)
        }

        methodBuilder.addStatement("outState.putAll(intent.getExtras())")
            .endControlFlow()

        classBuilder.addMethod(methodBuilder.build())
    }
}