import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

/**
 * li 是一家公司,有一个工作队只有一个人(单个线程的线程池  DefaultEventExecutor) 负责统计计算工作
 * wang 是一家公司 ,有一个工作队只有一个人(单个线程的线程池  DefaultEventExecutor) 负责数据写入工作
 * 
 * wang 是调用者  li 是执行者  且2者是异步
 * 
 * 
 * 工作流程 1：
 *  wang 约定了一个结果的存放处 A (promise）
 *  wang 给自己工人分配了一个写入工作
 *  wang 委托给li一个统计计算任务，并告知其结果存放处（传参promise）,当li计算完成后把结果写入A 并通知wang（promise.setSuccess(10)）
 * 
 * 工作流程 2：
 *  wang 约定了一个结果的存放处 promise
 *  wang 委托给li一个统计计算任务，并告知li完成后把结果填入A（传参promise）
 *  wang 不断查看A 看是否有结果 promise.await()
 */
public class PromiseTest {

    public static void main(String[] args) throws Exception {
        // isSuccessTest();
        // isErrorTest();
        // cancelTest();
        // DeadLock();
        errorExample();
    }

    /**
     * wang 一个线程等待 li 一个线程的计算结果 那么笔记 li先计算出结果 wang在拿到结果去写入 2个线程互不影响 如果 li
     **/
    public static void DeadLock() throws Exception {
        /** 创建wang,li 工作组,li负责计算,wang负责写入 **/
        final DefaultEventExecutor wang = new DefaultEventExecutor();
        final DefaultEventExecutor li = new DefaultEventExecutor();
        /** step 2 wang创建一个Promise 监听li的计算数据写入 **/
        Promise<Integer> promise = wang.newPromise();

        wang.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    System.out.println(Thread.currentThread().getName() + " 计算结果:" + promise.await());
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        promise.addListener(new GenericFutureListener<Future<? super Integer>>() {

            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                System.out.println(Thread.currentThread().getName() + " 写入结果:" + future.getNow());
            }
        });

        wang.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    System.out.println(Thread.currentThread().getName() + " 计算结果:" + 10);
                    promise.setSuccess(10);
                } catch (Exception e) {
                }
            }
        });

    }

    /**
     * 计算中取消 result= CANCELLATION_CAUSE_HOLDER = new CauseHolder(new CancellationException())
     */
    public static void cancelTest() throws Exception {
        /** 创建wang,li 工作组,li负责计算,wang负责写入 **/
        final DefaultEventExecutor wang = new DefaultEventExecutor();
        final DefaultEventExecutor li = new DefaultEventExecutor();

        /** wang 约定了一个结果的存放处 **/
        Promise<Integer> promise = wang.newPromise();
        /** 流程一 wang添加了一个监控当li计算完成结果后通知并执行写入结果 **/
        promise.addListener(new GenericFutureListener<Future<? super Integer>>() {

            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                System.out.println(Thread.currentThread().getName() + " 写入结果:" + future.getNow());
            }
        });
        // 等一会
        Thread.sleep(1000);
        /** step 3 li 工作组负责计算结果 **/
        li.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    promise.cancel(true);
                } catch (InterruptedException e) {
                }
            }
        });
        System.out.println(promise.await());
        System.out.println(promise.await().getNow());
        System.out.println(promise.isDone());
        System.out.println(promise.isSuccess());
    }

    /** 计算成功await()等待结果 **/
    public static void isSuccessTest() throws Exception {
        /** 创建wang,li 工作组,li负责计算,wang负责写入 **/
        final DefaultEventExecutor wang = new DefaultEventExecutor();
        final DefaultEventExecutor li = new DefaultEventExecutor();

        /** wang 约定了一个结果的存放处 **/
        Promise<Integer> promise = wang.newPromise();
        /** 流程一 wang添加了一个监控当li计算完成结果后通知并执行写入结果 **/
        promise.addListener(new GenericFutureListener<Future<? super Integer>>() {

            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                System.out.println(Thread.currentThread().getName() + " 写入结果:" + future.getNow());
            }
        });
        // 等一会
        Thread.sleep(1000);
        /** li 把promise给li工作组负责计算结果 **/
        li.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    System.out.println(Thread.currentThread().getName() + " 计算结果:" + 10);
                    promise.setSuccess(10);
                } catch (InterruptedException e) {
                }
            }
        });
        /** 流程二 wang不断查看A 看是否有结果 **/
        System.out.println(promise.await());
        System.out.println(promise.await().getNow());
        System.out.println(promise.isDone());
        System.out.println(promise.isSuccess());
    }

    /**
     * 计算出现异常 sync()获取promise抛出异常 await()获取promise正常 result= new CauseHolder(cause) getNow()=null
     **/
    public static void isErrorTest() throws Exception {
        /** 创建wang,li 工作组,li负责计算,wang负责写入 **/
        final DefaultEventExecutor wang = new DefaultEventExecutor();
        final DefaultEventExecutor li = new DefaultEventExecutor();

        /** wang 约定了一个结果的存放处 **/
        Promise<Integer> promise = wang.newPromise();
        /** 流程一 wang添加了一个监控当li计算完成结果后通知并执行写入结果 **/
        promise.addListener(new GenericFutureListener<Future<? super Integer>>() {

            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                System.out.println(Thread.currentThread().getName() + " 写入结果:" + future.getNow());
            }
        });
        // 等一会
        Thread.sleep(1000);
        /** step 3 li 工作组负责计算结果 **/
        li.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    throw new RuntimeException();
                } catch (Exception e) {
                    promise.setFailure(e);
                }

            };
        });
        // 抛出异常
        // System.out.println(promise.sync());
        // 正常返回
        System.out.println(promise.await());
        System.out.println(promise.await().getNow());
        System.out.println(promise.isDone());
        System.out.println(promise.isSuccess());
    }

    /** li自己调用并通知自己 **/
    public static void errorExample() {
        final DefaultEventExecutor li = new DefaultEventExecutor();
        Future<String> result = li.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {
                System.out.println(Thread.currentThread().getName() + " 计算结果:" + 10);
                return "20";
            }
        });

        result.addListener(new GenericFutureListener<Future<? super String>>() {

            @Override
            public void operationComplete(Future<? super String> future) throws Exception {
                System.out.println(Thread.currentThread().getName() + " 写入结果:" + future.getNow());
            }
        });
    }

}
