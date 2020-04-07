package com.uetty.jedis.test;

import com.uetty.jedis.config.SimpleRemoteConfigure;
import com.uetty.jedis.lock.DistributedLock;

import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.LockSupport;

public class HighFrequencyTest {

    public static void main(String[] args) {

        SimpleRemoteConfigure configure = new SimpleRemoteConfigure();
        configure.setKeyPrefix("testLock");
        configure.setServerHost("127.0.0.1");

        DistributedLock distributedLock = new DistributedLock(configure);

        String tryThreadName = "tthread";
        String lockThreadName = "gthread";

        List<Thread> threads = new ArrayList<>();
        int keyNums = 9;
        int threadSize = 50;
        int tryNums = 0;
        int lockNums = 0;
        CyclicBarrier cyclicBarrier = new CyclicBarrier(threadSize);

        for (int i = 0; i < 18; i++) {
            String key = "key" + (i % keyNums + 1);
            threads.add(new T2(distributedLock, cyclicBarrier, key, tryThreadName + (++tryNums)));
        }
        for (int i = 18; i < threadSize; i++) {
            String key = "key" + (i % keyNums + 1);
            threads.add(new T1(distributedLock, cyclicBarrier, key, lockThreadName + (++lockNums)));
        }

        while (threads.size() > 0) {
            int size = threads.size();
            int random = (int)(Math.random() * 100) % size;
            threads.remove(random).start();
        }
        LockSupport.park();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    static class T1 extends Thread {

        DistributedLock distributedLock;

        CyclicBarrier cyclicBarrier;
        String key;
        String threadName;

        public T1(DistributedLock distributedLock, CyclicBarrier cyclicBarrier,
                  String key, String threadName) {
            this.distributedLock = distributedLock;
            this.cyclicBarrier = cyclicBarrier;
            this.key = key;
            this.threadName = threadName;
        }

        @Override
        public void run() {
            while (true) {
                DistributedLock.Lock lock = null;
                try {
                    cyclicBarrier.await();
                    lock = distributedLock.lock(key);
//                    long cur = System.currentTimeMillis();
                    long sleep = 5000 + (long)(Math.random() * 35000);
                    System.out.println(threadName + " get lock [" + key + "], token [" + lock.getToken() + "], sleep --> " + (sleep / 1000) + "s");

                    Thread.sleep(sleep);


                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                } finally {
                    if (lock != null) {
                        lock.unlock();
                        System.out.println(threadName + " unlock [" + key + "]");
                    }
                }
            }
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    static class T2 extends Thread {

        DistributedLock distributedLock;
        CyclicBarrier cyclicBarrier;
        String key;
        String threadName;

        public T2(DistributedLock distributedLock, CyclicBarrier cyclicBarrier,
                  String key, String threadName) {
            this.distributedLock = distributedLock;
            this.cyclicBarrier = cyclicBarrier;
            this.key = key;
            this.threadName = threadName;
        }

        @Override
        public void run() {
            while (true) {
                DistributedLock.Lock lock = null;
                try {
                    cyclicBarrier.await();
                    lock = distributedLock.tryLock(key, 6000);
                    if (lock == null) {
                        lock = distributedLock.tryLock(key, 6000);
                    }

                    long sleep = 5000 + (long)(Math.random() * 35000);
                    if (lock != null) {
//                        long cur = System.currentTimeMillis();
                        System.out.println(threadName + " get lock [" + key + "], token [" + lock.getToken() + "], sleep --> " + (sleep / 1000) + "s");
                    } else {
//                        System.out.println("thread " + threadName + " failed get lock [" + key + "] ");
                    }

                    Thread.sleep(sleep);


                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                } finally {
                    if (lock != null) {
                        lock.unlock();
                        System.out.println(threadName + " unlock [" + key + "]");
                    }
                }
            }
        }
    }
}
