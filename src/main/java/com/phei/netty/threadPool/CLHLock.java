package com.phei.netty.threadPool;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 
 * 
 * A线程   B线程   C线程 顺序获得锁的
 * 
 * A 线程获得锁
 * 
 *  head ------>   tail初始化node {locked=false
 *                                 next          ------> A线程node {locked = true
 *                                                                  next   = null       <--------- tail 
 *                                                  
 * B 线程获得锁失败                                                 
 * 
 *  head ------>   tail初始化node {locked=false                                              b线程pred 指向A线程locked
 *                                 next          ------> A线程node {locked = true
 *                                                                  next   =       ------>   B线程node {locked = true
 *                                                                                                      next              <--------- tail                                                      
 * A线程   B线程   C线程 顺序释放锁的  
 * 
 * A 线程node(false)  --->  B 线程node(false)  --->   C 线程node(false)                                               
 * @author wuhao.w
 *
 */
public class CLHLock {  
    private static class Node {  
        private volatile boolean locked = false;//锁状态  
        private volatile Node next;
        private String   name;
    }  
  
    private AtomicReference<Node> tail = new AtomicReference<>();  
    private AtomicReference<Node> head = new AtomicReference<>();  
  
    public CLHLock() {  
        Node node = new Node();  
        head.set(node);  
        tail.set(node);  
    }  
    //线程获取锁之后进入锁队列，locked设置为true，后面的线程将阻塞  
    public void lock(int key) {  
        Node newNode = new Node();  
        newNode.locked = true; 
        newNode.name=Thread.currentThread().getName();
        Node pred = null;  
        while (true) {  
            pred = tail.get();  
            if (tail.compareAndSet(pred, newNode)) {  
                pred.next = newNode;  
                break;  
            }  
        }  
        while (pred.locked) {  
        }  
    }  
    //线程释放锁后将自己从锁队列中移除，并将locked修改为false  
    public void unlock(int key) {  
        Node h = head.get();  
        Node next = h.next;  
        while (next != null) {  
            if (head.compareAndSet(h, next)) {  
                next.locked = false;  
                break;  
            }  
            h = head.get();  
            next = h.next;  
        }  
    }
    
    public static void main(String[] args) throws InterruptedException {
        final CLHLock lock = new CLHLock();
        lock.lock(1);

        for (int i = 0; i < 3; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    lock.lock(1);
                    System.out.println(Thread.currentThread().getName() + " acquired the lock!");
                    lock.unlock(1);
                }
            },"thrad"+i).start();
            Thread.sleep(100);
        }

        System.out.println("main thread unlock!");
        lock.unlock(1);
    }
}  