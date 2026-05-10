package com.cy.compiler.activity.builder.method

import com.cy.compiler.activity.ActivityClass
import com.cy.compiler.activity.entity.Field
import com.cy.compiler.activity.entity.OptionalField
import com.cy.compiler.types.Types
import com.cy.compiler.utils.capitalizeFirstLetter
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.MethodSpec
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.TypeSpec
import javax.lang.model.element.Modifier

class StartMethodBuilder(private val activityClass: ActivityClass) {

    private val optionalFields: List<Field>

    private val requiredFields: List<Field>

    init {
        // 通过 partition 将 TreeSet 中的元素进行划分
        val pair = activityClass.fields.partition { it is OptionalField }
        optionalFields = pair.first
        requiredFields = pair.second
    }

    fun build(classBuilder: TypeSpec.Builder) {


        // 构建 start 方法
        val start = buildStartMethod("start", optionalFields)

        // 构建 startWithoutOptional
        val startMethodWithoutOptional = buildStartMethod("startWithoutOptional", null)

        classBuilder.addMethod(start.build())
        classBuilder.addMethod(startMethodWithoutOptional.build())

        // 如果 optional 字段少，就为他们生成对应的方法
        if (optionalFields.size <= 2) {
            optionalFields.forEach { field ->
                classBuilder.addMethod(
                    buildStartMethod(
                        "startWithOptional${field.name.capitalizeFirstLetter()}",
                        listOf(field)
                    ).build()
                )
            }
        } else {
            optionalFields.forEach { field ->
                classBuilder.addField(TypeName.get(field.type), field.name, Modifier.PRIVATE)
                classBuilder.addMethod(
                    buildSetMethod(
                        "set${field.name.capitalizeFirstLetter()}",
                        field
                    ).build()
                )
            }
            classBuilder.addMethod(buildFillOptionsMethod(optionalFields).build())
            classBuilder.addMethod(
                buildStartMethod(
                    "startWithOptionals",
                    null,
                    false
                ).addStatement("fillOptions(intent)")
                    .build()
            )
        }
    }

    fun buildStartMethod(
        name: String,
        optionalFields: List<Field>?,
        isStaticMethod: Boolean = true
    ): MethodSpec.Builder {
        val startMethod = MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Types.getContextTypeName(), "context")
            .returns(TypeName.VOID)
            .addStatement(
                "\$T intent = new \$T(context, \$T.class)",
                Types.getIntentTypeName(),
                Types.getIntentTypeName(),
                activityClass.element
            )

        if (isStaticMethod) {
            startMethod.addModifiers(Modifier.STATIC)
        }

        requiredFields.forEach { field ->
            startMethod.addParameter(TypeName.get(field.type), field.name)
            startMethod.addStatement("intent.putExtra(\$S, ${field.name})", field.name)
        }

        optionalFields?.forEach { field ->
            startMethod.addParameter(TypeName.get(field.type), field.name)
            startMethod.addStatement("intent.putExtra(\$S, ${field.name})", field.name)
        }

        startMethod.addStatement("\$T.INSTANCE.startActivity(context, intent)", ClassName.get("com.cy.runtime", "ActivityBuilder"))

        return startMethod
    }


    fun buildSetMethod(name: String, field: Field): MethodSpec.Builder {
        val classNameType = ClassName.get(activityClass.packageName, activityClass.name + "Builder")
        val setMethod = MethodSpec.methodBuilder(name)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(TypeName.get(field.type), field.name)
            .returns(classNameType)
            .addStatement("this.${field.name} = ${field.name}")
            .addStatement("return this")

        return setMethod
    }

    fun buildFillOptionsMethod(fields: List<Field>): MethodSpec.Builder {
        val fillOptionsMethod = MethodSpec.methodBuilder("fillOptions")
            .addModifiers(Modifier.PRIVATE)
            .returns(TypeName.VOID)
            .addParameter(Types.getIntentTypeName(), "intent")

        fields.forEach { field ->
            fillOptionsMethod.beginControlFlow("if (this.${field.name} != null)")
                .addStatement("intent.putExtra(\$S, this.${field.name})", field.name)
                .endControlFlow()
        }

        return fillOptionsMethod
    }
}