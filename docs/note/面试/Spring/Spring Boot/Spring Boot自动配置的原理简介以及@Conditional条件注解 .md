[原文地址](https://juejin.cn/post/7113547738859438117)
> 基于Spring Boot 2.x详细介绍了Spring Boot自动配置的原理，以及@Conditional系列条件注解。

## 1 @SpringBootApplication自动配置原理

**@SpringBootApplication是一个组合注解，主要由@ComponentScan、@SpringBootConfiguration、@EnableAutoConfiguration这三个注解组成。@EnableAutoConfiguration是Spring Boot实现自动配置的关键注解。**

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/48aae4160a7c4ae0b848ea344b49d2b3~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp)

**@ComponentScan用于扫描指定包及子包路径，并将符合条件的组件类注册到Spring容器中** 。默认的包`路径`是当前@ComponentScan所在的包及其子包。默认的过滤条件是，如果类上具有包括@Component、@Service、@Repository、@Controller、@Configuration等注解，那么该类作为组件类而被注册到容器中，也可以通过指定includeFilters和excludeFilters属性来自定义条件。

**@SpringBootConfiguration用于声明当前类是一个Spring Boot配置类，具有和@Configuration注解同样的作用**。但应用程序中可以添加N个@Configuration注解的配置类，但是只能存在`一个`@SpringBootConfiguration注解的配置类。推荐使用@SpringBootConfiguration来表示作为一个Boot应用程序的启动类。

**@EnableAutoConfiguration用于声明在程序启动时，将会自动加载Spring Boot默认的配置。这是实现自动配置的关键注解。**

@EnableAutoConfiguration注解内部主要是借助 `@Import` 注解引入的`AutoConfigurationImportSelector`类来完成其功能。

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/dfc6fae0f5c8460ead27519864051049~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp) 而在AutoConfigurationImportSelector类中再借助`SpringFactoriesLoader`工具类获取**所有**引入的jar包中和当前类路径下的**META-INF/spring.factories**文件。

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/18062cba4bd04ee3bf81a9b441de2745~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp) **spring.factories 文件必须是 Properties 格式，其中 key 是接口或抽象类的完全限定名称，value 是逗号分隔的实现类名称列表。SpringFactoriesLoader会将文件里面的自动配置类以及工厂类加载到Spring容器中，从而实现自动加载。这可以看作一种特殊的SPI机制。**

其中，**与常见的自动配置类相关**的是位于`spring-boot-autoconfigure.jar`下的`/META-INF/spring.factories`文件中的配置信息，其中名为“`org.springframework.boot.autoconfigure.EnableAutoConfiguration`”的key对应的value就是一系列自动配置类的全路径名。

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/bb2928a825f64249b0f72888ca4857e4~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp) Spring将会拆分这个value为一个全路径名集合，但他并不会将这些配置类全都加载，而是会将`符合规则`的自动配置类加载到Spring 容器中。

在过滤的时候，将会检查类上的`Conditional系列注解`，@ConditionalOnClass和@ConditionalOnMissingClass、@ConditionalOnWebApplication和@ConditionalOnNotWebApplication、@ConditionalOnBean和@ConditionalOnMissingBean和@ConditionalOnSingleCandidate注解（如果存在)，只有符合规则的自动配置类才会注册到容器中。

我们也可以通过@SpringBootApplication的`exclude和excludeName`属性指定排除某些自动配置类的注册，这样Spring Boot就不会自动注册某些配置。

**这些自动配置类基本上都是使用@Configuration注解标注，并且其内部有一系列的@Bean方法或者同样被@Configuration注解标注的内部类。当这些自动配置类被加载到容器中之后，它们内部的@Bean方法或者内部类将会被解析，就有可能帮助我们进行一系列自动化的配置，将配置信息注册到Spring 容器中。**

**以上就是Spring Boot自动配置的原理，实际上还是很好理解的。**

比如AopAutoConfiguration配置类，这个类用于自动配置Spring AOP的功能。该类被配置到了spring.factories文件中，因此它会在Spring Boot项目启动时被自动加载，而它的内部的@Configuration静态内部类也会跟着加载并且根据@Conditional条件注解选用，我们在这些内部类上面就能看到熟悉的@EnableAspectJAutoProxy注解，而这个注解就用于开始Spring AOP注解支持。

因此，我们不必在Spring Boot项目上手动添加@EnableAspectJAutoProxy注解，Spring Boot会自动帮我们配置这个注解。并且，**spring.aop. proxy-target-class这个属性的默认值为true，也就是说，Spring Boot 2.x项目中默认采用CGLIB动态代理。**

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a769d1c1b4834524bb7476df806dbb4e~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp)

## 2 @Conditional系列条件注解

`@Conditional`注解用于判断某个配置是否需要跳过，并且可被用作元注解而标注在其他注解上，这样就提供了丰富的校验机制，可用于判断某个自动配置类是否需要进行某些自动配置。

`spring-boot-autoconfigure`包中提供了一系列的`@Conditional`注解的衍生注解，它们已被标注在各种自动配置类上，当条件满足时，就会进行自动配置。我们也可以直接使用，常见的`@Conditional`系列注解如下：

| @ConditionalOnBean | 容器里存在指定类型的Bean时生效 |
| --- | --- |
| @ConditionalOnClass | 类路径存在指定的类时生效 |
| @ConditionalOnExpression | SpEL表达式结果为true时生效 |
| @ConditionalOnJava | 指定的Java版本存在时生效 |
| @ConditionalOnJndi | 指定的JNDI存在时生效 |
| @ConditionalOnMissingBean | 容器里不存在指定类型的Bean时生效 |
| @ConditionalOnMissingClass | 类路径不存在指定的类时生效 |
| @ConditionalOnNotWebApplication | 当前项目不是 Web 项目时生效 |
| @ConditionalOnWebApplicatio | 当前项目是 Web 项目时生效 |
| @ConditionalOnProperty | 指定的属性值和预期值一致时生效 |
| @ConditionalOnResource | 类路径存在指定的资源时生效 |
| @ConditionalOnSingleCandidate | 容器里指定类型的Bean只有一个或或者指定了一个首选Bean（@Primary）时生效 |

**相关文章：**

1.    [spring.io/](https://link.juejin.cn/?target=https%3A%2F%2Fspring.io%2F "https://spring.io/")
2.    [Spring Framework 5.x 学习](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Fcategory_10402193.html "https://blog.csdn.net/weixin_43767015/category_10402193.html")
3.    [Spring MVC 5.x 学习](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Fcategory_11020222.html "https://blog.csdn.net/weixin_43767015/category_11020222.html")
4.    [Spring Framework 5.x 源码](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Fcategory_10402194.html "https://blog.csdn.net/weixin_43767015/category_10402194.html")