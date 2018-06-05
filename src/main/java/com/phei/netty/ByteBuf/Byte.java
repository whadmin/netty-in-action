package com.phei.netty.ByteBuf;

import java.io.UnsupportedEncodingException;

/**
 * http://blog.csdn.net/qiantudou/article/details/49928423
 * http://blog.csdn.net/xiaochunyong/article/details/7748713
 * @author wuhao.w
 *
 */
public class Byte {
    
    public static void main(String[] args) throws UnsupportedEncodingException  {  
        // TODO Auto-generated method stub  
        int data = 1;  
        String binaryStr = java.lang.Integer.toBinaryString(data);  
        System.out.println("the result is : " + binaryStr);  
        byte results[] = binaryStr.getBytes("utf8");  
//        for(int i = 0;i < results.length ; i++){  
//            System.out.println("the " + i +  " result is : " + results[i]);//"1"的ascii码是49。  
//        }
        
        byte[] test=intToByteArray(new java.lang.Integer(data));
        System.out.println(Integer.toBinaryString((test[0] & 0xFF) + 0x100).substring(1));
        System.out.println(Integer.toBinaryString((test[1] & 0xFF) + 0x100).substring(1));
        System.out.println(Integer.toBinaryString((test[2] & 0xFF) + 0x100).substring(1));
        System.out.println(Integer.toBinaryString((test[3] & 0xFF) + 0x100).substring(1));
    }  
    
    
    
    /**
     * int到byte[]
     * 
     * @param i
     * @return
     */
    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        // 由高位到低位
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

    /**
     * byte[]转int
     * 
     * @param bytes
     * @return
     */
    public static int byteArrayToInt(byte[] bytes) {
        int value = 0;
        // 由高位到低位
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (bytes[i] & 0x000000FF) << shift;// 往高位游
        }
        return value;
    }

}
