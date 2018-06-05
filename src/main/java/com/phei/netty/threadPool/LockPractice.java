package com.phei.netty.threadPool;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockPractice {
    private static int a=0;
    private static Lock lock=new ReentrantLock();
    
    public static void increateBySynchronized(){
        a=0;
        for (int i = 0; i < 1000; i++) {
            Thread t=new Thread(new Runnable() {
                public void run() {
                    for (int j = 0; j < 100; j++) {
                        synchronized (LockPractice.class) {
                            a++;
                        }
                    }
                }
            });
            t.start();
        }
        while (Thread.activeCount()>1) {
            Thread.yield();
        }
        System.out.println(a);
    }
    
    public static void increateByLock(){
        a=0;
        for (int i = 0; i < 10; i++) {
            Thread t=new Thread(new Runnable() {
                public void run() {
                    for (int j = 0; j < 10; j++) {
                        lock.lock();
                        try {
                            a++;
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            });
            t.start();
        }
        while (Thread.activeCount()>1) {
            Thread.yield();
        }
        System.out.println(a);
    }
    
    public static void main(String[] args) {
        //increateBySynchronized();
        increateByLock();
    }
}
