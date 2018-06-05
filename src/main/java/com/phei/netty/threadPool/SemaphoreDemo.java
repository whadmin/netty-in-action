package com.phei.netty.threadPool;


import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class SemaphoreDemo {
    private Semaphore smp = new Semaphore(2); 
    private Random rnd = new Random();
    
    class TaskDemo implements Runnable{
        private String id;
        TaskDemo(String id){
            this.id = id;
        }
        @Override
        public void run(){
            try {
                smp.acquire();
                System.out.println("Thread " + id + " is working");
                Thread.sleep(rnd.nextInt(1000));
                smp.release();
                System.out.println("Thread " + id + " is over");
            } catch (InterruptedException e) {
            }
        }
    }
    
    public static void main(String[] args){
        SemaphoreDemo semaphoreDemo = new SemaphoreDemo();
        //注意我创建的线程池类型，
        ExecutorService se = Executors.newCachedThreadPool();
        se.submit(new Thread(semaphoreDemo.new TaskDemo("a"),"a"));
        se.submit(new Thread(semaphoreDemo.new TaskDemo("b"),"b"));
        se.submit(new Thread(semaphoreDemo.new TaskDemo("c"),"c"));
        se.submit(new Thread(semaphoreDemo.new TaskDemo("d"),"d"));
        se.submit(new Thread(semaphoreDemo.new TaskDemo("e"),"e"));
        se.submit(new Thread(semaphoreDemo.new TaskDemo("f"),"f"));
        se.shutdown();
    }
}
