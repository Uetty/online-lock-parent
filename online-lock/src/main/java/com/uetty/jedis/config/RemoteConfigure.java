package com.uetty.jedis.config;

import com.uetty.jedis.remote.RemoteSynchronizer;

/**
 * 远程服务器配置参数抽象类
 * <p>默认{@link SimpleRemoteConfigure}</p>
 */
public abstract class RemoteConfigure {

    public abstract RemoteSynchronizer createRemoteSynchronizer();
}
