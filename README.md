# 使用java基于raft协议实现的kv数据库

## 简介
 1. 使用java实现的raft协议，在半数以上节点正常时服务可用，系统满足线性一致性；
 2. 使用netty实现底层通信，利用protobuf进行编解码；
 3. 使用rocksDB进行日志和状态机的持久化，数据基于磁盘，消耗内存小；
 4. 使用rocksDB存储状态机快照，对日志落后过多的节点传输快照可以快速恢复数据；
 5. 主节点请求复制日志的缓冲区会根据并发量动态调配，既能适应高吞吐量场景又能降低单次请求的响应时间；
 6. 支持简单kv操作，支持过期时间的设置。

## 简单使用
### 一、搭建服务端集群
    
 ```
    #运行jar即可(第一次启动jar会创建默认配置文件以及日志、状态机、快照文件目录，如需更换端口可修改对应配置)

    #本机地址端口
    ip=127.0.0.1
    port=8040
    #所有节点
    nodes=127.0.0.1:8040,127.0.0.1:8041,127.0.0.1:8042
```
### 二、客户端使用

-  配置服务端节点，可以写在配置文件中，也可使用api配置
```
  //这里简单使用api进行设置节点
  PropertiesUtil.setValue("nodes", "127.0.0.1:8041,127.0.0.1:8042,127.0.0.1:8043");
```
- 同步调用(所有修改操作均支持过期时间的设置)
```
    StringKvOperations operations = new StringKvOperations();
    //设值
    operations.opsForValue().set("test","v1");
    //删除
    operations.opsForValue().delete("test");
    //设置key，并附带过期时间
    operations.opsForValue().set("test","v1",1000L);
    //存在时修改
    boolean setIfPresent = operations.opsForValue().setIfPresent("test", "0");
    //不存在时修改
    boolean setIfAbsent = operations.opsForValue().setIfAbsent("test", "0");
    //设置过期时间
    boolean expire = operations.opsForValue().expire("test", 100L);
    //获取
    String testValue = operations.opsForValue().get("test");
    //自增
    long incr = operations.opsForValue().incr("test");
    long incrBy = operations.opsForValue().incrBy("test",10L);
    //自减
    long decr = operations.opsForValue().decr("test");
    //是否存在
    boolean hasKey = operations.opsForValue().hasKey("test");
```

- 异步调用（数据操作和同步api一致，增加回调函数即可）
```
    StringKvOperations operations = new StringKvOperations();
    //异步自增
    operations.opsForValueAsync().incr("async", v -> {
         //自增成功后调用
         System.out.println("async incr:"+v)
   });
```
    


