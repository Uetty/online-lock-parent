package com.uetty.jedis.remote;

import com.uetty.jedis.lock.DistributedLock;

import java.util.concurrent.ConcurrentHashMap;

public abstract class RemoteSynchronizer {

    public enum Signal {
        LOCK,
        UNLOCK,
        RENEWAL,
        ;
    }

    public abstract void notifySynchronize(Signal signal);


}
