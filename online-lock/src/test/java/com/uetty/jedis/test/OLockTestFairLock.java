package com.uetty.jedis.test;

import com.uetty.jedis.config.SimpleRemoteConfigure;
import com.uetty.jedis.lock.DistributedLock;

import java.util.concurrent.locks.LockSupport;

/**
 * 测试本地线程请求锁的公平性（不能用tryLock，tryLock本身就不会公平），
 * 无法做到跨分布式的公平，要做到分布式公平，需要依赖服务器端提供功能
 */
public class OLockTestFairLock {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("---------------");
        long currentTimeMillis = System.currentTimeMillis();
        LockSupport.parkNanos(DistributedLock.WAIT_TIME_UNIT);

        System.out.println("pass --> " + (System.currentTimeMillis() - currentTimeMillis));

        SimpleRemoteConfigure configure = new SimpleRemoteConfigure();
        configure.setKeyPrefix("testLock");
        configure.setServerHost("127.0.0.1");

        DistributedLock distributedLock = new DistributedLock(configure);
        distributedLock.initKey("keyaaa", true); // 将锁keyaaa设置为公平锁

        OLockTestWithTry1.T1 t1 = new OLockTestWithTry1.T1(distributedLock);
        OLockTestWithTry1.T2 t2 = new OLockTestWithTry1.T2(distributedLock);
        OLockTestWithTry1.T2 t22 = new OLockTestWithTry1.T2(distributedLock);
//        OLockTestWithTry1.T3 t3 = new OLockTestWithTry1.T3(distributedLock);
        OLockTestWithTry1.T4 t4 = new OLockTestWithTry1.T4(distributedLock);


        t1.start();;
        t2.start();
//        t3.start();
//        t4.start();
        t22.start();

        LockSupport.park();
    }
}
