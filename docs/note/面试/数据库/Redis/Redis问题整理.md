## 1.Redis持久化

- RDB持久化：RDB 持久化是将 Redis 在内存中的数据定期保存到磁盘上的一个快照文件中。这个快照是一个二进制文件，保存了Redis在某个时间点的数据。
- AOF持久化：AOF持久化是将Redis的操作命令以追加的方式写入到一个文件中，保证数据的持久化，在服务器重启时通过重新执行AOF文件中的命令来恢复数据。

## 
