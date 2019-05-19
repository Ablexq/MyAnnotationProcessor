
# 参考：

[悦动圈：Java Annotation最佳入门实践](https://joyrun.github.io/2016/07/18/java-annotation/)

[悦动圈：Android APT（编译时代码生成）最佳实践](https://joyrun.github.io/2016/07/19/AptHelloWorld/)

[java注解与APT技术](https://blog.csdn.net/fengxingzhe001/article/details/78520298)

[annotationProcessor 自动生成代码(上)](https://www.jianshu.com/p/c8c113a1b975)

[annotationProcessor 自动生成代码(下)](https://www.jianshu.com/p/676537664d04)

[java.util.Collections.singleton()的一些理解](https://blog.csdn.net/pmdream/article/details/80525451)

SOURCE(.java)                   --【编译】-->  
CLASS(.class（字节码文件）)     --【运行】--> 
RUNTIME


# 处理注解的三种方式：

> 1.反射（运行时执行，耗费性能）
> 2.gradle版本2.2之前使用android-apt（编译期执行，但过时）
> 3.gradle版本2.2之后使用Google内置的annotationProcessor（编译期执行，强烈推荐使用）


<font color="#ff0000">annotationProcessor 和 android-apt（Annotation Processing Tool）的功能是一样的，它们是替代关系，且都是编译期执行。</font>

android-apt是由一位开发者自己开发的apt框架，源代码托管在[这里](https://bitbucket.org/hvisser/android-apt)，
随着<font color="#ff0000">Android Gradle 插件 2.2 版本</font>的发布，
Android 官方提供了 annotationProcessor 来代替 android-apt ，
annotationProcessor同时支持 javac 和 jack 编译方式，而android-apt只支持 javac 方式。
同时android-apt作者宣布不在维护，当然目前android-apt仍然可以正常运行，如果你没有想支持 jack 编译方式的话，可以继续使用 android-apt。


#### apt简介

APT(Annotation Processing Tool)是一种处理注解的工具,它对源代码文件进行检测找出其中的Annotation，根据注解自动生成代码。 
Annotation处理器在处理Annotation时可以根据源文件中的Annotation生成额外的源文件和其它的文件(文件具体内容由Annotation处理器的编写者决定),
APT还会编译生成的源文件和原来的源文件，将它们一起生成class文件。


####  使用 android-apt 依赖示例

添加android-apt到Project下的build.gradle中
``` 
buildscript {
    repositories {
      mavenCentral()
    }
    dependencies {
        //1.Android Gradle 插件版本2.2以下： 
        classpath 'com.android.tools.build:gradle:1.3.0'
        //2.android-apt
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
    }
}
```

在Module中build.gradle的配置
``` 
apply plugin: 'com.android.application'
//3.添加android-apt插件
apply plugin: 'com.neenbedankt.android-apt'

dependencies {
     //4.依赖注解编译器
     apt 'com.squareup.dagger:dagger-compiler:1.1.0'
     compile 'com.squareup.dagger:dagger:1.1.0'
}
```
#### 使用 annotationProcessor 依赖示例：

将以上 android-apt 示例切换 annotationProcessor 如下：

``` 
buildscript {
    repositories {
      mavenCentral()
    }
    dependencies {
        //1.Android Gradle 插件版本升级到 2.2 及以上
        classpath 'com.android.tools.build:gradle:2.3.0'
        //2.删掉android-apt
        //classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
    }
}
```

在Module中build.gradle的配置
``` 

apply plugin: 'com.android.application'
//3.删掉android-apt插件
//apply plugin: 'com.neenbedankt.android-apt'

dependencies {
     //4.使用 annotationProcessor 替换 apt 依赖注解编译器
     //              apt 'com.squareup.dagger:dagger-compiler:1.1.0'
     annotationProcessor 'com.squareup.dagger:dagger-compiler:1.1.0'
     compile 'com.squareup.dagger:dagger:1.1.0'
}
```
# annotationProcessor 简介及使用：

annotationProcessor 的使用大概分为两部分：annotation和annotation-compiler。
总体原理是，我们【定义 annotation 和 annotation-compiler 】，然后在合适的地方【使用annotation】。
当编译器编译到我们使用annotation的地方时，便会执行【annotation-compiler生成相应的代码】。
通过annotation的定义位置和相关参数，我们可以生成不同的代码。

#### 定义 annotation
首先我们新建Java-Library，并定义注解类：

``` 
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface DoctorInterface {
}
```
<font color="#ff0000">注意是 ：
Retention(RetentionPolicy.CLASS) </font>

#### 定义 annotation-compiler
我们再新建一个Java-Library。

``` 
apply plugin: 'java-library'

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'com.google.auto.service:auto-service:1.0-rc4' //自动注册注解处理器
    implementation 'com.squareup:javapoet:1.8.0' //javapoet代码生成框架
    implementation project(':router-annotation') //依赖注解模块
}

sourceCompatibility = "1.7"
targetCompatibility = "1.7"

```

AutoService 主要的作用是注解 processor 类，并对其生成 META-INF 的配置信息。
JavaPoet 这个库的主要作用就是帮助我们通过类调用的形式来生成代码。


实现注解DoctorInterface的意义：

``` 
//自动注册（必须）
@AutoService(Processor.class)
//@SupportedSourceVersion(SourceVersion.RELEASE_7)//指定Java版本
//@SupportedAnnotationTypes("com.example.annotationlib.DoctorInterface")//指定注解
public class DoctorInterfaceProcessor extends AbstractProcessor {

    private Filer filer;//文件相关
    private Messager messager;//日志相关
    private Elements elementUtils; //元素相关

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
        elementUtils = processingEnvironment.getElementUtils();
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
//        Set<String> set = new HashSet<>();
//        set.add(DoctorInterface.class.getCanonicalName());
//        return set;
        return Collections.singleton(DoctorInterface.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        MethodSpec main = MethodSpec.methodBuilder("main")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(String[].class, "args")
                .addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
                .build();
        TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(main)
                .build();
        JavaFile javaFile = JavaFile.builder("com.example.helloworld", helloWorld)
                .build();
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
```
annotations 获取注解集合
RoundEnvironment 获取被注解的类
ProcessingEnvironment 用于生成代码

TypeElement 是Element的子类，我们通过它获取注解的名称、参数等等。
Filer 可以用来创建新源、类或辅助文件。

JavaPoet为我们提供了方法、类、类注释等标准格式代码的创建方式。
MethodSpec是方法块，
TypeSpec是类型块，
JavaFile是Java文件。

#### 使用 annotation 和 annotation-compiler

在自己的module中， 依赖 annotation 和 annotation-compiler 模块：
``` 
implementation project(path: ':annotationlib')
annotationProcessor project(path: ':annotationcompilelib')
```
使用注解，重新rebuild：
```  
@DoctorInterface
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
```
点击Android Studio的ReBuild Project，可以在在app的 build/generated/source/apt目录下，即可看到生成的代码。


# 其他

``` 
public interface RoundEnvironment {
    boolean processingOver();

    boolean errorRaised();

    Set<? extends Element> getRootElements();

    Set<? extends Element> getElementsAnnotatedWith(TypeElement var1);

    Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> var1);
}

```

``` 
public interface TypeElement extends Element, Parameterizable, QualifiedNameable {
    List<? extends Element> getEnclosedElements();

    NestingKind getNestingKind();

    Name getQualifiedName();

    Name getSimpleName();

    TypeMirror getSuperclass();

    List<? extends TypeMirror> getInterfaces();

    List<? extends TypeParameterElement> getTypeParameters();

    Element getEnclosingElement();
}

```
# Provided 和annotationProcessor区别

参考：[你必须知道的APT、annotationProcessor、android-apt、Provided、自定义注解](https://blog.csdn.net/xx326664162/article/details/68490059)

#### annotationProcessor

只在编译的时候执行依赖的库，但是库最终不打包到apk中，

编译库中的代码没有直接使用的意义，也没有提供开放的api调用，最终的目的是得到编译库中生成的文件，供我们调用。

#### Provided

Provided 虽然也是编译时执行，最终不会打包到apk中，但是跟apt/annotationProcessor有着根本的不同。

> A 、B、C都是Library。 
A依赖了C，B也依赖了C 
App需要同时使用A和B 
那么其中A（或者B）可以修改与C的依赖关系为Provided

A这个Library实际上还是要用到C的，只不过它知道B那里也有一个C，自己再带一个就显得多余了，等app开始运行的时候，
A就可以通过B得到C，也就是两人公用这个C。所以自己就在和B汇合之前，假设自己有C。如果运行的时候没有C，肯定就要崩溃了。

总结一下，Provided是间接的得到了依赖的Library，运行的时候必须要保证这个Library的存在，否则就会崩溃，起到了避免依赖重复资源的作用。





