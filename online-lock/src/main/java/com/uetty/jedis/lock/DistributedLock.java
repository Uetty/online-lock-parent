package com.uetty.jedis.lock;

import com.uetty.jedis.config.RemoteConfigure;
import com.uetty.jedis.remote.RemoteSynchronizer;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.LockSupport;

/**
 * 分布式锁实现类
 */
public class DistributedLock {

    private final ConcurrentHashMap<String, Sync> lockPool;
    private volatile RemoteSynchronizer remoteSynchronizer;

    public static final long WAIT_TIME_UNIT = 200_000L; // nano second
    private static final long WAIT_USE_PARK_THRESHOLD = 1500_000_000; // milli second

    /**
     * 初始化方法
     * @param remoteConfigure 远程服务器配置参数
     */
    public DistributedLock(RemoteConfigure remoteConfigure) {
        this.lockPool = new ConcurrentHashMap<>();
        remoteSynchronizer = remoteConfigure.createRemoteSynchronizer(lockPool);
    }

    /**
     * 初始化对应键名的锁
     * @param key 锁键名
     * @param fair 是否公平（仅本地公平）
     */
    public void initKey(String key, boolean fair) {
        Objects.requireNonNull(key);
        lockPool.putIfAbsent(key, new Sync(fair));
    }

    /**
     * 阻塞请求锁
     * @param key 锁键名
     * @return 返回锁
     */
    public Lock lock(String key) {
        return lock(key, false);
    }

    /**
     * 尝试获取锁（非公平方式），不一定获取到
     * @param key 锁键名
     * @param waitMillis 等待毫秒数
     * @return 返回锁或者未获取到时返回null
     */
    public Lock tryLock(String key, long waitMillis) {
        return tryLock(key, false, waitMillis);
    }

    /**
     * 请求分布式锁
     * @param key 锁键名
     * @param fair 是否公平锁（仅在锁未初始化时有效）
     * @return 返回锁
     */
    public Lock lock(String key, boolean fair) {
        initKey(key, fair);
        Sync sync = lockPool.get(key);
        sync.lock();

        return new Lock(key, sync.getLockToken());
    }

    /**
     * 尝试获取锁（非公平方式），不一定获取到
     * @param key 锁键名
     * @param fair 仅在锁初始化时指定是否公平，当前方法一定是不公平的获取
     * @param waitMillis 等待毫秒数
     * @return 返回锁或者未获取到时返回null
     */
    public Lock tryLock(String key, boolean fair, long waitMillis) {
        initKey(key, fair);
        long nanos = System.nanoTime();
        Sync sync = lockPool.get(key);
        boolean isSuccess = sync.tryLock(nanos, waitMillis * 1000_000);
        if (isSuccess) {
            return new Lock(key, sync.getLockToken());
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public final class Lock {
        private String key;
        private String token;

        Lock(String key, String token) {
            this.key = key;
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void unlock() {
            Sync sync = lockPool.get(key);
            sync.unlock();
        }
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class Sync extends AbstractQueuedSynchronizer {

        volatile boolean fair; // 是否公平锁
        volatile AtomicInteger lockState; // 分布式锁状态
        volatile String lockToken; // 分布式锁token

        /**
         * 无需进食（没有业务线程等待锁，也没有线程持有锁）
         */
        public static final int LOCK_STATE_NO_MEAL = 0;
        /**
         * 等待投食（业务线程正在等待远程同步器获取锁）
         */
        public static final int LOCK_STATE_WAIT_FEEDING = 1;
        /**
         * 取消进食（tryLock时间过期，取消lock）
         */
        public static final int LOCK_STATE_CANCEL_FEEDING = 2;
        /**
         * 正在进食（远程同步器已投食，业务线程正在处理）
         */
        public static final int LOCK_STATE_EATING = 3;
        /**
         * 收拾餐具（业务线程在本地释放锁，等待远程同步器清理）
         */
        public static final int LOCK_STATE_CUTLERY_CLEAN = 4;

        private Sync(boolean fair) {
            this.fair = fair;
            lockState = new AtomicInteger(0);
        }

        public int getLockState() {
            return lockState.get();
        }

        public boolean changeLockState(int expect, int state) {
            return lockState.compareAndSet(expect, state);
        }

        private void setLockStateCancel() {
            setState(0);
            lockState.set(LOCK_STATE_CANCEL_FEEDING);
        }

        @SuppressWarnings("unused")
        public void setLockToken(String lockToken) {
            this.lockToken = lockToken;
        }

        public String getLockToken() {
            return this.lockToken;
        }

        protected void lock() {
            if (!fair && compareAndSetState(0, 1)) { // 非公平的会进行一次尝试，失败的情况下才会进入排队
                // 切换资源状态
                while (!changeLockState(LOCK_STATE_NO_MEAL, LOCK_STATE_WAIT_FEEDING)) {
                    LockSupport.parkNanos(WAIT_TIME_UNIT);
                }
                setExclusiveOwnerThread(Thread.currentThread());
                sendSignal(RemoteSynchronizer.Signal.LOCK);

                while (lockState.get() != Sync.LOCK_STATE_EATING) {
                    LockSupport.parkNanos(WAIT_TIME_UNIT);
                }
                return;
            }
            acquire(1);
        }

        public void sendSignal(RemoteSynchronizer.Signal signal) {
            remoteSynchronizer.notifySynchronize(signal);
        }

        protected boolean tryLock(long startNanoSeconds, long waitNanoSeconds) {

            while (System.nanoTime() - startNanoSeconds < waitNanoSeconds) {
                if (compareAndSetState(0, 1)) {
                    // 切换资源状态
                    while (!changeLockState(LOCK_STATE_NO_MEAL, LOCK_STATE_WAIT_FEEDING)) {
                        long restTime = waitNanoSeconds - (System.currentTimeMillis() - startNanoSeconds);
                        if (restTime > WAIT_USE_PARK_THRESHOLD) {
                            LockSupport.parkNanos(WAIT_TIME_UNIT);
                        } else if (restTime <= 0) {
                            compareAndSetState(1, 0);
                            return false;
                        }
                    }
                    setExclusiveOwnerThread(Thread.currentThread());

                    sendSignal(RemoteSynchronizer.Signal.LOCK);

                    while (lockState.get() != Sync.LOCK_STATE_EATING) {
                        long restTime = waitNanoSeconds - (System.currentTimeMillis() - startNanoSeconds);
                        if (restTime > WAIT_USE_PARK_THRESHOLD) {
                            LockSupport.parkNanos(WAIT_TIME_UNIT);
                        } else if (restTime <= 0) {
                            setExclusiveOwnerThread(null);

                            setLockStateCancel();

                            sendSignal(RemoteSynchronizer.Signal.LOCK);

                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @SuppressWarnings("UnusedReturnValue")
        protected boolean unlock() {
            return release(1);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() && compareAndSetState(c, arg)) {
                    setExclusiveOwnerThread(current);

                    // 切换资源状态
                    while (!changeLockState(LOCK_STATE_NO_MEAL, LOCK_STATE_WAIT_FEEDING)) {
                        LockSupport.parkNanos(WAIT_TIME_UNIT);
                    }
                    setExclusiveOwnerThread(Thread.currentThread());

                    sendSignal(RemoteSynchronizer.Signal.LOCK);

                    while (lockState.get() != Sync.LOCK_STATE_EATING) {
                        LockSupport.parkNanos(WAIT_TIME_UNIT);
                    }

                    return true;
                }
            } else {
                if (current == getExclusiveOwnerThread()) {
                    int next = c + arg;
                    if (next < 0) {
                        throw new Error("Maximum lock count exceeded");
                    }
                    setState(next);
                    return true;
                }
            }

            return false;
        }

        @Override
        protected boolean tryRelease(int release) {
            int c = getState() - release;
            if (Thread.currentThread() != getExclusiveOwnerThread()) {
                return false;
            }
            boolean free = false;
            if (c == 0) { // 判断可重入锁，多次加锁后是否释放到0
                free = true;
                setExclusiveOwnerThread(null);
                changeLockState(LOCK_STATE_EATING, LOCK_STATE_CUTLERY_CLEAN);
            }
            setState(c);

            sendSignal(RemoteSynchronizer.Signal.UNLOCK);

            return free;
        }
    }
}
