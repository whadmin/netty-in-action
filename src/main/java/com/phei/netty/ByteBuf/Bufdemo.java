package com.phei.netty.ByteBuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Bufdemo {

    public static void main(String[] args) {


        byte[] req = "00000000".getBytes();
        ByteBuf firstMessage = Unpooled.buffer(req.length);
        firstMessage.writeBytes(req);

        System.out.println(firstMessage.readerIndex());
        System.out.println(firstMessage.writerIndex());
        System.out.println(firstMessage.capacity());
        firstMessage.setInt(0, 1);
        int length = firstMessage.readableBytes();
        for (int i = 0; i < length; i++) {
            System.out.println(firstMessage.getByte(i));
        }
        // System.out.println(firstMessage.discardReadBytes());

    }
    


    

}
