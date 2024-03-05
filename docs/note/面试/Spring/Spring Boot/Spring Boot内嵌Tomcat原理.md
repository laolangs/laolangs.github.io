[原文地址](https://juejin.cn/post/7114679361042645022)

**本次的spring-boot-starter-parent版本为2.3.0。**

之前分析了[Spring boot的启动源码](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Farticle%2Fdetails%2F123239408 "https://blog.csdn.net/weixin_43767015/article/details/123239408")的大概流程，结尾我们说了一些内嵌tomcat原理的原理，现在我们来深入探究一下！

## 1 ServletWebServerFactoryConfiguration

实际上，spring boot是指出多种服务器启动的，并不只是tomcat，还有jetty等。因此我们可以猜测具体哪种服务器是可以配置的，而spring boot又是以自动配置闻名，那么这些服务器肯定与某些自动配置类相关。

实际上，spring boot的servlet web服务器的配置类就是位于spring-boot-autoconfigure.jar下的/META-INF/spring.factories文件中的一个名为ServletWebServerFactoryAutoConfiguration的自动配置类。

在该自动配置类中，会通过@Import想容器中注入四个配置类，我们可以看到，各种容器的web服务配置，Tomcat、Jetty、Undertow，其中Tomcat对应EmbeddedTomcat。

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/bf8cabecd8eb4b91be1a1a2ae341c28f~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp)

这个EmbeddedTomcat配置类又会向Spring容器注入TomcatServletWebServerFactory，这个类就是Tomcat启动的关键类，用于创建TomcatWebServer。

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0ebb8ccf0ee94989a46934755e9329ea~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp)

另外，ServletWebServerFactoryAutoConfiguration中还会注入一系列的Customizer，用于修改内嵌Tomcat的参数和配置。

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/30e6365cca894ae48e445f5b85ba3a56~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp)

## 2 onRefresh启动web服务

那么，这个TomcatServletWebServerFactory是怎么在什么时候被加载到容器中并使用的呢？Tomcat又是什么时候被启动的呢？

之前的文章就讲过，在spring boot容器启动过程中，在创建容器之后，会执行刷新容器的操作，也就是refresh()操作，这个操作实际上就是spring容器的启动方法，将会加载bean以及各种配置。该方法是spring项目的核心方法，源码非常多，我们在之前以及花了大量时间讲过了，在此不再赘述，之前的文章链接[Spring IoC容器初始化源码](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Farticle%2Fdetails%2F109258468 "https://blog.csdn.net/weixin_43767015/article/details/109258468")。

在refresh()方法中，有一个onRefresh()方法。

![在这里插入图片描述](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/4ea5f28e798d4076b202ad10ad22308e~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp)

这个onRefresh方法默认是一个空的实现，这是留给子类容器实现的扩展方法。这个方法是在所有的bean定义被注入到容器中之后调用的，而在onRefresh方法之后，则会对所有的普通单例bean进行实例化和初始化。

默认的web服务容器是AnnotationConfigServletWebServerApplicationContext，它又继承了ServletWebServerApplicationContext，该类就对onRefresh方法进行了实现，并且Spring boot的web服务器就是在此启动的！

```java
/** * ServletWebServerApplicationContext实现的方法 */ @Override protected void onRefresh(){ //调用父类的逻辑 super.onRefresh(); try{ /* * 关键方法，创建webserver */ createWebServer(); } catch( Throwable ex ){ throw new ApplicationContextException( "Unable to start web server", ex ); } }
```

可以看到，内部调用了createWebServer方法创建web服务器。

## 2.1 createWebServer创建web服务

createWebServer方法的代码如下，它会通过之前配置的ServletWebServerFactory，获取webServer，即创建web服务器。

一般我们使用的ServletWebServerFactory就是TomcatServletWebServerFactory，使用的webserver就是TomcatWebServer。

在创建了webserver之后，会想容器注入两个SmartLifecycle类型的bean实例，这实际上是一个扩展点的实例，用于实现容器回调。

其中，注册的WebServerStartStopLifecycle实例，在ServletWebServerApplicationContext类型的容器启动完毕后会调用该实例的start方法启动webServer并发送事件，在ServletWebServerApplicationContext类型的容器销毁时将会调用该实例的stop方法销毁webServer。

```java
private volatile WebServer webServer; /** * ServletWebServerApplicationContext的方法 * <p> * 创建web服务 */ private void createWebServer(){ //获取WebServer，这里默认是空的 WebServer webServer = this.webServer; //获取ServletContext，即servlet上下文，这里默认是空的 ServletContext servletContext = getServletContext(); /* * 获取webServer，初始化web服务 */ if( webServer == null && servletContext == null ){ //获取web服务工厂，默认就是TomcatServletWebServerFactory ServletWebServerFactory factory = getWebServerFactory(); /* * 通过web服务工厂获取web服务，核心代码 * 创建内嵌的Tomcat并启动 */ this.webServer = factory.getWebServer( getSelfInitializer() ); /* * 注册WebServerGracefulShutdownLifecycle的实例到容器中 * ReactiveWebServerApplicationContext容器启动完毕后会调用该实例的start方法 * ReactiveWebServerApplicationContext容器销毁时将会调用该实例的stop方法 */ getBeanFactory().registerSingleton( "webServerGracefulShutdown", new WebServerGracefulShutdownLifecycle( this.webServer ) ); /* * 注册WebServerStartStopLifecycle的实例到容器中 * ServletWebServerApplicationContext容器启动完毕后会调用该实例的start方法尝试启动webServer并发送事件 * ServletWebServerApplicationContext容器销毁时将会调用该实例的stop方法销毁webServer */ getBeanFactory().registerSingleton( "webServerStartStop", new WebServerStartStopLifecycle( this, this.webServer ) ); } else if( servletContext != null ){ try{ getSelfInitializer().onStartup( servletContext ); } catch( ServletException ex ){ throw new ApplicationContextException( "Cannot initialize servlet context", ex ); } } //初始化ConfigurableWebEnvironment类型的配属数据源 initPropertySources(); }
```

### 2.1.1 getWebServerFactory获取web服务工厂

该方法获取web服务工厂，工厂用于创建web服务。

```java
/** * ServletWebServerApplicationContext的方法 * <p> * 获取ServletWebServerFactory，用于初始化webServer * 默认返回TomcatServletWebServerFactory */ protected ServletWebServerFactory getWebServerFactory(){ //从容器中搜索ServletWebServerFactory类型的beanName数组 //之前的ServletWebServerFactoryConfiguration配置类就会像容器中 //注入ServletWebServerFactory的bean，默认就是TomcatServletWebServerFactory String[] beanNames = getBeanFactory().getBeanNamesForType( ServletWebServerFactory.class ); //没有web服务工厂 if( beanNames.length == 0 ){ throw new ApplicationContextException( "Unable to start ServletWebServerApplicationContext due to missing " + "ServletWebServerFactory bean." ); } //有多个web服务工厂 if( beanNames.length > 1 ){ throw new ApplicationContextException( "Unable to start ServletWebServerApplicationContext due to multiple " + "ServletWebServerFactory beans : " + StringUtils.arrayToCommaDelimitedString( beanNames ) ); } //从容器中获取web服务工厂的实例 return getBeanFactory().getBean( beanNames[ 0 ], ServletWebServerFactory.class ); }
```

### 2.1.2 getWebServer获取web服务

ServletWebServerFactory的方法，用于获取web服务。其中TomcatServletWebServerFactory的方法用于创建Tomcat实例并返回TomcatServer。

该方法中的一些名词比如baseDir、connector、Service、Host、AutoDeploy 、Engine等等都是Tomcat中的概念，我们之前就介绍过了，在此不再赘述：[Tomcat的核心组件以及server.xml配置全解【一万字】](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Farticle%2Fdetails%2F112601007 "https://blog.csdn.net/weixin_43767015/article/details/112601007")。

在最后的getTomcatWebServer方法中会对Tomcat服务器进行启动。控制台会输出日志：Tomcat initialized with port(s): 8080 (http)。

```java
/** * TomcatServletWebServerFactory的方法 * 创建内嵌的Tomcat * * @param initializers 初始化器 * @return Tomcat的web服务 */ @Override public WebServer getWebServer( ServletContextInitializer... initializers ){ if( this.disableMBeanRegistry ){ Registry.disableRegistry(); } //创建Tomcat实例 Tomcat tomcat = new Tomcat(); //设置Tomcat的基本目录 File baseDir = ( this.baseDirectory != null ) ? this.baseDirectory : createTempDir( "tomcat" ); tomcat.setBaseDir( baseDir.getAbsolutePath() ); //设置Connector，用于接受请求发挥响应 Connector connector = new Connector( this.protocol ); connector.setThrowOnFailure( true ); tomcat.getService().addConnector( connector ); //自定义连接器 customizeConnector( connector ); tomcat.setConnector( connector ); //是否自动部署 tomcat.getHost().setAutoDeploy( false ); //设置Engine configureEngine( tomcat.getEngine() ); //自己扩展的连接器 for( Connector additionalConnector : this.additionalTomcatConnectors ){ tomcat.getService().addConnector( additionalConnector ); } //准备上下文 prepareContext( tomcat.getHost(), initializers ); //创建TomcatWebServer，启动Tomcat，返回TomcatWebServer return getTomcatWebServer( tomcat ); }
```

Tomcat启动后的继续执行Spring的逻辑，初始化bean实例等等，Spring容器初始化完毕之后，调用WebServerStartStopLifecycle的start方法，对TomcatWebServer进行启动，此时控制台会输出日志：Tomcat started on port(s): 8080 (http) with context path ''。

**相关文章：**

1.  [spring.io/](https://link.juejin.cn/?target=https%3A%2F%2Fspring.io%2F "https://spring.io/")
2.  [Spring Framework 5.x 学习](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Fcategory_10402193.html "https://blog.csdn.net/weixin_43767015/category_10402193.html")
3.  [Spring MVC 5.x 学习](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Fcategory_11020222.html "https://blog.csdn.net/weixin_43767015/category_11020222.html")
4.  [Spring Framework 5.x 源码](https://link.juejin.cn/?target=https%3A%2F%2Fblog.csdn.net%2Fweixin_43767015%2Fcategory_10402194.html "https://blog.csdn.net/weixin_43767015/category_10402194.html")

> 如有需要交流，或者文章有误，请直接留言。另外希望点赞、收藏、关注，我将不间断更新各种Java学习博客！