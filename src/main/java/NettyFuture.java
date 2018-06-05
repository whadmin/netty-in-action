import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class NettyFuture {
    
    public static void main(String[] args) {
         DefaultEventExecutor wang = new DefaultEventExecutor();
         DefaultEventExecutor li   = new DefaultEventExecutor();
         Future<Integer> promise = wang.newPromise();
         promise.addListener(new GenericFutureListener<Future<? super Integer>>() {

             @Override
             public void operationComplete(Future<? super Integer> future) throws Exception {
                 System.out.println(Thread.currentThread().getName() + " 写入结果:" + future.getNow());
             }
         });
         
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
    }

}
