[原文地址](https://juejin.cn/post/7113920258016018439)

> 基于Spring Boot 2.x详细介绍了Spring Boot的配置文件的加载优先级。

## 1 总体优先级

**Spring boot支持非常多的配置，常见的外部配置和内部配置的整体优先级（[docs.spring.io/spring-boot…](https://link.juejin.cn/?target=https%3A%2F%2Fdocs.spring.io%2Fspring-boot%2Fdocs%2F2.3.10.RELEASE%2Freference%2Fhtml%2Fspring-boot-features.html%23boot-features-external-config%25EF%25BC%2589%25EF%25BC%259A "https://docs.spring.io/spring-boot/docs/2.3.10.RELEASE/reference/html/spring-boot-features.html#boot-features-external-config%EF%BC%89%EF%BC%9A")**

1. 命令行参数，比如--server.port=8080。
2. JVM系统属性，通过System.getProperties()方法获取。
3. 系统环境属性，通过System.getenv()方法获取。
4. jar包外部的application-{profile}.properties/yml配置文件，如果没有指明激活的profile，则默认为default。
5. jar包内部的application-{profile}.properties/yml配置文件，如果没有指明激活的profile，则默认为default。
6. jar包外部的application.properties/yml配置文件。
7. jar包内部的application.properties/yml配置文件。
8. 通过@Configuration注解类上的@PropertySource注解引入的配置文件。

## 2 内部配置优先级

**配置文件查找时首先查找指定profile的，然后再查找没有profile的。查找的目录优先级（[docs.spring.io/spring-boot…](https://link.juejin.cn/?target=https%3A%2F%2Fdocs.spring.io%2Fspring-boot%2Fdocs%2F2.3.10.RELEASE%2Freference%2Fhtml%2Fspring-boot-features.html%23boot-features-external-config-application-property-files%25EF%25BC%2589%25EF%25BC%259A "https://docs.spring.io/spring-boot/docs/2.3.10.RELEASE/reference/html/spring-boot-features.html#boot-features-external-config-application-property-files%EF%BC%89%EF%BC%9A")**

1. 项目根目录下的/config目录下的配置文件。
2. 项目根目录下的配置文件。
3. 项目类路径（resources）下的/config目录下的配置文件。
4. 项目类路径（resources）下的配置文件。

**注意点：**

1. 同一目录下，`.properties`文件优先于`.yml`文件加载。
2. 可以使用`spring.config.location`属性替换默认查找位置，也可以使用`spring.config.additional-location`属性来添加额外的配置位置，将会首先查找指定的位置然后查找默认位置。
3. 需要注意的是，项目根目录下的配置文件或者/config目录下的配置文件在打包的时候不会被打入jar包中，因此一般很少用到。

## 3 bootstrap和application的优先级

bootstrap配置文件由spring父上下文加载，并且比application配置文件优先加载（父上下文不会使用application配置文件），而application配置文件由子上下文加载。bootstrap加载的配置信息不能被application的相同配置覆盖。

但是注意，如果要使用配置文件中的变量，那么同名变量将使用application文件中的配置，比如如果两个配置文件都有server.post变量，那么Spring将使用application中配置的值。为什么？因为在Environment中，application配置文件的propertySource排在bootstrap配置文件的propertySource之前，Spring 在进行属性注入、获取时，将会顺序遍历所有的propertySource查找属性，如果找到了就直接返回。.peoperties文件比.yaml文件的属性查找优先级更高的原理一样。

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/49fcb5fd57d54e5483a555c37bb8b416~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp)

**相关文章：**

1.   [spring.io/](https://link.juejin.cn/?target=https%3A%2F%2Fspring.io%2F "https://spring.io/")
2.   [Spring Framework 5.x 学习](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Fcategory_10402193.html "https://blog.csdn.net/weixin_43767015/category_10402193.html")
3.   [Spring MVC 5.x 学习](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Fcategory_11020222.html "https://blog.csdn.net/weixin_43767015/category_11020222.html")
4.   [Spring Framework 5.x 源码](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Fcategory_10402194.html "https://blog.csdn.net/weixin_43767015/category_10402194.html")

> 如有需要交流，或者文章有误，请直接留言。另外希望点赞、收藏、关注，我将不间断更新各种Java学习博客！

本文收录于以下专栏

![cover](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/95414745836549ce9143753e2a30facd~tplv-k3u1fbpfcp-jj:160:120:0:0:q75.avis)

上一篇

Spring Boot自动配置的原理简介以及@Conditional条件注解

下一篇

Spring Boot启动源码分析【一万字】
