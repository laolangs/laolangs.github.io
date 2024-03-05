# Kafka解惑

## 副本同步流程

通过混合push-pull模式

1. Leader接收到消息主动push给Follower,确保Follower副本尽快跟上Leader的最新消息。
2. Follower主动pull，通常发生在Follower副本与Leader副本存在数据落后或者不一致的情况，Follower会定期发送请求，获取缺失的消息数据，确保与Leader副本的同步。

## Kafka常见应用场景

1. 活动跟踪，做pv,uv等
2. 传递消息，标准消息中间件的功能
3. 收集指标和日志，熟悉服务日志，路由到下级进行分析，如ES
4. 提交日志，例如mysql变更日志，如故障可进行数据重放快速进行恢复
5. 流处理，对数据进行实时计算后推送给下级应用。

## Kafka分区顺序保证

单个partition保证有序,对数据有严格顺序要求时，设置`max.in.flight.request.per.connection=1,retires=0`,会严重影响生产
者的吞吐量。

## Kafka分区

ProducerRecord 包含了目标主题，键和值，
Kafka 的消息都是一个个的键值对。key相同时，散列到特定的分区上，保证数据总被映射到同一个分区（**一旦新增了分区，不能保证映射的有效性**）。

自定义分区器`partitioner.class`，让数据落到同一个partition

>自定义分区样例

```java
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import java.util.List;
import java.util.Map;

public class CustomPartitioner implements Partitioner {

    @Override
    public void configure(Map<String, ?> configs) {
        // 在这里进行配置，如果需要的话
    }

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        // 根据消息的值进行自定义分区逻辑
        // 这里假设消息的值是字符串，根据字符串的长度进行分区
        String messageValue = (String) value;
        int partition = messageValue.length() % cluster.partitionCountForTopic(topic);
        return partition;
    }

    @Override
    public void close() {
        // 在这里进行资源释放，如果有的话
    }
}

// 使用自定义分区器
public class KafkaProducerExample {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        
        // 指定自定义分区器
        props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, "com.example.CustomPartitioner");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        // 发送消息到 Kafka 主题
        producer.send(new ProducerRecord<>("custom-partition-topic", "key", "value"));

        producer.close();
    }
}
```

## 多线程下的消费者
KafkaConsumer 的实现不是线程安全的，所以我们在多线程的环境下，使用
KafkaConsumer 的实例要小心，应该每个消费数据的线程拥有自己的 KafkaConsumer 实例。

```java
public class KafkaExample {

    public static void main(String[] args) {
        // Kafka 配置
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        // 创建生产者
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        // 创建消费者线程池
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            consumerExecutor.submit(() -> {
                // 每个消费者线程创建了一个独立的 Kafka 消费者实例
                KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
                consumer.subscribe(Collections.singletonList("test-topic"));
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    records.forEach(record -> System.out.println("Consumer: " + consumer.groupId() + ", Record: " + record.value()));
                }
            });
        }

        // 创建生产者线程
        ExecutorService producerExecutor = Executors.newFixedThreadPool(1);
        producerExecutor.submit(() -> {
            for (int i = 0; i < 10; i++) {
                ProducerRecord<String, String> record = new ProducerRecord<>("test-topic", "key" + i, "value" + i);
                producer.send(record);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            producer.close();
        });
    }
}

```

## 消费者手动提交偏移量

```java
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("test-topic"));

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                // 处理消息
                for (var record : records) {
                    System.out.printf("Consumed record with key %s and value %s%n", record.key(), record.value());

                    // 组合使用手动提交，每处理 100 条消息提交一次偏移量
                    if (records.count() > 0) {
                        consumer.commitAsync();
                        if (records.count() >= 100) {
                            consumer.commitSync();
                        }
                    }
                }
                // 手动同步提交偏移量
                // consumer.commitSync();
                // 异步提交
                // consumer.commitAsync();
            }
        } finally {
            consumer.close();
        }

// 手动追踪已处理的消息偏移量，

        Map<TopicPartition, Long> processedOffsets = new HashMap<>(); // 用于跟踪处理消息的偏移量
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                // 处理消息
                for (var record : records) {
                    System.out.printf("Consumed record with key %s and value %s%n", record.key(), record.value());
                    // 记录处理的偏移量
                    processedOffsets.put(new TopicPartition(record.topic(), record.partition()), record.offset());
                }
                // 提交已处理消息的偏移量
                consumer.commitSync(processedOffsets);
                processedOffsets.clear(); // 清空已处理的偏移量，准备下一轮记录
            }
        } finally {
            consumer.close();
        }
```

## 监听分区再均衡

实现ConsumerRebalanceListenerj接口，consumer订阅主题时，传入自定义监听器。

```java
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import java.time.Duration;
import java.util.*;

public class KafkaConsumerExample {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        // 传入自定义分区再均衡监听器
        consumer.subscribe(Collections.singletonList("test-topic"), new CustomConsumerRebalanceListener(consumer));

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                // 处理消息
                for (var record : records) {
                    System.out.printf("Consumed record with key %s and value %s%n", record.key(), record.value());
                }
            }
        } finally {
            consumer.close();
        }
    }
}

class CustomConsumerRebalanceListener implements org.apache.kafka.clients.consumer.ConsumerRebalanceListener {
    private KafkaConsumer<String, String> consumer;
    private Map<TopicPartition, Long> lastCommittedOffsets = new HashMap<>();

    public CustomConsumerRebalanceListener(KafkaConsumer<String, String> consumer) {
        this.consumer = consumer;
    }

// 分区均衡调用前回调，提交偏移量
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        System.out.println("Partitions revoked: " + partitions);
        for (TopicPartition partition : partitions) {
            long offset = lastCommittedOffsets.getOrDefault(partition, 0L);
            consumer.commitSync(Collections.singletonMap(partition, offset));
        }
    }

// 分区均衡完成后回调，重新设置偏移量
    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        System.out.println("Partitions assigned: " + partitions);
        for (TopicPartition partition : partitions) {
            long offset = getLastCommittedOffset(partition);
            consumer.seek(partition, offset);
        }
    }
    // 偏移量可以借助外部工具来记录（例如mysql,保证数据消费和存放是原子性），再分区均衡后重新设置
    private long getLastCommittedOffset(TopicPartition partition) {
        // 此处应实现获取偏移量的逻辑，这里简单返回0
        return 0L;
    }
}
```

## 优雅退出

1. 注册shutdownhook

   ```java
        // 创建一个 CountDownLatch，用于等待优雅退出
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        // 注册一个关闭钩子，用于接收终止信号并执行退出逻辑
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received shutdown signal. Closing consumer and exiting gracefully.");
            // 提交偏移量并关闭消费者
            consumer.close();
            shutdownLatch.countDown(); // 计数器减一，释放等待
        }));

        // 消费消息
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                // 处理消息
                for (var record : records) {
                    System.out.printf("Consumed record with key %s and value %s%n", record.key(), record.value());
                }
            }
        } finally {
            shutdownLatch.countDown(); // 确保在发生异常时也能释放等待
        }
   ```

2. 调用consumer.wakeup(),消费者被唤醒后，poll() 方法会立即抛出 WakeupException，从而退出消费循环。在 finally 块中关闭了消费者，并释放了 CountDownLatch，确保优雅退出。

   ```java
        // 创建一个 CountDownLatch，用于等待优雅退出
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        // 注册一个关闭钩子，用于接收终止信号并执行退出逻辑
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received shutdown signal. Waking up consumer and exiting gracefully.");
            // 唤醒消费者
            consumer.wakeup();
            try {
                shutdownLatch.await(); // 等待优雅退出
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        // 消费消息
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                // 处理消息
                for (var record : records) {
                    System.out.printf("Consumed record with key %s and value %s%n", record.key(), record.value());
                }
            }
        } catch (Exception e) {
            System.out.println("Consumer is awake. Exiting gracefully.");
        } finally {
            consumer.close();
            shutdownLatch.countDown(); // 释放等待
        }
    ```

## kafka 为什么快?

参考<https://zhuanlan.zhihu.com/p/337861077>

1. partition 并行处理
2. 顺序写磁盘，充分利用磁盘特性
3. 利用了现代操作系统分页存储 Page Cache 来利用内存提高 I/O 效率
4. 采用了零拷贝技术Producer 生产的数据持久化到 broker，采用 mmap 文件映射，实现顺序的快速写入Customer 从 broker 读取数据，采用 sendfile将磁盘文件读到 OS 内核缓冲区后，转到 NIO buffer进行网络发送，减少 CPU 消耗
5. 批量读写
6. 批量压缩

## Kafka中的ISR(InSyncRepli)、OSR(OutSyncRepli)、AR(AllRepli)又代表什么？

ISR 处于同步状态的副本
OSR 处于未同步状态的副本，OSR 中的副本可能存在滞后或延迟，并且可能需要花费更长的时间来同步领导者副本的数据
AR 是指包括 ISR 和 OSR 在内的所有副本。

## Kafka中的HW、LEO等分别代表什么？

`HW（High Watermark）`： High Watermark 是指一个分区中所有副本的最小的偏移量。简单来说，HW 是一个分区中已经被所有副本成功复制的消息的最大偏移量。对于生产者而言，消息被成功发送到 Kafka 后，只有当其偏移量大于等于 HW 时才能确认消息已被持久化。对于消费者而言，只有当消费到的消息的偏移量小于 HW 时，才能确保消费者不会丢失任何已经被持久化的消息。

`LEO（Log End Offset）`： Log End Offset 是指一个分区中当前日志段的最大偏移量。简单来说，LEO 是一个分区中当前日志段中最新消息的下一个偏移量。LEO 可以理解为分区中消息的最大偏移量加一。对于生产者而言，LEO 表示了当前分区中最新的消息的偏移量，可以用来确定下一条要发送的消息的偏移量。对于消费者而言，LEO 表示了分区中当前可消费的消息的最大偏移量。

总结来说，HW 表示了已经被持久化的消息的边界，而 LEO 表示了当前分区中最新消息的边界。在生产者和消费者之间，可以通过 HW 和 LEO 来确定消息的可靠性和可消费性。

## Kafka中是怎么体现消息顺序性的？

分区内顺序性、分区间并行性、消费者组和分区对应关系，Kafka顺序性主要是通过分区来保证的。

## Kafka中的分区器、序列化器、拦截器是否了解？它们之间的处理顺序是什么？

`序列化器（Serializer）`对消息的key和value进行序列化为字节数组。

`拦截器（Interceptor）`对消息进行定制化处理，可以用于记录日志、修改消息、添加额外的信息。

`分区器（Partitioner）`决定了消息发送到哪个分区

处理顺序：序列化器 -> 拦截器 -> 分区器

## Kafka生产者客户端的整体结构是什么样子的？使用了几个线程来处理？分别是什么？

生产者配置、生产者实例、消息缓冲区、发送线程、分区器、序列化器

整体结构中通常会包含一个主发送线程和若干个后台线程（如消息发送线程、缓冲区刷新线程等），这些线程协同工作来实现消息的异步发送和处理。

## 有哪些情形会造成重复消费？

消费者提交消费位移错误、消费者重启后未正确恢复状态、消费者组内发生再均衡、消息重复发送

## 当你使用kafka-topics.sh创建（删除）了一个topic之后，Kafka背后会执行什么逻辑？

1. 会在zookeeper中的/brokers/topics节点下创建一个新的topic节点，如：/brokers/topics/first

2. 触发Controller的监听程序

3. kafka Controller 负责topic的创建工作，并更新metadata cache

## topic的分区数可不可以增加？如果可以怎么增加？如果不可以，那又是为什么？

可以通过kafka-topics.sh进行增加分区
`./kafka-topics.sh --zookeeper localhost:2181 --alter --topic my_topic --partitions 10
`

## topic的分区数可不可以减少？如果可以怎么减少？如果不可以，那又是为什么？

不可以进行分区减少操作，分区减少后，分区的数据会丢失。非要进行数据分区减少，通过创建一个新的topic，指定需要的分区，将数据复制过去即可，中间涉及数据转移状态切换。

## Kafka有内部的topic吗？如果有是什么？有什么所用？

__consumer_offsets（记录消费偏移量）、__transaction_state（存储事务状态）、__consumer_group_metadata（存储消费者组的元数据信息）、__deletions_topic（存储被删除的主题或分区的元数据信息）

## Kafka分区分配的概念？

分区平衡，消费者组内唯一性，消费者组外隔离性

分区策略：

1. `Round-robin 分配策略`，默认情况下，Kafka 使用的是 Round-robin 分配策略，即按照消费者的顺序依次分配分区。
2. `Range 分配策略`，可以通过配置选项来指定使用 Range 分配策略，该策略将分区按照分区的范围（即分区的编号）进行排序，然后将连续的分区范围分配给每个消费者。这种策略更适用于需要精细控制分区分配的场景。

## 简述Kafka的日志目录结构？

1. Log Dir（日志目录）：日志目录是 Kafka 存储消息日志和元数据的根目录。
2. Topic Dir（主题目录）： 主题目录是日志目录下的子目录，用于存储特定主题的消息日志和元数据。
3. Partition Dir（分区目录）： 分区目录是主题目录下的子目录，用于存储特定分区的消息日志和元数据。
4. Log Segment File（日志段文件）： 日志段文件是 Kafka 存储消息的基本单位，每个分区的消息日志被分成多个连续的日志段文件存储。
5. Index File（索引文件）： 索引文件用于加速消息的检索和查找，每个日志段文件都对应一个索引文件。
6. Time Index File（时间索引文件）： 时间索引文件是一种特殊的索引文件，用于根据时间范围来快速定位消息。

## 如果我指定了一个offset，Kafka怎么查找到对应的消息？

topic->partition->segment->index->message


## 聊一聊Kafka Controller的作用？

1. 集群管理： 控制器负责监视和管理整个 Kafka 集群的状态。它定期检查集群中各个 Broker 的健康状态，并根据需要执行相应操作，如启动、停止、重启 Broker。
2. Broker 故障检测和处理： 控制器负责检测和处理 Broker 的故情况。当探测到某个 Broker 发生故障时，控制器会负责触发副重分配操作，将故障 Broker 上的分区副本重新分配到其他健康的Broker 上，以保证分区的可用性和数据的完整性。
3. 分区的副本管理： 控制器负责管理主题分区的副本分配和副本状态同步。它会监控分区的副本状态，确保每个分区的副本数量达到配的副本因子，并处理分区副本的选举和同步等操作。
4. 领导选举： 控制器负责管理分区的领导者（Leader）选举。当分的领导者发生变更时，控制器会负责触发新的领导者选举过程，并保选举结果的一致性和可靠性。
5. 元数据管理： 控制器负责管理集群的元数据信息，包括主题、分区消费者组等的元数据。它会监控元数据的变更，并将变更信息广播集群中的其他 Broker，以确保集群中各个节点的元数据信息保持致。

总之，Kafka Controller 是 Kafka 集群中的核心组件之一，承担着监控、管理和维护集群状态的重要任务。它确保了 Kafka 集群的稳定运行和高可用性，并提供了一些关键的功能和服务，如故障处理、副本管理、领导选举等。

## Kafka中有那些地方需要选举？这些地方的选举策略又有哪些？

基于 ZooKeeper 的分布式一致性算法实现的选举（候选者的提名、选票投票和最终结果的确定），涉及选举如下：

1. 分区的领导者选举
2. Controller 的选举
