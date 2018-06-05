package com.phei.netty.threadPool;


import java.util.concurrent.locks.Lock;  
import java.util.concurrent.locks.ReentrantLock;  


/**
 *  A    B    C   D   个线程顺序获得锁
 *  
 *  A 线程获得锁
 *  
 *  B 线程
 *  
 *   addnode
 *   
 *   node   -----> node  
 *   tail   -----> node
 *   head   -----> node
 *   
 *   
 *   
 *   tail  ---->addnode  {
 *                         prev -----> node
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * @author wuhao.w
 *
 */
public class PrintQueue {  
      
    private final Lock queueLock = new ReentrantLock();  
      
    public void printJob(Object document) {  
        queueLock.lock();  
        Long duration=(long)(2000);  
        System.out.println(Thread.currentThread().getName()+ ":PrintQueue: Printing a Job during "+(duration/1000)+" seconds");  
        try {  
            Thread.sleep(duration);  
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        }finally{  
            queueLock.unlock();  
        }  
    }  
      
    public static void main(String[] args) {  
        PrintQueue printQueue = new PrintQueue();  
        Thread thread[] = new Thread[10];  
        for(int i = 0; i < 3; i++) {  
            thread[i] = new Thread(new Job(printQueue), "Thread " + i);  
        }  
        for(int i = 0; i < 3; i++) {  
            thread[i].start();  
        }  
    }  
} 

 class Job implements Runnable {  
    
    private PrintQueue printQueue;  
  
    public Job(PrintQueue printQueue) {  
        this.printQueue = printQueue;  
    }  
  
    @Override  
    public void run() {  
        System.out.printf("%s: Going to print a document\n", Thread  
                .currentThread().getName());  
        printQueue.printJob(new Object());  
        System.out.printf("%s: The document has been printed\n", Thread  
                .currentThread().getName());  
    }  
  
}  