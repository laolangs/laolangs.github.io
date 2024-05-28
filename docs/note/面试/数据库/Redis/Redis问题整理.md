## 1.Redis持久化

- RDB持久化：RDB 持久化是将 Redis 在内存中的数据定期保存到磁盘上的一个快照文件中。这个快照是一个二进制文件，保存了Redis在某个时间点的数据。Redis会fork一个子进程，将内存数据写入临时文件，然后替换原来的RDB文件。手动触发通过SAVE(同步阻塞服务端进程)/BGSAVE(异步执行)。
- AOF持久化：AOF持久化是将Redis的操作命令以追加的方式写入到一个文件中，保证数据的持久化，在服务器重启时通过重新执行AOF文件中的命令来恢复数据。AOF Rewrite只保留最终状态数据，压缩AOF文件大小。

## Redis主从结构

- slave服务器向master发送psync命令（此时发送的是psync ? -1），告诉master我需要同步数据了。
- master接收到psync命令后会进行BGSAVE命令生成RDB文件快照。
- 生成完后，会将RDB文件发送给slave。
- slave接收到文件会载入RDB快照，并且将数据库状态变更为master在执行BGSAVE时的状态一致。
- master会发送保存在缓冲区里的所有写命令，告诉slave可以进行同步了
- slave执行这些写命令。

断线重连

- 当slave断开重连后，会发送psync 命令给master。
- master首先会对服务器运行id进行判断，如果与自己相同就进行判断偏移量
- master会判断自己的偏移量与slave的偏移量是否一致。
- 如果不一致，master会去缓冲区中判断slave的偏移量之后的数据是否存在。
- 如果存在就会返回+continue回复，表示slave可以执行部分同步了。
- master发送断线后的写命令给slave
- slave执行写命令。

## 如何保证缓存与数据库的双写一致性？

- 先删除缓存，再更新数据库。
- 延时双删。先更新数据库，再删除缓存，唯一不同的是，我们把这个删除的动作，在不久之后再执行一次，比如 5s 之后。

## 缓存常见问题

### 缓存雪崩

- 设置合理的过期时间，分散过期时间
- 持久化机制，可以快速恢复缓存数据
- 限流降级（Guava RateLimiter限流）

### 缓存穿透

- 设置空缓存
- 布隆过滤器
```java
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class CacheFilter {
    private BloomFilter<String> bloomFilter;

    public CacheFilter() {
        // 创建一个布隆过滤器，预计存储1000个元素，误判率为0.01
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(), 1000, 0.01);
    }

    // 查询缓存，如果不存在则查询数据库
    public String getDataFromCacheOrDB(String key) {
        if (!bloomFilter.mightContain(key)) {
            // 缓存中不存在该数据，直接返回null，防止缓存穿透
            return null;
        }
        // 从缓存中获取数据
        String data = getFromCache(key);
        if (data == null) {
            // 缓存中不存在，从数据库中获取数据
            data = getFromDB(key);
            if (data != null) {
                // 将数据存入缓存
                putIntoCache(key, data);
            } else {
                // 数据库中也不存在该数据，将key加入布隆过滤器
                bloomFilter.put(key);
            }
        }
        return data;
    }

    private String getFromCache(String key) {
        // 从缓存中获取数据的逻辑
        return null;
    }

    private void putIntoCache(String key, String data) {
        // 将数据存入缓存的逻辑
    }

    private String getFromDB(String key) {
        // 从数据库中获取数据的逻辑
        return null;
    }

    public static void main(String[] args) {
        CacheFilter cacheFilter = new CacheFilter();
        String result1 = cacheFilter.getDataFromCacheOrDB("key1");
        System.out.println("Result 1: " + result1);

        String result2 = cacheFilter.getDataFromCacheOrDB("key2");
        System.out.println("Result 2: " + result2);
    }
}
```

### 缓存击穿

- 分布式锁（数据频繁更新）
- 设置热点数据不过期(数据基本不会发生更新)