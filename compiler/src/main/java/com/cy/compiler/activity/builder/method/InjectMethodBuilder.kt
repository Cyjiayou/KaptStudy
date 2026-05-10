package com.cy.compiler.activity.builder.method

import com.cy.compiler.activity.ActivityClass
import com.cy.compiler.activity.entity.OptionalField
import com.cy.compiler.types.Types
import com.cy.compiler.utils.capitalizeFirstLetter
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class InjectMethodBuilder(private val activityClass: ActivityClass) {

    fun build(classBuilder: TypeSpec.Builder) {
        val methodBuilder = MethodSpec.methodBuilder("inject")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(Types.getActivityTypeName(), "activity")
            .addParameter(Types.getBundleTypeName(), "bundle")
            .returns(TypeName.VOID)
            .beginControlFlow("if (activity instanceof \$T)", activityClass.element.asType())
            .addStatement("\$T typedActivity = (\$T) activity", activityClass.element.asType(), activityClass.element.asType())
            .addStatement("\$T extras = bundle == null ? typedActivity.getIntent().getExtras(): bundle", Types.getBundleTypeName())
            .beginControlFlow("if (extras != null)")

        // KAPT 会将属性改成 private，并生成对应的 get 和 set 方法，见 stubs 目录
        activityClass.fields.forEach { field ->
            if (field is OptionalField) {
                methodBuilder.addStatement(
                    "\$T ${field.name}Value = \$T.<\$T>get(extras, \$S, ${field.defaultValue})",
                    TypeName.get(field.type),
                    Types.getBundleUtilsTypeName(),
                    TypeName.get(field.type),
                    field.name
                )
            } else {
                methodBuilder.addStatement(
                    "\$T ${field.name}Value = \$T.<\$T>get(extras, \$S)",
                    TypeName.get(field.type),
                    Types.getBundleUtilsTypeName(),
                    TypeName.get(field.type),
                    field.name
                )
            }
            methodBuilder.addStatement("typedActivity.set${field.name.capitalizeFirstLetter()}(${field.name}Value)")
        }
        methodBuilder.endControlFlow().endControlFlow()

        classBuilder.addMethod(methodBuilder.build())
    }
}