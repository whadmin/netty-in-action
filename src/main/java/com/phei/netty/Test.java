package com.phei.netty;

import java.util.concurrent.atomic.AtomicInteger;

public class Test {

    private static final int           COUNT_BITS = Integer.SIZE - 3;
    // runState is stored in the high-order bits
    private static final int           RUNNING    = -1 << COUNT_BITS;
    private static final int           SHUTDOWN   = 0 << COUNT_BITS;
    private static final int           STOP       = 1 << COUNT_BITS;
    private static final int           TIDYING    = 2 << COUNT_BITS;
    private static final int           TERMINATED = 3 << COUNT_BITS;

    private final static AtomicInteger ctl        = new AtomicInteger(ctlOf(RUNNING, 0));

    private static final int           CAPACITY   = (1 << COUNT_BITS) - 1;
    
    /**
     * 第0位，任意终端
     */
    public final static int any_terminal = 0;
    
    /**
     * 第1位，pc
     */
    public final static int pc = 1 << 0;
    
    /**
     * 第2位，wap
     */
    public final static int wap = 1 << 1;
    
    /**
     * 第3位，主客
     */
    public final static int nativeApp = 1 << 2;

    // Packing and unpacking ctl
    private static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    private static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    static final int SHARED_SHIFT   = 16;
    static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
    static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
    static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

    public static void main(String[] args) {
        
        System.out.println("RUNNING   :" + toFullBinaryString(pc));
        System.out.println("RUNNING   :" + toFullBinaryString(wap));

//        System.out.println("RUNNING   :" + RUNNING);
//        System.out.println("SHUTDOWN  :" + SHUTDOWN);
//        System.out.println("STOP      :" + STOP);
//        System.out.println("TIDYING   :" + TIDYING);
//        System.out.println("TERMINATED:" + TERMINATED);
//        System.out.println("RUNNING   :" + toFullBinaryString(RUNNING)); // 输出完整的二进制序列
//        System.out.println("SHUTDOWN  :" + toFullBinaryString(SHUTDOWN)); // 输出完整的二进制序列
//        System.out.println("STOP      :" + toFullBinaryString(STOP)); // 输出完整的二进制序列
//        System.out.println("TIDYING   :" + toFullBinaryString(TIDYING)); // 输出完整的二进制序列
//        System.out.println("TERMINATED:" + toFullBinaryString(TERMINATED));
//        System.out.println("CAPACITY  :" + toFullBinaryString(CAPACITY));
//        System.out.println("~CAPACITY :" + toFullBinaryString(~CAPACITY));
//        System.out.println("ctl :" + toFullBinaryString(ctl.intValue()));
//        System.out.println(workerCountOf(1));
//        System.out.println(workerCountOf(2));
//        System.out.println(workerCountOf(3));
//
//        System.out.println(runStateOf(1));
//        System.out.println(runStateOf(2));
//        System.out.println(runStateOf(3));

        // System.out.println("SHARED_UNIT   :" + toFullBinaryString(SHARED_UNIT));
        // System.out.println("MAX_COUNT     :" + toFullBinaryString(MAX_COUNT));
        // System.out.println("EXCLUSIVE_MASK:" + toFullBinaryString(EXCLUSIVE_MASK));
        //
        // System.out.println("-1   :"+toFullBinaryString(2147483647));
        // System.out.println("6    :"+toFullBinaryString(-2147483647));
        //
        // System.out.println(5<<1);
        // System.out.println(5<<2);
        // System.out.println(5<<3);

        // testRequest();
    }

    public static String toFullBinaryString(int num) {
        char[] chs = new char[Integer.SIZE];
        for (int i = 0; i < Integer.SIZE; i++) {
            chs[Integer.SIZE - 1 - i] = (char) (((num >> i) & 1) + '0');
        }
        return new String(chs);
    }

    public static void testRequest() {
        retry: // 1<span style="font-family: Arial, Helvetica, sans-serif;">（行2）</span>
        for (int i = 0; i < 10; i++) {
            // retry:// 2（行4）
            while (i == 5) {
                continue retry;
            }
            System.out.print(i + " ");
        }
    }

}
