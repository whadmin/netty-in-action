package com.netty.learning;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程池实现类
 *
 * 线程池好比一个工厂，作为一个工厂几大要素必不可少 "订单"，"员工"，同时还需要管理机制以应对市场的需求。
 * 线程池中 “订单” 对应的是Runnable,而“员工” 对应的是Worker
 *
 * 线程池和工厂一样采用生产消费者模型，
 *
 * 线程池会对新提交的Runnable先会添加一个workQueue中，
 * Worker会从workQueue取出Runnable去执行
 *
 * 创建线程池和创建一个工厂差不多，不过这个工厂为了最大限度控制成本，创建出一个员工没有，只有接收订单才去雇佣员工处理订单
 *
 * 线程池和工厂一样需要由管理策略
 *
 * 工厂为了维持管理自身的成本，会雇佣一定数量正式员工为工厂服务，在线程池中表示为CorePoolSize，遇到订单高峰期也会雇佣一些临时员工来处理，而高峰过了就会辞退长期空闲的临时员工
 *
 * 当获取新任务时：
 *
 * 发现员工数量少于CorePoolSize（正式员工离职），工厂会雇佣一个正式员工来完成新任务
 *
 * 发现员工数量大于CorePoolSize，将任务加入工作队列，
 *
 * 发现员工数量大于CorePoolSize，工作队列装不下了，雇佣新临时员工将任务直接分配给临时员工，
 *
 *
 */
public class ThreadPoolExecutor extends AbstractExecutorService {

    /**
     * 用于记录线程池池的 状态和当前待完成任务数量
     * 前3位记录线程池状态
     * 后29位记录运行work数量
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));

    /** Java 中Integer 类型长度为32位，线程池用一个int类型的前3位表示线程池的状态**/
    private static final int COUNT_BITS = Integer.SIZE - 3;

    /** 用来计算出当前线程池状态中间变量，同时也表示work最大数量
     *  00011111 11111111 11111111 11111111
     **/
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    /** -----------------线程池状态----------------- **/

    /**
     * 线程池RUNNING状态,当前状态下线程池可以接收新的任务，对新接收的任务进行处理，
     * 工厂正常运行
     *
     * -1 二进制 11111111111111111111111111111111 左移动 29位 前三位 111
     */
    private static final int RUNNING    = -1 << COUNT_BITS;

    /**
     * 线程池SHUTDOWN状态,当前状态下线程池不在接收新任务，对之前接收的任务(其中包括还在队列等待和正在执行的任务)
     *  工厂不在接收新的订单,工厂运行出现了问题
     *
     *  0 二进制 00000000000000000000000000000000 左移动 29位 前三位 000
     */
    private static final int SHUTDOWN   =  0 << COUNT_BITS;

    /**
     * 线程池STOP状态,当前状态下线程池不在接收新任务，对之前接收的任务存在队列没有处理的不在处理，正在执行做中断
     *  工厂不在接收新的订单,工厂要倒闭了
     *
     *  1 二进制 00000000000000000000000000000001 左移动 29位 前三位 001
     */
    private static final int STOP       =  1 << COUNT_BITS;

    /**
     * 线程池TIDYING状态,当前没有待执行的任务，等待执行注册到JVM的钩子函数terminated()
     *  工厂走倒闭程序，需要做最后清理工作
     *
     *  2 二进制 00000000000000000000000000000010 左移动 29位 前三位 010
     */
    private static final int TIDYING    =  2 << COUNT_BITS;

    /**
     * 执行完VM的钩子函数terminated()
     *  工厂关闭
     *  3 二进制 00000000000000000000000000000011 左移动 29位 前三位 011
     */
    private static final int TERMINATED =  3 << COUNT_BITS;

    /** 计算获取当前线程池状态 **/
    private static int runStateOf(int c)     { return c & ~CAPACITY; }

    /** 计算获取当前运行work数量**/
    private static int workerCountOf(int c)  { return c & CAPACITY; }


    /**
     * 即根据线程池的状态和worker数量合并成整形 ctl
     */
    private static int ctlOf(int rs, int wc) { return rs | wc; }


    /** 判断当前线程池是否小于s,c表示当前线程池状态 **/
    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    /** 判断当前线程池是否大于等于s,c表示当前线程池状态 **/
    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    /** 判断当前线程池是否正在正常运行  RUNNING状态**/
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * 使用CAS增加线程池中work数量（后29位可以直接整数运算）
     * 成功返回true,失败返回false
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /**
     * 使用CAS减少线程池中work数量（后29位可以直接整数运算）
     * 成功返回true,失败返回false
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * 使用CAS减少线程池中work数量（后29位可以直接整数运算）,失败循环继续尝试直到成功
     */
    private void decrementWorkerCount() {
        do {} while (! compareAndDecrementWorkerCount(ctl.get()));
    }


    private final BlockingQueue<Runnable> workQueue;


    /**
     * 存放worker线程的集合
     */
    private final HashSet<Worker> workers = new HashSet<Worker>();


    /**
     * 控制ThreadPoolExecutor的全局可重入锁
     */
    private final ReentrantLock mainLock = new ReentrantLock();



    /**
     * 控制ThreadPoolExecutor的全局可重入锁
     */
    private final Condition termination = mainLock.newCondition();


    /**
     * 记录work数量（片段值）
     */
    private int largestPoolSize;


    /**
     * 完成任务数量
     */
    private long completedTaskCount;


    /**
     * work线程构造工厂
     */
    private volatile ThreadFactory threadFactory;


    /**
     * 线程池无法接收新任务时，拒绝执行任务处理器，可以自定义
     */
    private volatile RejectedExecutionHandler handler;


    /**
     * work线程（非核心线程）空闲的时间，大于此时间是被销毁
     */
    private volatile long keepAliveTime;


    /**
     * 是否允许回收核心work线程
     */
    private volatile boolean allowCoreThreadTimeOut;


    /**
     * 线程池中核心work线程的数量。
     */
    private volatile int corePoolSize;


    /**
     * 线程池中允许的最大work数量
     */
    private volatile int maximumPoolSize;


    private static final RejectedExecutionHandler defaultHandler =
            new AbortPolicy();


    private static final RuntimePermission shutdownPerm =
            new RuntimePermission("modifyThread");


    /**
     * work使用AQS同步锁,用来判断当前work能否接收新任务
     *
     * 同步状态0，表示空闲 可以接收新任务
     * 同步状态1，表示正在执行任务 无法接收新任务
     *
     * 获取同步状态将 同步状态设置为1 ，释放同步状态设置为0
     */
    private final class Worker
            extends AbstractQueuedSynchronizer
            implements Runnable
    {

        private static final long serialVersionUID = 6138294804551838833L;

        /** 工作线程*/
        final Thread thread;
        /** 初始化Worker，分配的第一个任务 */
        Runnable firstTask;
        /** 每个work执行的任务数量 */
        volatile long completedTasks;

        /**
         * 实例化Worker
         */
        Worker(Runnable firstTask) {
            setState(-1);
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        /** 工作线程执行，调用外部TheadPoolExecutor.runWorker方法  */
        public void run() {
            runWorker(this);
        }


        /**
         * 判断当前Work是否空闲
         */
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        /**
         * tryAcquire 为AQS 尝试获取独占同步状态模板方法实现。
         */
        protected boolean tryAcquire(int unused) {

            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        /**
         * tryRelease为AQS 尝试释放独占同步状态模板方法实现。
         */
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        /**
         * 获取独占同步状态
         */
        public void lock()        { acquire(1); }

        /**
         * 尝试获取同步状态
         */
        public boolean tryLock()  { return tryAcquire(1); }

        /**
         * 释放独占同步状态
         */
        public void unlock()      { release(1); }

        /**
         * 判断能够护球同步状态
         */
        public boolean isLocked() { return isHeldExclusively(); }

        /**
         * 中断work正在执行任务
         */
        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }



    /**
     * CAS+循环设置线程池状态为shutdown
     */
    private void advanceRunState(int targetState) {
        for (;;) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) ||
                    ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    /**
     * 尝试将线程池状态设置为Terminate
     */
    final void tryTerminate() {
        for (;;) {
            int c = ctl.get();
            /**
             * 判断线程池能否进入TERMINATED状态
             * 如果以下3中情况任一为true，return，不进行终止
             * 1、还在运行状态
             * 2、状态是TIDYING、或 TERMINATED，已经终止过了
             * 3、SHUTDOWN 且 workQueue不为空
             * 4
             */
            if (isRunning(c) ||
                    runStateAtLeast(c, TIDYING) ||
                    (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
                return;

            /** 线程池workQueue不为空 return，并中断workQueue其中一个work**/

            /**
             * 线程池为stop状态，且还存在work,中断唤醒一个正在等任务的空闲worker，
             * 再次调用getTask(),线程池状态发生改变，返回null,work工作线程退出循环
             */
            if (workerCountOf(c) != 0) { // Eligible to terminate
                interruptIdleWorkers(ONLY_ONE);
                return;
            }
            /** 获取主锁：mainLock **/
            final ReentrantLock mainLock = this.mainLock;
            /** 加锁 **/
            mainLock.lock();
            try {
                /** 将线程池状态设置为TIDYING **/
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        /** 释放子类实现 **/
                        terminated();
                    } finally {
                        /** 将线程池状态设置为TERMINATED **/
                        ctl.set(ctlOf(TERMINATED, 0));
                        /** 释放锁 **/
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                /** 释放锁 **/
                mainLock.unlock();
            }
            // else retry on failed CAS
        }
    }

    /*
     * Methods for controlling interrupts to worker threads.
     */

    /**
     * 检查调用者是否有权限shutdown线程池
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 找到断线程池中空闲的work，中断其工作线程
     * onlyOne=true 表示仅仅中断一个空闲的work
     * onlyOne=false 表示中断所有空闲的work
     */
    private void interruptIdleWorkers(boolean onlyOne) {
        /** 获取主锁：mainLock **/
        final ReentrantLock mainLock = this.mainLock;
        /** 获取锁 **/
        mainLock.lock();
        try {
            /** 遍历所有work **/
            for (Worker w : workers) {
                Thread t = w.thread;
                /** 判断work工作线程是否没有被中断，且能获取独占同步状态（空闲） **/
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        /**  中断work工作线程 **/
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne)
                    break;
            }
        } finally {
            /** 释放锁 **/
            mainLock.unlock();
        }
    }

    /**
     * 找到断线程池中空闲的work，中断其工作线程
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;



    /**
     * 调用handler拒绝策略
     */
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    /**
     * 调用shutdown时运行状态转换后执行最后一步的清理模板方法
     */
    void onShutdown() {
    }

    /**
     * 判断线程池状态是RUNNING或SHUTDOWN,默认仅状态为RUNNING返回true
     * @param shutdownOK ==tue 时 状态为SHUTDOWN也返回true
     */
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * 将workQueue中的元素放入一个List并返回
     */
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        /** 将队列中的值全部从队列中移除，并赋值给对应集合 **/
        q.drainTo(taskList);
        /** 并发在判断 workQueue是否为空，将新添加加入到taskList**/
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r))
                    taskList.add(r);
            }
        }
        return taskList;
    }

    /**
     * 创建一个work执行任务
     * @param firstTask 任务(可以分配一个null,仅仅创建一个work)
     * @param core      是否创建的是一个core work
     * @return
     */
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        /** 无需循环校验，成功推出  **/
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            /**
             * 校验能否创建一个work
             * rs >= SHUTDOWN ，表示当前线程处于SHUTDOWN ，STOP、TIDYING、TERMINATED状态都不接收新任务返回false
             *
             * 只有当前线程状态为SHUTDOWN，创建一个线程去处理工作队列中任务可以通过校验
             * rs == SHUTDOWN && firstTask == null && ！workQueue.isEmpty()
             * **/
            if (rs >= SHUTDOWN &&
                    ! (rs == SHUTDOWN &&
                            firstTask == null &&
                            ! workQueue.isEmpty()))
                return false;

            /** 使用CASwork数量+1 **/
            for (;;) {
                /** 获取当前work数量 **/
                int wc = workerCountOf(c);

                /** 核心work数量大于corePoolSize，总work大于maximumPoolSize直接返回 **/
                if (wc >= CAPACITY ||
                        wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;

                /** worker + 1,成功跳出retry循环 **/
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                c = ctl.get();
                /** 如果状态不等于之前获取的state，跳出内层循环，继续去外层循环判断 **/
                if (runStateOf(c) != rs)
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        /** 创建work并执行任务 **/
        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            /** 实例化：Worker，并分配任务firstTask **/
            w = new Worker(firstTask);
            final Thread t = w.thread;
            /** work中工作线程不为null **/
            if (t != null) {
                /** 获取主锁：mainLock **/
                final ReentrantLock mainLock = this.mainLock;
                /** 加锁  **/
                mainLock.lock();
                try {
                    /** 获取当前线程池状态 **/
                    int rs = runStateOf(ctl.get());

                    /** 当前线程池状态为运行，或当前线程池状态为SHUTDOWN，提交是null任务
                     *  将创建的work添加到workers集合中
                     * **/
                    if (rs < SHUTDOWN ||
                            (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();
                        workers.add(w);
                        int s = workers.size();
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    /** 释放锁  **/
                    mainLock.unlock();
                }
                /** 创建成功，启动work执行任务 **/
                if (workerAdded) {
                    /** 启动work **/
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (! workerStarted)
                /** 失败创建work只能当前线程池状态不是运行状态 **/
                addWorkerFailed(w);
        }
        return workerStarted;
    }

    /**
     * 失败创建work只能当前线程池状态不是运行状态
     */
    private void addWorkerFailed(Worker w) {
        /** 获取主锁：mainLock **/
        final ReentrantLock mainLock = this.mainLock;
        /** 加锁 **/
        mainLock.lock();
        try {
            /** 尝试从workers删除，感觉没啥用， **/
            if (w != null)
                workers.remove(w);
            /** 将work数量-1 **/
            decrementWorkerCount();
            /** 尝试将线程池状态设置为Terminate **/
            tryTerminate();
        } finally {
            /** 释放 **/
            mainLock.unlock();
        }
    }

    /**
     * 执行work销毁退出操作
     * work 要结束的worker
     * completedAbruptly 表示是否需要对work数量-1操作
     *  runWorker 正常执行时 completedAbruptly 为false
     *  runWorker 执行出现异常 completedAbruptly 为true
     */
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
            decrementWorkerCount();

        /** 从workers 集合中移除worker **/
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        /** 尝试将线程池状态设置为Terminate **/
        tryTerminate();

        int c = ctl.get();
        /**  **/
        if (runStateLessThan(c, STOP)) {
            /** 如果 work正常退出，需要判断当前线程数量 < 要维护的线程数量 如果是addWorker()添加一个非核心work **/
            if (!completedAbruptly) {
                /**
                 * 如果允许回收核心线程，且workQueue还存在需要处理任务 work线程需要大于1
                 * 如果不允许回收核心线程，则work线程需要大于corePoolSize
                 * **/
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && ! workQueue.isEmpty())
                    min = 1;
                if (workerCountOf(c) >= min)
                    return; // replacement not needed
            }
            /** 如果 work 是异常退出  addWorker() 添加一个非核心work**/
            addWorker(null, false);
        }
    }

    /**
     * 从WorkQueue获取任务
     * 同时用来判断work何时退出销毁
     */
    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out?

        /** 无限循环，
         *  当work超过指定时间没有获取时，设置timedOut = true进行二次遍历时销毁当前work **/
        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            /** 线程池中状态 >= STOP 或者 线程池状态 == SHUTDOWN且阻塞队列为空，则停止worker - 1，return null **/
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            /** 获取work数量 **/
            int wc = workerCountOf(c);

            /**  判断是否需要开启work淘汰机制 **/
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;


            /**
             * 以下几种情况直接销毁当前work
             *
             * 超时没有获取任务timedOut=tue,for循环遍历第二次时
             * 当前任务超过maximumPoolSize
             * **/
            if ((wc > maximumPoolSize || (timed && timedOut))
                    && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c))
                    return null;
                continue;
            }

            try {
                /**
                 * 如果开启work淘汰机制超时获取任务，调用poll阻塞获取任务，存在超时，如果超时没有获取到任务
                 * 设置timedOut = true 进入第二次循环销毁
                 *
                 * 如果没开启work淘汰机制超时获取任务，调用take阻塞获取任务
                 * 【这里的阻塞都能被中断响应！！】
                 **/
                Runnable r = timed ?
                        workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                        workQueue.take();
                if (r != null)
                    return r;
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }


    /**
     * work执行逻辑。
     * 内部存在一个for循环，不断循环获取任务执行。当线程池状态还在运行，work线程会一直运行不会推出循环
     * getTask()线程返回null时退出，一般可能当前work超时被销毁或线程池不在运行。
     * @param w
     */
    final void runWorker(Worker w) {
        /** 获取当前线程 **/
        Thread wt = Thread.currentThread();
        /** 获取执行任务**/
        Runnable task = w.firstTask;
        /** 将任务从work清理**/
        w.firstTask = null;
        /** 初始化同步状态为0(创建时为-1) **/
        w.unlock(); // allow interrupts
        boolean completedAbruptly = true;
        try {
            /**
             * 如果当前work中存在任务则执行，不存在则从WorkQueue获取任务
             * getTask()！=null 时work永远不停止
             *  **/
            while (task != null || (task = getTask()) != null) {
                /** 获取work独占同步状态 **/
                w.lock();

                /** 如果当前线程池的状态为STOP，将work中工作线程标记为中断
                 * 1、如果线程池状态>=stop，且当前线程没有设置中断状态，wt.interrupt()
                   2、如果一开始判断线程池状态<stop，但Thread.interrupted()为true，即线程已经被中断，又清除了中断标示，再次判断线程池状态是否>=stop
                      是，再次设置中断标示，wt.interrupt()
                 *    否，不做操作，清除中断标示后进行后续步骤
                 *
                 * **/
                if ((runStateAtLeast(ctl.get(), STOP) ||
                        (Thread.interrupted() &&
                                runStateAtLeast(ctl.get(), STOP))) &&
                        !wt.isInterrupted())
                    wt.interrupt();


                try {
                    /** 模板方法给子类扩展 **/
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        /** 处理任务 **/
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        /** 模板方法给子类扩展 **/
                        afterExecute(task, thrown);
                    }
                } finally {
                    /** 重置任务 **/
                    task = null;
                    /** work执行的任务数量  **/
                    w.completedTasks++;
                    /** 释放work独占同步状态 **/
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }


    /**
     * 创建一个线程池,使用默认线程池的拒绝策略和创建work工厂
     * @param corePoolSize 线程池中核心work线程的数量。
     * @param maximumPoolSize 线程池中允许的最大work数量
     * @param keepAliveTime work线程（非核心线程）空闲的时间，大于此时间是被销毁
     * @param unit keepAliveTime的单位。TimeUnit
     * @param workQueue 用来保存等待执行的任务的阻塞队列
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), defaultHandler);
    }

    /**
     * 创建一个线程池,使用默认线程池的拒绝策略
     * @param corePoolSize 线程池中核心work线程的数量。
     * @param maximumPoolSize 线程池中允许的最大work数量
     * @param keepAliveTime work线程（非核心线程）空闲的时间，大于此时间是被销毁
     * @param unit keepAliveTime的单位。TimeUnit
     * @param workQueue 用来保存等待执行的任务的阻塞队列
     * @param threadFactory 创建work工厂
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                threadFactory, defaultHandler);
    }

    /**
     * 创建一个线程池,使用默认的创建work工厂
     * @param corePoolSize 线程池中核心work线程的数量。
     * @param maximumPoolSize 线程池中允许的最大work数量
     * @param keepAliveTime work线程（非核心线程）空闲的时间，大于此时间是被销毁
     * @param unit keepAliveTime的单位。TimeUnit
     * @param workQueue 用来保存等待执行的任务的阻塞队列
     * @param handler 线程池的拒绝策略
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), handler);
    }

    /**
     * 创建一个线程池，
     * @param corePoolSize 线程池中核心work线程的数量。
     * @param maximumPoolSize 线程池中允许的最大work数量
     * @param keepAliveTime work线程（非核心线程）空闲的时间，大于此时间是被销毁
     * @param unit keepAliveTime的单位。TimeUnit
     * @param workQueue 用来保存等待执行的任务的阻塞队列
     * @param threadFactory 创建work工厂
     * @param handler 线程池的拒绝策略
     */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
                maximumPoolSize <= 0 ||
                maximumPoolSize < corePoolSize ||
                keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }


    /**
     * 执行任务
     */
    public void execute(Runnable command) {
        /** 提交任务为null 抛出异常。 **/
        if (command == null)
            throw new NullPointerException();

        /** 获取ctl **/
        int c = ctl.get();

        /** work线程数量少于corePoolSize **/
        if (workerCountOf(c) < corePoolSize) {
            /** 创建新work线程并设置为核心线程执行任务 addWorker(command, true)  **/
            if (addWorker(command, true))
                return;
            c = ctl.get();
        }
        /** 进入此逻辑表示work线程数量大于corePoolSize或者前一步执行失败 **/

        /** 判断线程池是Running运行状态，将任务添加到workQueue尾部成功（队列满了返回false） **/
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            /** Double Check下当前线程状态是不是Running运行状态，不是就删除刚刚添加的任务，执行拒绝任务 **/
            if (! isRunning(recheck) && remove(command))
                reject(command);
            /** 异常情况 前面workerCountOf(c) < corePoolSize说明当时还存在大量work，说明线程池突然停止，为保证任务都能处理，
             * 创建一个临时work去处理当前workQueue中的任务  **/
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }

        /** 队列满了，创建一个非核心work执行新添加任务 **/
        else if (!addWorker(command, false))
             /** 执行失败，执行拒绝任务 **/
            reject(command);
    }

    /**
     * 温柔的终止线程池
     */
    public void shutdown() {
        /** 获取主锁：mainLock **/
        final ReentrantLock mainLock = this.mainLock;
        /** 加锁 **/
        mainLock.lock();
        try {
            /** 判断调用者是否有权限shutdown线程池 **/
            checkShutdownAccess();
            /** CAS+循环设置线程池状态为shutdown  **/
            advanceRunState(SHUTDOWN);
            /** 找到断线程池中空闲的work，中断其工作线程   **/
            interruptIdleWorkers();
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            /** 释放锁 **/
            mainLock.unlock();
        }
        /** 尝试将线程池状态设置为Terminate **/
        tryTerminate();
    }

    /**
     * 强硬的终止线程池
     * 返回在队列中没有执行的任务
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        /** 获取主锁：mainLock **/
        final ReentrantLock mainLock = this.mainLock;
        /** 加锁 **/
        mainLock.lock();
        try {
            /** 判断调用者是否有权限shutdown线程池 **/
            checkShutdownAccess();
            /** CAS+循环设置线程池状态为shutdown  **/
            advanceRunState(STOP);
            /** 找到断线程池中空闲的work，中断其工作线程   **/
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            /** 释放锁 **/
            mainLock.unlock();
        }
        /** 尝试将线程池状态设置为Terminate **/
        tryTerminate();
        return tasks;
    }

    /**
     * 判断当前线程池状态是非运行状态
     */
    public boolean isShutdown() {
        return ! isRunning(ctl.get());
    }

    /**
     * 判断线程池正在停止到TERMINATED状态过程中
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return ! isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    /**
     * 返回线程池 状态是否为TERMINATED
     */
    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    /**
     * 等待线程终止
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        /** 获取等待时间 **/
        long nanos = unit.toNanos(timeout);
        /** 获取主锁：mainLock **/
        final ReentrantLock mainLock = this.mainLock;
        /** 加锁 **/
        mainLock.lock();
        try {
            for (;;) {
                /**  当线程状态为终止时返回 **/
                if (runStateAtLeast(ctl.get(), TERMINATED))
                    return true;
                /** 超时返回 **/
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            /** 释放锁 **/
            mainLock.unlock();
        }
    }



    protected void finalize() {
        shutdown();
    }

    /**
     * 设置 threadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    /**
     * 获取 threadFactory
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * 设置 handler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    /**
     * 获取 handler
     */
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * 设置corePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0)
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();
        else if (delta > 0) {
            // We don't really know how many new threads are "needed".
            // As a heuristic, prestart enough new workers (up to new
            // core size) to handle the current number of tasks in
            // queue, but stop if queue becomes empty while doing so.
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }

    /**
     * 返回corePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * 如果线程池中work线程数量小于corePoolSize，添加一个核心work
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize &&
                addWorker(null, true);
    }

    /**
     * 添加一个work
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    /**
     * 初始化添加核心work到corePoolSize
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    /**
     * 返回 allowCoreThreadTimeOut
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * 设置 allowCoreThreadTimeOut 设置为true
     * 找到断线程池中空闲的work，中断其工作线程
     * @since 1.6
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();
        }
    }

    /**
     * 设置maximumPoolSize，如果设置maximumPoolSize大于原始值
     * 找到断线程池中空闲的work，中断其工作线程
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize)
            interruptIdleWorkers();
    }

    /**
     * 返回maximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * 设置keepAliveTime，如果keepAliveTime小于原始值
     * 找到断线程池中空闲的work，中断其工作线程
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0)
            interruptIdleWorkers();
    }

    /**
     * 返回 keepAliveTime
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /* User-level queue utilities */

    /**
     * 返回workQueue
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * 从workQueue 删除task
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }

    /**
     * 遍历线程池所有work，将工作线程状态为取消的删除
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {

            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    q.remove(r);
        }

        tryTerminate();
    }

    /* Statistics */

    /**
     * 获取work数量
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return runStateAtLeast(ctl.get(), TIDYING) ? 0
                    : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 获取正在执行任务work
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 获取work数量
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 获取待完成任务
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked())
                    ++n;
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 获取线程池完成任务总理
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 线程池用字符串及表示
     */
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked())
                    ++nactive;
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" :
                (runStateAtLeast(c, TERMINATED) ? "Terminated" :
                        "Shutting down"));
        return super.toString() +
                "[" + rs +
                ", pool size = " + nworkers +
                ", active threads = " + nactive +
                ", queued tasks = " + workQueue.size() +
                ", completed tasks = " + ncompleted +
                "]";
    }

    /* Extension hooks */

    /**
     * 模板方法给子类实现，执行任务前的操作
     */
    protected void beforeExecute(Thread t, Runnable r) { }

    /**
     * 模板方法给子类实现，执行任务后的操作
     */
    protected void afterExecute(Runnable r, Throwable t) { }

    /**
     * 模板方法给子类实现，线程池状态从TIDYING到TERMINATED需要做的清理动作
     */
    protected void terminated() { }

    /* Predefined RejectedExecutionHandlers */

    /**
     * A handler for rejected tasks that runs the rejected task
     * directly in the calling thread of the {@code execute} method,
     * unless the executor has been shut down, in which case the task
     * is discarded.
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code CallerRunsPolicy}.
         */
        public CallerRunsPolicy() { }

        /**
         * Executes task r in the caller's thread, unless the executor
         * has been shut down, in which case the task is discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * A handler for rejected tasks that throws a
     * {@code RejectedExecutionException}.
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() { }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                    " rejected from " +
                    e.toString());
        }
    }

    /**
     * A handler for rejected tasks that silently discards the
     * rejected task.
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardPolicy}.
         */
        public DiscardPolicy() { }

        /**
         * Does nothing, which has the effect of discarding task r.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor e) {
        }
    }

    /**
     * A handler for rejected tasks that discards the oldest unhandled
     * request and then retries {@code execute}, unless the executor
     * is shut down, in which case the task is discarded.
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardOldestPolicy} for the given executor.
         */
        public DiscardOldestPolicy() { }

        /**
         * Obtains and ignores the next task that the executor
         * would otherwise execute, if one is immediately available,
         * and then retries execution of task r, unless the executor
         * is shut down, in which case task r is instead discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}
