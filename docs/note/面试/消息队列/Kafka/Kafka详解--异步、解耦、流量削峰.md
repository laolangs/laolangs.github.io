![](https://csdnimg.cn/release/blogv2/dist/pc/img/reprint.png)

[CodingALife](https://blog.csdn.net/mingyuli "CodingALife") ![](https://csdnimg.cn/release/blogv2/dist/pc/img/newCurrentTime2.png) 于 2021-04-04 18:11:28 发布

**目录**

[1、解耦](https://blog.csdn.net/mingyuli/article/details/115431459#t0 "1、解耦")

[2、接口异步处理](https://blog.csdn.net/mingyuli/article/details/115431459#t1 "2、接口异步处理")

[3、流量削峰：](https://blog.csdn.net/mingyuli/article/details/115431459#t2 "3、流量削峰：")

[4、问题](https://blog.csdn.net/mingyuli/article/details/115431459#t3 "4、问题")

[1、消息的顺序性MQ怎么保证？](https://blog.csdn.net/mingyuli/article/details/115431459#t4 "1、消息的顺序性MQ怎么保证？")

[2、缓冲和削峰：](https://blog.csdn.net/mingyuli/article/details/115431459#t5 "2、缓冲和削峰：")

[3、什么情况下一个 broker 会从 isr中踢出去](https://blog.csdn.net/mingyuli/article/details/115431459#t6 "3、什么情况下一个 broker 会从 isr中踢出去")

[4、kafka producer如何优化打入速度](https://blog.csdn.net/mingyuli/article/details/115431459#t7 "4、kafka producer如何优化打入速度")

[5、为什么Kafka不支持读写分离？](https://blog.csdn.net/mingyuli/article/details/115431459#t8 "5、为什么Kafka不支持读写分离？")

___

## 1、解耦

首先我们看下耦合较高的情况，谁愿意负责A系统？难道被累死么？

![1](https://img-blog.csdnimg.cn/20181213195251692.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MjMyMzgwMg==,size_16,color_FFFFFF,t_70)

负责A系统的大兄弟自作主张引入MQ[消息队列](https://so.csdn.net/so/search?q=%E6%B6%88%E6%81%AF%E9%98%9F%E5%88%97&spm=1001.2101.3001.7020)后，我管你老王、老张还是老李要什么数据，我放在MQ中，你们要就从MQ中拿

## ![在这里插入图片描述](https://img-blog.csdnimg.cn/201812131955530.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MjMyMzgwMg==,size_16,color_FFFFFF,t_70)  
2、接口异步处理

首先来看下没有引入MQ时候，假设打开一个网页随意点一下都需要一秒才返回数据，才响应出来

![99](https://img-blog.csdnimg.cn/20181213200116584.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MjMyMzgwMg==,size_16,color_FFFFFF,t_70)  
因为，Ajax请求到系统服务，服务响应给用户的时间为：消息队列耗时+系统服务耗时；

## ![在这里描述](https://img-blog.csdnimg.cn/20181213200152733.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MjMyMzgwMg==,size_16,color_FFFFFF,t_70)  
3、流量削峰：

ok,没有采用MQ，用户并发访问直接爆了服务器，服务器强悍不爆，你硬件读写能跟得上？Mysql并发2000就要GG，直接罢工了；

![4](https://img-blog.csdnimg.cn/20181213200758560.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MjMyMzgwMg==,size_16,color_FFFFFF,t_70)

引入MQ，你干不完是吧，先存起来，等你慢慢干；  
通常用于 类似于双11 等并发访问较大时候；  
秒杀活动，也要用到；

![34](https://img-blog.csdnimg.cn/20181213201130379.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MjMyMzgwMg==,size_16,color_FFFFFF,t_70)

## 4、问题

### 1、消息的顺序性MQ怎么保证？

在MQ的模型中，顺序需要由3个阶段去保障：

消息被发送时保持顺序  
消息被存储时保持和发送的顺序一致  
消息被消费时保持和存储的顺序一致

发送时保持顺序意味着对于有顺序要求的消息，用户应该在同一个线程中采用同步的方式发送。存储保持和发送的顺序一致则要求在同一线程中被发送出来的消息A和B，存储时在空间上A一定在B之前。而消费保持和存储一致则要求消息A、B到达Consumer之后必须按照先A后B的顺序被处理。

![2](https://img-blog.csdnimg.cn/20181213205040848.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MjMyMzgwMg==,size_16,color_FFFFFF,t_70)

### 2、缓冲和削峰：

上游数据时有突发流量，下游可能扛不住，或者下游没有足够多的机器来保证冗余，[kafka](https://so.csdn.net/so/search?q=kafka&spm=1001.2101.3001.7020)在中间可以起到一个缓冲的作用，把消息暂存在kafka中，下游服务就可以按照自己的节奏进行慢慢处理。

### 3、什么情况下一个 broker 会从 isr中踢出去

leader会维护一个与其基本保持同步的Replica列表，该列表称为ISR(in-sync Replica)，每个Partition都会有一个ISR，而且是由leader动态维护 ，如果一个follower比一个leader落后太多，或者超过一定时间未发起数据复制请求，则leader将其重ISR中移除 。

### 4、kafka producer如何优化打入速度

增加线程

提高 batch.size

增加更多 producer 实例

增加 partition 数

设置 acks=-1 时，如果延迟增大：可以增大 num.replica.fetchers（follower 同步数据的线程数）来调解；

跨数据中心的传输：增加 socket 缓冲区设置以及 OS tcp 缓冲区设置。

### 5、为什么Kafka不支持读写分离？

在 Kafka 中，生产者写入消息、消费者读取消息的操作都是与 leader 副本进行交互的，从 而实现的是一种主写主读的生产消费模型。

Kafka 并不支持主写从读，因为主写从读有 2 个很明 显的缺点:

(1)数据一致性问题。数据从主节点转到从节点必然会有一个延时的时间窗口，这个时间 窗口会导致主从节点之间的数据不一致。某一时刻，在主节点和从节点中 A 数据的值都为 X， 之后将主节点中 A 的值修改为 Y，那么在这个变更通知到从节点之前，应用读取从节点中的 A 数据的值并不为最新的 Y，由此便产生了数据不一致的问题。

(2)延时问题。类似 Redis 这种组件，数据从写入主节点到同步至从节点中的过程需要经 历网络→主节点内存→网络→从节点内存这几个阶段，整个过程会耗费一定的时间。而在 Kafka 中，主从同步会比 Redis 更加耗时，它需要经历网络→主节点内存→主节点磁盘→网络→从节 点内存→从节点磁盘这几个阶段。对延时敏感的应用而言，主写从读的功能并不太适用。