package com.phei.netty.threadPool;

import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.AbstractQueuedSynchronizer.Node;

public class Node {

    static final Node SHARED    = new Node();
    /** Marker to indicate a node is waiting in exclusive mode */
    static final Node EXCLUSIVE = null;

    /** waitStatus value to indicate thread has cancelled */
    static final int  CANCELLED = 1;
    /** waitStatus value to indicate successor's thread needs unparking */
    static final int  SIGNAL    = -1;
    /** waitStatus value to indicate thread is waiting on condition */
    static final int  CONDITION = -2;
    /**
     * waitStatus value to indicate the next acquireShared should unconditionally propagate
     */
    static final int  PROPAGATE = -3;

    volatile int      waitStatus;

    volatile Node     prev;

    volatile Node     next;

    volatile Thread   thread;

    Node              nextWaiter;

    final boolean isShared() {
        return nextWaiter == SHARED;
    }

    final Node predecessor() throws NullPointerException {
        Node p = prev;
        if (p == null) throw new NullPointerException();
        else return p;
    }

    Node(){ // Used to establish initial head or SHARED marker
    }

    Node(Thread thread, Node mode){ // Used by addWaiter
        this.nextWaiter = mode;
        this.thread = thread;
    }

    Node(Thread thread, int waitStatus){ // Used by Condition
        this.waitStatus = waitStatus;
        this.thread = thread;
    }

    public static void main(String[] args) {
        Node pred = new Node(null, 1);
        Node node = new Node(null, 0);
        pred.next = node;
        node.prev = pred;

        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
        System.out.println(pred);
        System.out.println(node);
        System.out.println(11);

    }
    
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
    
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
    
    private boolean doAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (nanosTimeout <= 0L)
            return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;
                if (shouldParkAfterFailedAcquire(p, node) &&
                    nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

}
