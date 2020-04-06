package com.uetty.jedis;

import com.uetty.jedis.config.RemoteConfigure;
import com.uetty.jedis.config.SimpleRemoteConfigure;
import com.uetty.jedis.lock.DistributedLock;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.LockSupport;

public class App {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("---------------");
        long currentTimeMillis = System.currentTimeMillis();
        LockSupport.parkNanos(DistributedLock.WAIT_TIME_UNIT);

        System.out.println("pass --> " + (System.currentTimeMillis() - currentTimeMillis));

        SimpleRemoteConfigure configure = new SimpleRemoteConfigure();
        configure.setKeyPrefix("testLock:");
        configure.setServerHost("127.0.0.1");

        DistributedLock distributedLock = new DistributedLock(configure);

        T1 t1 = new T1(distributedLock);
        T2 t2 = new T2(distributedLock);

        t1.start();;
        t2.start();

        LockSupport.park();
    }

    public static class T2 extends Thread {
        DistributedLock distributedLock;

        public T2(DistributedLock distributedLock) {
            this.distributedLock = distributedLock;
        }

        @Override
        public void run() {
            String threadName = this.getName();
            while (true) {
                DistributedLock.Lock lock = distributedLock.lock("keyaaa");
                System.out.println(threadName + " getLock " + lock.getToken() + "--> " + (System.currentTimeMillis() / 1000));
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("unlock");
                lock.unlock();
            }
        }
    }

    public static class T1 extends Thread {
        DistributedLock distributedLock;

        public T1(DistributedLock distributedLock) {
            this.distributedLock = distributedLock;
        }

        @Override
        public void run() {
            String threadName = this.getName();
            while (true) {
                DistributedLock.Lock lock = distributedLock.lock("keyaaa");
                System.out.println(threadName + " getLock " + lock.getToken() + "--> " + (System.currentTimeMillis() / 1000));
                try {
                    Thread.sleep(40000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("unlock");
                lock.unlock();
            }
        }
    }
}
