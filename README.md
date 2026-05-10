# Apt/Kapt 学习记录
由于工作需要，学习 Apt/Kapt 基础知识，后续也会补充 ksp 的基础知识。主要分为三个部分：**基础知识** 和 **项目实践**。

# 基础知识

## 注解
自定义注解在 Kotlin 中像是给代码定义一套“元数据标准”。下面从**语法结构、元注解**两个层面来讲解。

### 基本语法

```kotlin
annotation class MyApiConfig (
    val url: String,           // 支持基本类型
    val prot: Int = 8080,        // 支持默认值
    val method: HttpMethod    // 支持枚举
)

enum class HttpMethod {
    GET,
    POST
}

@MyApiConfig(url = "123", method = HttpMethod.GET)
class MyClass {
}
```
**限制条件**
- 注解的参数只能是：基本类型、字符串、枚举、类引用（KClass）、其他注解以及这些类型的数组；
- 参数不能为可空类型（不能是 String?）;
- 注解类不能有主体，也不能继承其他类；


### 配置注解的“行为”（元注解）
元注解（Meta-Annotations）是用来修饰”注解“的注解，他们定义了自定义的标签能贴在哪，活多久等等。

**@Target：标签贴在哪**

主要用于限制注解只能给类用，还是只能给属性等等；
- AnnotationTarget.CLASS: 类、接口、对象等；
- AnnotationTarget.FIELD: 字段；
- AnnotationTarget.FUNCTION: 函数（不包含构造函数）；

**@Rentation：活多久**
- AnnotationRetention.SOURCE: 只存在于源码，编译后消失；
- AnnotationRetention.BINARY: 存储在编译后的字节码中，但运行时不可见；
- AnnotationRetention.RUNTIME: 存储在字节码中，且**运行时可以通过反射读取**；

### 获取注解值
有时候需要在注解处理器中获取注解的值。
如果能直接访问到注解类，可以直接调用 `getAnnotation()`
```kotlin
val annotation = element.getAnnotation(Optional::class.java)

if (annotation != null) {
    val sValue = annotation.stringValue
    val iValue = annotation.intValue
    val fValue = annotation.floatValue
    val bValue = annotation.booleanValue

    // 使用这些值...
}
```

## 注解处理器
### AbstractProcessor
`AbstractProcessor` 是注解处理器的心脏，所有自定义注解处理器（APT）必须继承的基类，定义了从扫描注解到生成代码的整个生命周期；

#### 核心四大方法
1. `init(ProcessingEnvironment env)`
初始化方法。可以从 `env` 中获取四个工具类：
- `Elements`: 用来处理程序元素（类、方法、字段）；
- `Types`: 用来处理类型镜像（父类、接口、泛型）；
- `Filer`: 用来创建新的源文件（JavaPoet 的最终目的地）；
- `Messager`: 用来在编译期打印日志（报错、警告）；

2. `process(Set<? extends TypeElement> annotations, RoundEnviroment roundEnv)`
**核心逻辑所在**。编译器会多次调用该方法。
- 可以通过 `roundEnv.getElementsAnnotatedWith()` 找到所有被注解标记过的代码；
- 返回值`boolean`: 如果返回 true，表示这些注解已被处理，后续的处理器就不再处理他们了；

3. `getSupportedAnnotationType()`:
告诉编译器，这个处理器是为那些注解服务的，返回注解的全类名集合；

4. `getSupportedSourceVersion()`:
指定支持的 Java 版本，通过返回 `SourceVersion.latestSupported()`

#### 注解处理的“轮次”（Rounds）
1. 第一轮：编译器扫描源码，发现注解，调用 process。生成了新的 .java 文件。
2. 后续轮次：由于生成了新代码，新代码里可能也有注解。编译器会再次调用 process 处理新生成的内容。
3. 最后一轮：当没有新的注解被发现，且没有新文件生成时，process 最后一次运行，此时 roundEnv.processingOver() 为 true。

#### 编译器如何找到处理器
**传统方式**：在 `resources/META-INF/services/javax.annotation.processing.Processor` 文件中填入处理器哦的全类名；

**现代方案**：使用 Google 开发的 `@AutoService` 注解，它会自动生成上述的配置文件
```kotlin
@AutoService(Processor::class)
class MyBuilderProcessor: AbstractProcessor() {
    // ...
}
```

### Types/ClassName


### Elements

### Messager

### Filer

### 调试方法
1. 在 Android Studio 中，点击菜单栏的 Run -> Edit Configuration；
2. 点击 + 号，选择 Remote JVM Debug；

<img src="/Users/caoyang/Library/Application Support/typora-user-images/image-20260505195810403.png" alt="image-20260505195810403" style="zoom:67%;" />

1. 设置断点；
2. 执行 `./gradlew clean :app:assembleDebug -Dorg.gradle.debug=true --no-daemon` 命令；
3. 点击 Debug 按钮运行配置的 Configuration

## 文件写入（JavaPoet/KotlinPoet）
### JavaPoet
JavaPoet 是专门用于生成 `.java` 源文件的，JavaPoet 提供了流式 API，能自动处理**导包（Import）、缩进和代码格式化**；

**关键类**
- `MethodSpec`：代表一个方法或构造函数；
- `FieldSpec`：代表一个成员变量；
- `TypeSpec`：代表一个类、接口、枚举或注解；
- `JavaFile`：代表整个 Java 文件，负责确定包名和写入磁盘；

**基础语法：占位符**
JavaPoet 不使用简单的字符串拼接，而是使用特殊的占位符来保证代码的正确性：
- **`$L` (Literals)**：字面量。比如数字、字符串（不带引号）、或者一段代码片段。
- **`$S` (Strings)**：字符串。它会自动帮你加上双引号，并处理转义字符。
- **`$T` (Types)**：类型。这是最强大的功能！你传入一个 `Class` 或 `TypeName`，它会**自动帮你生成 `import` 语句**。
- **`$N` (Names)**：引用。用于引用你定义的另一个变量或方法名。

**示例**
```kotlin
// 1. 定义方法 (setAge)
val setAgeMethod = MethodSpec.methodBuilder("setAge")
    .addModifiers(Modifier.PUBLIC)
    .returns(ClassName.get(packageName, "UserBuilder")) // 返回 Builder 自身
    .addParameter(Int::class.java, "age") // 参数类型和名字
    .addStatement("this.age = age") // 自动加分号
    .addStatement("return this")
    .build()

// 2. 定义字段 (age)
val ageField = FieldSpec.builder(Int::class.java, "age")
    .addModifiers(Modifier.PRIVATE)
    .build()

// 3. 定义类 (UserBuilder)
val builderClass = TypeSpec.classBuilder("UserBuilder")
    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
    .addField(ageField)
    .addMethod(setAgeMethod)
    .build()

// 4. 生成文件并写入 Filer
val javaFile = JavaFile.builder(packageName, builderClass)
    .skipJavaLangImports(true) // 自动跳过 java.lang 的导入
    .build()

javaFile.writeTo(filer)
```

# 实战
