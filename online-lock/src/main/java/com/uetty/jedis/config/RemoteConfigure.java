package com.uetty.jedis.config;

import com.uetty.jedis.lock.DistributedLock;
import com.uetty.jedis.remote.RemoteSynchronizer;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程服务器配置参数抽象类
 * <p>默认{@link SimpleRemoteConfigure}</p>
 */
public abstract class RemoteConfigure {

    public abstract RemoteSynchronizer createRemoteSynchronizer(ConcurrentHashMap<String, DistributedLock.Sync> lockPool);
}
