package com.uetty.jedis.remote;

import com.uetty.jedis.config.LuaConfig;
import com.uetty.jedis.config.SimpleRemoteConfigure;
import com.uetty.jedis.lock.DistributedLock;
import com.uetty.jedis.thread.NamedThreadFactory;
import com.uetty.jedis.util.UuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class SimpleRemoteSynchronizer extends RemoteSynchronizer {

    final static Logger LOG = LoggerFactory.getLogger(DistributedLock.class);

    private final ConcurrentMap<String, WorkingNode> lockWorkingNodes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WorkingNode> unlockWorkingNodes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WorkingNode> renewalWorkingNodes = new ConcurrentHashMap<>();

    private SimpleRemoteConfigure configure;
    private ConcurrentHashMap<String, DistributedLock.Sync> lockPool;
    private String keyPrefix;
    private int batchSize;
    private int expireTime;
    private int renewalInterval;
    private RedisServer lockServer;
    private RedisServer unlockServer;
    private RedisServer renewalServer;

    private ThreadPoolExecutor lockThreadPool;
    private ThreadPoolExecutor unlockThreadPool;
    private ThreadPoolExecutor renewalThreadPool;

    private LockWorker lockWorker;
    private UnlockWorker unlockWorker;
    private RenewalWorker renewalWorker;

    public SimpleRemoteSynchronizer(SimpleRemoteConfigure configure, ConcurrentHashMap<String, DistributedLock.Sync> lockPool) {
        Objects.requireNonNull(configure);
        this.lockPool = lockPool;
        this.configure = configure;

        init();
    }

    private void init() {
        if (configure.getUri() != null) {
            lockServer = new RedisServer(configure.getUri(), configure.getSslSocketFactory(),
                    configure.getSslParameters(), configure.getHostnameVerifier());
            renewalServer = new RedisServer(configure.getUri(), configure.getSslSocketFactory(),
                    configure.getSslParameters(), configure.getHostnameVerifier());
            unlockServer = new RedisServer(configure.getUri(), configure.getSslSocketFactory(),
                    configure.getSslParameters(), configure.getHostnameVerifier());
        } else {
            lockServer = new RedisServer(configure.getServerHost(), configure.getServerPort(),
                    configure.isUseSSL(), configure.getSslSocketFactory(), configure.getSslParameters(),
                    configure.getHostnameVerifier());
            renewalServer = new RedisServer(configure.getServerHost(), configure.getServerPort(),
                    configure.isUseSSL(), configure.getSslSocketFactory(), configure.getSslParameters(),
                    configure.getHostnameVerifier());
            unlockServer = new RedisServer(configure.getServerHost(), configure.getServerPort(),
                    configure.isUseSSL(), configure.getSslSocketFactory(), configure.getSslParameters(),
                    configure.getHostnameVerifier());
        }
        keyPrefix = configure.getKeyPrefix();
        renewalInterval = configure.getRenewalInterval();
        expireTime = configure.getExpireTime();
        batchSize = configure.getBatchSize();

        ThreadPoolExecutor.DiscardPolicy discardPolicy = new ThreadPoolExecutor.DiscardPolicy();
        lockThreadPool = new ThreadPoolExecutor(1, 3, 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10), new NamedThreadFactory("olock-" + keyPrefix + "-lock"), discardPolicy);
        unlockThreadPool = new ThreadPoolExecutor(1, 3, 3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10), new NamedThreadFactory("olock-" + keyPrefix + "-unlock"), discardPolicy);
        renewalThreadPool = new ThreadPoolExecutor(1, 3,3, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10), new NamedThreadFactory("olock-" + keyPrefix + "-renewal"), discardPolicy);

        lockWorker = new LockWorker();
        unlockWorker = new UnlockWorker();
        renewalWorker = new RenewalWorker();

    }


    @Override
    public void notifySynchronize(Signal signal) {
        switch (signal) {
            case LOCK:
                lockThreadPool.execute(lockWorker);
                break;
            case UNLOCK:
                unlockThreadPool.execute(unlockWorker);
                break;
            case RENEWAL:
                renewalThreadPool.execute(renewalWorker);
                break;
        }
    }

    private List<WorkingNode> getLockNode() {
        List<WorkingNode> list = new ArrayList<>();
        synchronized (lockWorkingNodes) {
            for (String key : lockPool.keySet()) {
                try {
                    DistributedLock.Sync sync = lockPool.get(key);
                    if (sync == null || lockWorkingNodes.containsKey(key)
                            || (sync.getLockState() != DistributedLock.Sync.LOCK_STATE_WAIT_FEEDING
                            && sync.getLockState() != DistributedLock.Sync.LOCK_STATE_CANCEL_FEEDING)) {
                        continue;
                    }
                    WorkingNode node = new WorkingNode(key, sync);
                    list.add(node);

                } catch (Exception ignore) {}
            }
        }
        return list;
    }

    private List<WorkingNode> getUnlockNode() {
        List<WorkingNode> list = new ArrayList<>();
        synchronized (unlockWorkingNodes) {
            for (String key : lockPool.keySet()) {
                try {
                    DistributedLock.Sync sync = lockPool.get(key);
                    if (sync == null || sync.getLockState() != DistributedLock.Sync.LOCK_STATE_CUTLERY_CLEAN
                            || unlockWorkingNodes.containsKey(key)) {
                        continue;
                    }
                    WorkingNode node = new WorkingNode(key, sync);
                    list.add(node);

                } catch (Exception ignore) {}
            }
        }
        return list;
    }

    private List<WorkingNode> getRenewalNode() {
        synchronized (renewalWorkingNodes) {
            for (String key : lockPool.keySet()) {
                try {
                    DistributedLock.Sync sync = lockPool.get(key);
                    if (sync == null || sync.getLockState() != DistributedLock.Sync.LOCK_STATE_EATING
                            || renewalWorkingNodes.containsKey(key)) {
                        continue;
                    }
                    WorkingNode node = new WorkingNode(key, sync);
                    node.lastRenewal = System.currentTimeMillis();
                    renewalWorkingNodes.put(key, node);
                } catch (Exception ignore) {
                }
            }
        }

        List<WorkingNode> list = new ArrayList<>();
        for (String key : renewalWorkingNodes.keySet()) {
            WorkingNode node = renewalWorkingNodes.get(key);
            if (node != null && System.currentTimeMillis() - node.lastRenewal >= renewalInterval * 1000) {
                list.add(node);
            }
        }
        return list;
    }

    class LockWorker implements Runnable {
        private volatile AtomicLong serial = new AtomicLong(0);

        @Override
        public void run() {
            List<WorkingNode> nodes = getLockNode();
            if (nodes.size() == 0) {
                nodes = getLockNode();
            }
            if (nodes.size() == 0) {
                return;
            }
            
            while (nodes.size() > 0) {
                batchLock(nodes);
            }

            lockThreadPool.execute(this);
        }

        private List<WorkingNode> getKeyArgs(List<WorkingNode> nodes, List<List<byte[]>> keys, List<List<byte[]>> args) {
            List<WorkingNode> list = new ArrayList<>();
            synchronized (lockWorkingNodes) {
                while (nodes.size() > 0) {
                    WorkingNode node = nodes.remove(0);
                    if ((node.sync.getLockState() != DistributedLock.Sync.LOCK_STATE_WAIT_FEEDING
                            && node.sync.getLockState() != DistributedLock.Sync.LOCK_STATE_CANCEL_FEEDING)
                            || lockWorkingNodes.containsKey(node.key)) {
                        continue;
                    }
                    List<byte[]> key = new ArrayList<>();
                    List<byte[]> arg = new ArrayList<>();
                    key.add((keyPrefix + ":" + node.key).getBytes());
                    String token = UuidUtil.getUuid() + ":" + serial.incrementAndGet();
                    node.nextToken = token;
                    arg.add(token.getBytes());
                    arg.add(String.valueOf(expireTime).getBytes());
                    keys.add(key);
                    args.add(arg);
                    lockWorkingNodes.put(node.key, node);
                    list.add(node);
                    if (keys.size() >= batchSize) {
                        break;
                    }
                }
            }
            return list;
        }

        private void batchLock(List<WorkingNode> nodes) {
            List<List<byte[]>> keys = new ArrayList<>();
            List<List<byte[]>> args = new ArrayList<>();
            List<WorkingNode> workingNodes = getKeyArgs(nodes, keys, args);

            List<Object> result;
            //noinspection SynchronizeOnNonFinalField
            synchronized (lockServer) {
                try {
                    result = lockServer.execLuas(LuaConfig.LUA_NAME_LOCK, keys, args);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);

                    for (WorkingNode node : workingNodes) {
                        lockWorkingNodes.remove(node.key);
                    }
                    return;
                }
            }

            for (int i = 0; i < workingNodes.size(); i++) {
                try {
                    WorkingNode node = workingNodes.get(i);
                    Long r = (Long) result.get(i);

                    if (r != null && r == 1) {
                        modifyState(node);
                    }
                    lockWorkingNodes.remove(node.key);
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            }

        }

        private void modifyState(WorkingNode node) {
            node.sync.setLockToken(node.nextToken);
            if (!node.sync.changeLockState(DistributedLock.Sync.LOCK_STATE_WAIT_FEEDING, DistributedLock.Sync.LOCK_STATE_EATING)) {
                if (node.sync.changeLockState(DistributedLock.Sync.LOCK_STATE_CANCEL_FEEDING, DistributedLock.Sync.LOCK_STATE_CUTLERY_CLEAN)) {
                    notifySynchronize(Signal.UNLOCK);
                }
            } else {
                notifySynchronize(Signal.RENEWAL);
            }
        }
    }

    class UnlockWorker implements Runnable {
        @Override
        public void run() {
            List<WorkingNode> nodes = getUnlockNode();
            if (nodes.size() == 0) {
                nodes = getUnlockNode();
            }
            if (nodes.size() == 0) {
                return;
            }

            while (nodes.size() > 0) {
                batchUnlock(nodes);
            }
            unlockThreadPool.execute(unlockWorker);
        }

        private void batchUnlock(List<WorkingNode> nodes) {
            List<List<byte[]>> keys = new ArrayList<>();
            List<List<byte[]>> args = new ArrayList<>();
            List<WorkingNode> workingNodes = getKeyArgs(nodes, keys, args);

            //noinspection SynchronizeOnNonFinalField
            synchronized (unlockServer) {
                try {
                    unlockServer.execLuas(LuaConfig.LUA_NAME_UNLOCK, keys, args);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);

                    for (WorkingNode node : workingNodes) {
                        unlockWorkingNodes.remove(node.key);
                    }
                    return;
                }
            }

            for (WorkingNode node : workingNodes) {
                try {
                    node.sync.changeLockState(DistributedLock.Sync.LOCK_STATE_CUTLERY_CLEAN, DistributedLock.Sync.LOCK_STATE_NO_MEAL);
                    unlockWorkingNodes.remove(node.key);
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }

        private List<WorkingNode> getKeyArgs(List<WorkingNode> nodes, List<List<byte[]>> keys, List<List<byte[]>> args) {
            List<WorkingNode> list = new ArrayList<>();
            synchronized (unlockWorkingNodes) {
                while (nodes.size() > 0) {
                    WorkingNode node = nodes.remove(0);
                    if (node.sync.getLockState() != DistributedLock.Sync.LOCK_STATE_CUTLERY_CLEAN
                            || unlockWorkingNodes.containsKey(node.key)) {
                        continue;
                    }
                    if (node.sync.getLockToken() == null) {
                        node.sync.changeLockState(DistributedLock.Sync.LOCK_STATE_CUTLERY_CLEAN, DistributedLock.Sync.LOCK_STATE_NO_MEAL);
                        continue;
                    }
                    List<byte[]> key = new ArrayList<>();
                    key.add((keyPrefix + ":"  + node.key).getBytes());
                    List<byte[]> arg = new ArrayList<>();
                    arg.add(node.sync.getLockToken().getBytes());
                    arg.add(String.valueOf(expireTime).getBytes());
                    keys.add(key);
                    args.add(arg);
                    unlockWorkingNodes.put(node.key, node);
                    list.add(node);
                    if (keys.size() >= batchSize) {
                        break;
                    }
                }
            }
            return list;
        }
    }

    class RenewalWorker implements Runnable {
        @Override
        public void run() {
            List<WorkingNode> nodes = getRenewalNode();
            if (renewalWorkingNodes.size() == 0) {
                nodes = getRenewalNode();
            }
            if (renewalWorkingNodes.size() == 0) {
                return;
            }

            batchRenewal(nodes);

            LockSupport.parkNanos(200_000_000);
            renewalThreadPool.execute(renewalWorker);
        }

        private void batchRenewal(List<WorkingNode> nodes) {
            //noinspection SynchronizeOnNonFinalField
            synchronized (renewalServer) {
                try {
                    renewalServer.execByPipeline(pipeline -> {
                        for (WorkingNode node : nodes) {
                            pipeline.expire(keyPrefix + ":"  + node.key, expireTime);
                        }
                    });
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                    return;
                }
            }
            for (WorkingNode node : nodes) {
                node.lastRenewal = System.currentTimeMillis();
            }
        }

    }

    @SuppressWarnings("InnerClassMayBeStatic")
    class WorkingNode {
        String key;
        String nextToken;
        DistributedLock.Sync sync;
        long lastRenewal;

        public WorkingNode(String key, DistributedLock.Sync sync) {
            this.key = key;
            this.sync = sync;
            lastRenewal = System.currentTimeMillis();
        }
    }
}
