# online-lock

基于Redis实现的分布式锁，采用客户端主动续期机制实现的Redis分布式锁，期望解决目前常见Redis分布式锁存在的本地线程未结束但远程服务器锁已过期的问题。

### 设计结构

该分布式锁，主要由**本地线程管理器**和**远程同步插件**这两大部件组成。

![设计结构图](https://raw.githubusercontent.com/Uetty/online-lock-parent/master/doc/img/redis-lock-struct.png)

**本地线程管理器**：负责将本地请求同一个锁的线程排队，确保一把锁只有一条线程在等待，其他线程暂时驻留，减少cpu资源的消耗。

**远程同步插件**：负责与远程服务器（Redis）同步锁信息（申请、保活、释放），同时它作为一个插件，是可以根据需要选择的（虽然目前版本暂时只有单节点Redis服务器这个版本的同步插件）。



对于本地线程同步器，在线程排队的实现上，是借助JDK的AQS抽象类实现的，因此它有AQS本身带有的CAS乐观锁字段`state`，当`state>0`时，表示当前被某个线程占有了`state`次。为了能够线程安全地在本地线程管理器和远程同步插件之间同步数据，本地线程管理器上增加了`lockState`字段，同样使用CAS的机制进行数据的同步（后面有可能会更换为synchronized实现）。`lockState`字段共存在5种状态，为了理解方便，使用用餐术语解释这几种状态：

1. 状态值0，无需进食（即没有业务线程需要从远程服务器获取锁）
2. 状态值1，等待投食（即已有业务线程做好准备，等待同步器从远程服务器获取锁，更新给它）
3. 状态值2，取消进食（在tryLock方法达到等待时间仍没获取到锁的情况下，会取消等待锁）
4. 状态值3，正在进食（即同步器已从远程服务器获取锁，并已锁更新给它，业务线程继续执行，等待业务代码执行完后释放锁）
5. 状态值4，收拾餐具（即业务线程调用了释放锁的方法，但同步器未将释放锁这个操作更新到远程服务器）

### 使用方法

目前仅存在一个适配单Redis服务器（非集群）的远程同步插件SimpleRemoteSynchronizer，它使用SimpleRemoteConfigure来配置。使用方法如下：

```
SimpleRemoteConfigure configure = new SimpleRemoteConfigure();
configure.setKeyPrefix("lockinst");
configure.setServerHost("127.0.0.1");
DistributedLock distributedLock = new DistributedLock(configure);
try {
    DistributedLock.Lock lock = distributedLock.lock("lockname");
    // TODO 业务代码
    // ...
} finally {
    lock.unlock();
}
```

### 联系 
欢迎大家帮忙测试、[提交BUG](https://github.com/Uetty/online-lock-parent/issues)、贡献代码

↘[个人邮箱](mailto:vincent_field@foxmail.com)

