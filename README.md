# KaptStudy

一个用于学习 **APT / KAPT / KSP** 的 Android 示例项目。项目通过自定义注解和符号处理器，为标记过的 `Activity` 自动生成参数化跳转、参数注入和状态保存代码，串起从注解声明、KAPT Stub / KSP 符号模型，到代码生成和运行时调用的完整流程。

项目保留 KAPT 实现用于对照，并已接入 KSP 实现；`app` 当前使用 KSP 生成代码。

## 效果概览

在目标 `Activity` 上标记 `@Builder`，并在参数属性上标记 `@Required` 或 `@Optional`：

```kotlin
@Builder
class ThirdActivity : AppCompatActivity() {
    @Required
    var name: String? = null

    @Required
    var owner: String? = null

    @Optional
    var url: String? = null

    @Optional
    var createAt: Long? = null
}
```

编译后会生成同包名下的 `ThirdActivityBuilder`，可直接调用：

```kotlin
// 只传必填参数
ThirdActivityBuilder.startWithoutOptional(this, "hsm", "cy")

// 传入全部参数
ThirdActivityBuilder.start(this, "hsm", "cy", 0L, "https://www.baidu.com")

// Optional 字段不超过两个时，会额外生成按字段传参的方法
ThirdActivityBuilder.startWithOptionalUrl(this, "hsm", "cy", "https://www.baidu.com")
```

运行时库会在 `Activity` 创建时读取 `Intent` extras 并写回属性；在保存实例状态时自动把这些属性写入 `Bundle`。

## 项目结构

| 模块 | 职责 |
| --- | --- |
| `app` | 示例应用。定义带注解的 `SecondActivity`、`ThirdActivity` 并调用生成的 Builder。 |
| `annotation` | 对外提供 `@Builder`、`@Required`、`@Optional` 三个源码级注解。 |
| `compiler` | KAPT 注解处理器。扫描注解、校验目标类型，并使用 JavaPoet 生成 `*ActivityBuilder.java`。 |
| `ksp-compiler` | KSP 符号处理器。扫描 Kotlin 符号，并使用 KotlinPoet 生成 `*ActivityBuilder.kt`。 |
| `runtime` | 运行时支持库。通过 `Application.ActivityLifecycleCallbacks` 触发生成类的注入和状态保存方法。 |

关键代码入口：

- `compiler/.../BuilderProcessor.kt`：处理器入口，收集一个 `Activity` 的必填与可选字段。
- `compiler/.../activity/builder/ActivityClassBuilder.kt`：组织 Builder 类及其方法的生成。
- `ksp-compiler/.../BuilderProcessor.kt`：KSP 处理器入口；`build/ActivityClassBuilder.kt` 负责 KotlinPoet 生成。
- `runtime/.../ActivityBuilder.java`：注册生命周期回调，并通过反射调用生成类的 `inject`、`saveState`。
- `app/.../MyApp.kt`：初始化运行时库。

## 工作流程

```text
@Builder / @Required / @Optional
             │
     ┌───────┴────────┐
     ▼                ▼
KAPT 生成 Java Stub    KSP 提供 KS* 符号模型
     │                │
     ▼                ▼
JavaPoet 生成 .java    KotlinPoet 生成 .kt
     └───────┬────────┘
             ▼
        <Activity>Builder
             │
             ▼
业务代码调用生成的 start... 方法，写入 Intent extras
             │
             ▼
ActivityBuilder 生命周期回调反射调用 inject / saveState
```

KAPT 与 KSP 的生成产物分别位于：

```text
# KAPT（当前 app 未启用）
app/build/generated/source/kapt/debug/<包名>/<Activity>Builder.java

# KSP（当前 app 使用）
app/build/generated/ksp/debug/kotlin/<包名>/<Activity>Builder.kt
```

例如，本项目编译后可查看：

```text
app/build/generated/source/kapt/debug/com/cy/kaptstudy/SecondActivityBuilder.java
app/build/generated/source/kapt/debug/com/cy/kaptstudy/ThirdActivityBuilder.java
app/build/generated/ksp/debug/kotlin/com/cy/kaptstudy/SecondActivityBuilder.kt
app/build/generated/ksp/debug/kotlin/com/cy/kaptstudy/ThirdActivityBuilder.kt
```

## 注解与生成规则

### `@Builder`

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Builder
```

只能标记在类上。处理器当前仅支持 `android.app.Activity` 的子类；非 Activity 会在编译阶段报错。

### `@Required`

```kotlin
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Required
```

标记必须在跳转时提供的字段。生成的 `start`、`startWithoutOptional` 等方法会把它们作为参数，并写入 `Intent`。

### `@Optional`

```kotlin
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class Optional(
    val stringValue: String = "",
    val intValue: Int = 0,
    val floatValue: Float = 0f,
    val booleanValue: Boolean = false
)
```

标记可选字段，并可为基础类型或 `String` 指定缺省值。注入时，若 `Bundle` 中没有该键，运行时会使用注解中配置的默认值。

生成方法遵循以下规则：

- 始终生成 `start(...)`：传入必填字段和全部可选字段；
- 始终生成 `startWithoutOptional(...)`：只传必填字段；
- 可选字段数量不超过 2 时，为每个字段生成 `startWithOptionalXxx(...)`；
- 可选字段数量超过 2 时，生成 setter、`startWithOptionals(...)` 和内部的 `fillOptions(...)`，用于 Builder 式传参；
- 始终生成 `inject(Activity, Bundle)` 和 `saveState(Activity, Bundle)` 供运行时调用。

> 属性使用 Kotlin 声明时，KAPT 会在 Stub 中把属性转成私有字段及 `getXxx` / `setXxx` 方法；生成代码正是通过这些访问器写入和读取值。因此示例中的参数属性使用 `var`。

## 接入方式

### 1. 配置依赖与处理器

两种处理器实现生成相同的 `XxxActivityBuilder` 类名，因此一个 app 模块中应二选一启用，不能同时配置 `kapt(project(":compiler"))` 与 `ksp(project(":ksp-compiler"))`。

#### 使用 KAPT

应用模块需要依赖注解和运行时模块，并把处理器添加到 `kapt`：

```kotlin
plugins {
    kotlin("kapt")
}

dependencies {
    implementation(project(":annotation"))
    implementation(project(":runtime"))
    kapt(project(":compiler"))
}
```

处理器通过以下 Service Provider 文件被编译器发现：

```text
compiler/src/main/resources/META-INF/services/javax.annotation.processing.Processor
```

文件内容为处理器的全限定类名：

```text
com.cy.compiler.BuilderProcessor
```

#### 使用 KSP（当前 app 配置）

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":annotation"))
    implementation(project(":runtime"))
    ksp(project(":ksp-compiler"))
}
```

KSP 的发现文件为：

```text
ksp-compiler/src/main/resources/META-INF/services/
com.google.devtools.ksp.processing.SymbolProcessorProvider
```

文件内容是 Provider 的全限定名：

```text
com.cy.ksp.compiler.BuilderProcessorProvider
```

### 2. 初始化运行时库

在 `Application` 中注册生命周期回调：

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ActivityBuilder.INSTANCE.init(this)
    }
}
```

并在 `AndroidManifest.xml` 的 `<application>` 上配置该 `Application`。

### 3. 声明参数并调用生成类

为 Activity 与字段添加注解；重新编译后，即可从同包名代码调用自动生成的 `XxxActivityBuilder`。

## 构建与运行

环境要求：JDK 11、Android SDK（`compileSdk 36`）以及可用的 Android Studio / Gradle 环境。项目使用 Gradle 8.11.1、AGP 8.7.3 和 Kotlin 2.0.21。

```bash
# 编译 Debug 包，并触发当前启用的 KSP / KAPT 处理器生成代码
./gradlew :app:assembleDebug

# 安装到已连接设备或模拟器
./gradlew :app:installDebug

# 运行本地单元测试
./gradlew test
```

也可以直接用 Android Studio 打开项目，等待 Gradle Sync 完成后运行 `app` 配置。

## 调试注解处理器

为处理器调试时，可先在 Android Studio 创建 **Remote JVM Debug** 配置，再执行：

```bash
./gradlew clean :app:assembleDebug -Dorg.gradle.debug=true --no-daemon
```

Gradle 会等待调试器连接；连接后即可在 `BuilderProcessor` 或 JavaPoet 生成逻辑中断点查看处理轮次、KAPT Stub 和元素信息。

## 基础知识

### 注解

注解可以理解为附加在代码上的结构化元数据。它本身不直接改变运行逻辑，但编译器、注解处理器或反射框架可以读取它并据此执行相应行为。本项目的 `@Builder`、`@Required`、`@Optional` 就是在编译期被处理器读取。

#### 基本语法与限制

```kotlin
annotation class MyApiConfig(
    val url: String,
    val port: Int = 8080,
    val method: HttpMethod
)

enum class HttpMethod { GET, POST }

@MyApiConfig(url = "https://example.com", method = HttpMethod.GET)
class MyClass
```

注解参数只能是基本类型、字符串、枚举、类引用（`KClass`）、其他注解，以及它们的数组；参数不能声明为可空类型。注解类不能有主体，也不能继承其他类。

#### 元注解

元注解用于定义自定义注解的适用范围和生命周期：

- `@Target`：限制注解可以标记的位置。例如 `CLASS`、`FIELD`、`FUNCTION`；
- `@Retention(SOURCE)`：只在源码阶段存在，适用于 KAPT / KSP；
- `@Retention(BINARY)`：写入字节码，但运行时无法通过反射读取；
- `@Retention(RUNTIME)`：写入字节码，运行时可通过反射读取。

#### 在处理器中读取注解值

如果处理器能访问注解类，可通过 `Element.getAnnotation()` 读取参数：

```kotlin
val annotation = element.getAnnotation(Optional::class.java)
if (annotation != null) {
    val stringValue = annotation.stringValue
    val intValue = annotation.intValue
    val floatValue = annotation.floatValue
    val booleanValue = annotation.booleanValue
}
```

本项目的 `OptionalField` 正是根据字段类型读取这些值，并将其作为生成的注入代码的默认值。

#### 注解的编译产物

`@Builder`、`@Required` 和 `@Optional` 的**注解定义**会随 `annotation` 模块编译成 `com.cy.annotation.*.class`，供业务模块和处理器在编译时引用；但它们的使用处采用 `AnnotationRetention.SOURCE`，因此只在源码处理阶段可见，不会写入 app 的 class / dex，也不能在运行时反射读取。

本项目真正交付给 app 的处理产物是生成的 `XxxActivityBuilder` 源码：KAPT 生成 Java 文件，KSP 生成 Kotlin 文件；二者随后会被正常编译并打包。KAPT 的 Java Stub 和 KSP 的 `KSClassDeclaration`、`KSPropertyDeclaration` 都只是编译期间的中间表示，不会进入 APK。

### 注解处理器

`AbstractProcessor` 是传统 Java 注解处理器的基类。KAPT 会先为 Kotlin 源码生成 Java Stub，再让处理器像处理 Java 代码一样扫描其中的元素。

#### 核心方法

1. `init(ProcessingEnvironment)`：初始化入口。可以取得：
   - `Elements`：查询类、包、方法和字段等程序元素；
   - `Types`：比较、转换和检查 `TypeMirror`；
   - `Filer`：创建新的源文件、类文件或资源文件；
   - `Messager`：输出编译期错误、警告和提示。
2. `process(Set<TypeElement>, RoundEnvironment)`：处理器的核心。通过 `roundEnv.getElementsAnnotatedWith(...)` 找到被标记的元素，并在其中生成代码。返回 `true` 表示本处理器已消费这些注解。
3. `getSupportedAnnotationTypes()`：返回处理器关注的注解全限定名集合。
4. `getSupportedSourceVersion()`：声明支持的 Java 源码版本；通常可返回 `SourceVersion.latestSupported()`。

#### 处理轮次（Rounds）

一次编译不一定只调用一次 `process`：第一轮扫描原始源码；如果处理器生成了新的 `.java` 文件，编译器会在后续轮次扫描这些新文件；当没有新文件可生成时进入最后一轮，`roundEnv.processingOver()` 为 `true`。实际项目中应避免在后续轮次重复生成同名文件。

#### 处理器如何被发现

传统方式是在 `META-INF/services/javax.annotation.processing.Processor` 中写入处理器全限定名。本项目采用这种方式。也可以使用 Google AutoService 自动生成该服务配置文件。

### Elements、Types、Messager 与 Filer

- `Element` 描述源码中的声明：`TypeElement` 表示类或接口，`VariableElement` 表示字段、参数或枚举常量；
- `TypeMirror` 描述类型本身。可配合 `Types.isSubtype()` 检查继承关系，或用 `Types.isSameType()` 判断类型是否相同；
- `Messager.printMessage(Diagnostic.Kind.ERROR, ...)` 能令编译失败，`WARNING` 则会在构建日志中显示警告；
- `Filer` 是生成文件的最终出口。JavaPoet 的 `JavaFile.writeTo(filer)` 会通过它把源码写入构建目录。

`BuilderProcessor` 使用 `Elements` / `Types` 确认 `@Builder` 的目标是 `Activity`，用 `Messager` 报告不支持的目标或未标记 `@Builder` 的字段，最后将 JavaPoet 生成内容写入 `Filer`。

### JavaPoet

本项目使用 Palantir 维护的 JavaPoet 来生成 Java 源码。它提供 `MethodSpec`、`FieldSpec`、`TypeSpec` 和 `JavaFile` 等模型，能自动处理 import、缩进和格式化。

常用占位符：

- `$T`：类型，会自动处理导包；
- `$S`：字符串字面量，自动加引号及转义；
- `$L`：普通字面量或代码片段；
- `$N`：已声明的名称，如字段、方法或参数名。

下面是一个最小的生成示例：

```kotlin
val setAge = MethodSpec.methodBuilder("setAge")
    .addModifiers(Modifier.PUBLIC)
    .returns(ClassName.get(packageName, "UserBuilder"))
    .addParameter(Int::class.java, "age")
    .addStatement("this.age = age")
    .addStatement("return this")
    .build()

val builder = TypeSpec.classBuilder("UserBuilder")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addField(FieldSpec.builder(Int::class.java, "age", Modifier.PRIVATE).build())
    .addMethod(setAge)
    .build()

JavaFile.builder(packageName, builder).build().writeTo(filer)
```

### KSP

KSP 不生成 Java Stub，而是直接向处理器暴露 Kotlin / Java 的符号模型。处理器由 `SymbolProcessorProvider` 创建，并通过 `SymbolProcessor.process(resolver)` 在编译过程中运行：

```kotlin
class BuilderProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return BuilderProcessor(environment.logger, environment.codeGenerator)
    }
}
```

本项目使用的核心 API：

- `Resolver`：通过 `getSymbolsWithAnnotation("全限定注解名")` 查询标记过的符号；
- `KSAnnotated.validate()`：检查该符号的类型是否已解析。未通过的符号应从 `process` 返回，交给后续轮次重试；
- `KSClassDeclaration` / `KSPropertyDeclaration`：分别表示类和属性。`getAllSuperTypes()` 可用于检查是否继承 `Activity`；
- `KSAnnotation.arguments`：读取 `@Optional` 的默认值参数；
- `CodeGenerator`：创建生成文件；`Dependencies` 声明生成文件依赖哪些源文件，影响增量构建；
- `KSPLogger`：输出诊断信息。生产代码通常应使用 `error` 报错，并避免在正常构建中持续输出 `warn`。

KSP 配合 KotlinPoet 时，`toClassName()`、`toTypeName()` 可将 KSP 的符号类型转换为 KotlinPoet 类型；`FileSpec.writeTo(codeGenerator, dependencies)` 则将生成的 Kotlin 源码交给 KSP。

## 当前边界与审查建议

这是学习性质的实现，适合理解 KAPT 与 KSP 的代码生成链路。生产化前仍应补齐字段类型校验、可空性与 `Bundle` 支持范围处理、混淆规则、反射失败的错误策略，以及自动化测试等能力。

当前审查得到的重点事项：

- KAPT 版本中，超过两个可选字段时的 `startWithOptionals(...)` 会先调用 `startActivity`、后调用 `fillOptions(...)`；这些可选参数不会随启动 Intent 生效。KSP 版本已按正确顺序先填充、后启动；
- KSP 的 `process` 当前会跳过 `validate()` 失败的符号并返回空列表。应将未解析符号返回给 KSP，避免跨模块或生成类型场景下遗漏处理；
- `Field` / `OptionalField` 的 `TreeSet` 比较逻辑应改为同一个稳定 Comparator（先按必填/可选分组，再按字段名排序），并在同一属性同时标记 `@Required` 与 `@Optional` 时明确报错；
- 运行时会对每个 Activity 反射查找 Builder。未标记 `@Builder` 的 Activity 会触发并打印 `ClassNotFoundException`；建议将“没有生成类”视为正常分支，并缓存已解析的方法，减少反射与日志噪声；
- `@Required` 当前只决定生成方法的参数列表，若属性或参数可空，调用方仍可传入 `null`。应根据期望选择禁止可空必填参数，或在运行时检测缺失 key；
- `Intent` / `Bundle` 支持的类型有限。处理器应在生成前校验字段类型并给出带源码位置的错误，而不是让错误在生成代码编译时才出现；
- 启用代码压缩时，需要为反射调用的 `XxxActivityBuilder.inject/saveState` 增加 keep 规则，或改为不依赖反射的注册方案。
