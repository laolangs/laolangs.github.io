首先说下结论：kafka是不会在生产者发送消息的回调中，把发送的消息再一次返回回来的，因为这些消息我们可以自己记录，没必要浪费网络资源。

> kafka-client的回调方法

kafka原生的kafka-client包中，生产者发送回调方法如下，其中RecordMetadata包含发送成功后的分区、偏移量和时间戳等信息；Exception是发送失败后的异常信息

```java
producer.send(record, new Callback() {
    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        System.out.println(metadata.partition() + "---" + metadata.offset());
    }
});
```

可以发现确实回调方法里确实不包含消息数据，但是我们可以自己继承Callback类，添加消息属性，在send的时候使用我们自己写的callback类，这样就能拿到消息数据了，例子如下：

```java
class MyCallback implements Callback {
   
   private Object msg;
   
   public MyCallback(Object msg) {
       this.msg = msg;
   }
   
   @Override
   public void onCompletion(RecordMetadata metadata, Exception exception) {
   }
}
producer.send(record, new MyCallback(record));
```

> springboot中的kafka回调

那么spring提供的spring-kafka里面是怎么处理的呢，在spring中，使用kafka生产者发送消息的方法如下：

```java
/**
    * 发送消息并接收回调
    * 
    * @param msg
    */
   public void sendAndCallback(String msg) {
       ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send("DrewTest", msg);
       future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
           @Override
           public void onSuccess(SendResult<String, Object> result) {
               System.out.println("msg OK." + result.toString());
           }
           
           @Override
           public void onFailure(Throwable ex) {
               System.out.println("msg send failed: " + ex.getMessage());
           }
       });
   }
```

其中回调函数里的SendResult类里面包含ProducerRecord和RecordMetadata，而ProducerRecord就是spring定义的消息类，但是这里可以看到，在失败的onFailure方法中，仍然只能拿到异常Throwable类，其实spring内部已经在失败的时候对消息对象做了处理，只是没有显式返回给我们,源码如下：

```java
protected ListenableFuture<SendResult<K, V>> doSend(final ProducerRecord<K, V> producerRecord) {
 if (this.transactional) {
  Assert.state(inTransaction(),
    "No transaction is in process; "
     + "possible solutions: run the template operation within the scope of a "
     + "template.executeInTransaction() operation, start a transaction with @Transactional "
     + "before invoking the template method, "
     + "run in a transaction started by a listener container when consuming a record");
 }
 final Producer<K, V> producer = getTheProducer();
 if (this.logger.isTraceEnabled()) {
  this.logger.trace("Sending: " + producerRecord);
 }
 final SettableListenableFuture<SendResult<K, V>> future = new SettableListenableFuture<>();
 producer.send(producerRecord, buildCallback(producerRecord, producer, future));//kafka-client的发送方法
 if (this.autoFlush) {
  flush();
 }
 if (this.logger.isTraceEnabled()) {
  this.logger.trace("Sent: " + producerRecord);
 }
 return future;
}
 
private Callback buildCallback(final ProducerRecord<K, V> producerRecord, final Producer<K, V> producer,
  final SettableListenableFuture<SendResult<K, V>> future) {
 return (metadata, exception) -> {
  try {
      if (exception == null) {
    future.set(new SendResult<>(producerRecord, metadata));
    if (KafkaTemplate.this.producerListener != null) {
     KafkaTemplate.this.producerListener.onSuccess(producerRecord, metadata);
    }
    if (KafkaTemplate.this.logger.isTraceEnabled()) {
     KafkaTemplate.this.logger.trace("Sent ok: " + producerRecord + ", metadata: " + metadata);
    }
   }
   else {
    future.setException(new KafkaProducerException(producerRecord, "Failed to send", exception));
    if (KafkaTemplate.this.producerListener != null) {
     KafkaTemplate.this.producerListener.onError(producerRecord, exception);//传给监听器
    }
    if (KafkaTemplate.this.logger.isDebugEnabled()) {
     KafkaTemplate.this.logger.debug("Failed to send: " + producerRecord, exception);
    }
   }
  }
  finally {
   if (!KafkaTemplate.this.transactional) {
    closeProducer(producer, false);
   }
  }
 };
}
```

第一个doSend方法，就是我们调用spring-kafka发送消息的方法，可以看到里面其实也是调用了kafka-client的方法，其中传入的buildCallback回调方法中，把ProducerRecord消息对象传给了监听器。所以spring在生产回调中获取消息数据的方式就跟我一开始说的类似，就是自己继承或者包装了一个Callback类，然后把消息数据当参数传进去。

那么在spring中，我们要怎么在失败的时候拿到这些数据呢，上面说到，spring在buildCallback回调方法中，把消息对象传给监听器，所以，我们要拿到这些数据，也必须自己写个监听器，在监听器中获取这些消息数据，进行失败处理。

```java
@Service
public class MyProducerListener implements ProducerListener<String, Object> {
    @Override
    public void onSuccess(String topic, Integer partition, String key, Object value, RecordMetadata recordMetadata) {
        System.out.println("消息发送成功");
    }
    
    @Override
    public void onError(String topic, Integer partition, String key, Object value, Exception exception) {
        //可对进行重发重试
        System.out.println("消息发送失败");
    }
}

//设置kafkaTemplate的生产者监听器
@Autowired
public MyProducerListener producerListener;

kafkaTemplate.setProducerListener(producerListener);

```
