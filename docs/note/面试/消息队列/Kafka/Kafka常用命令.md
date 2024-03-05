## kafka常用命令

1. 创建topic

    ```bash
    bin/kafka-topics.sh --create --topic <topic-name> --bootstrap-server localhost:9092 --partitions 3 --replication-factor 2
    ```

2. 查看 Topic 列表:

    ```bash
    bin/kafka-topics.sh --list --bootstrap-server   localhost:9092
    ```

3. 查看 Topic 详情:

    ```bash
    bin/kafka-topics.sh --describe --topic <topic-name> --bootstrap-server localhost:9092
    ```

4. 生产者发送消息:

    ```bash
    bin/kafka-console-producer.sh --topic <topic-name>  --bootstrap-server localhost:9092
    ```

5. 消费者消费消息:

    ```bash
    bin/kafka-console-consumer.sh --topic <topic-name>  --from-beginning --bootstrap-server localhost:9092
    ```

6. 创建 Kafka 消费者组:

    ```bash
    bin/kafka-consumer-groups.sh --bootstrap-server     localhost:9092 --list
    ```

7. 查看消费者组详情:

    ```bash
    bin/kafka-consumer-groups.sh --bootstrap-server     localhost:9092 --describe --group <group-id>
    ```

8. 查看偏移量 (offset):

    ```bash
    bin/kafka-run-class.sh kafka.tools.GetOffsetShell   --topic <topic-name> --broker-list localhost:9092     --time -1
    ```

9. 生产者性能测试:

    ```bash
    bin/kafka-producer-perf-test.sh --topic <topic-name>    --num-records 100000 --record-size 1000 --throughput   100000 --producer-props bootstrap.    servers=localhost:9092
    ```

10. 消费者性能测试:

    ```bash
    bin/kafka-consumer-perf-test.sh --topic <topic-name>    --broker-list localhost:9092 --messages 1000000    --threads 1
    ```

11. 查看 ZooKeeper 中 Kafka 节点:

    ```bash
    bin/zookeeper-shell.sh localhost:2181 ls /brokers/ids
    ```
