/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.phei.netty.basic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.net.SocketAddress;
import java.util.logging.Logger;

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 */
public class TimeClientHandler1 extends ChannelHandlerAdapter {

    private static final Logger logger = Logger.getLogger(TimeClientHandler1.class.getName());

    private final ByteBuf       firstMessage;

    /**
     * Creates a client-side handler.
     */
    public TimeClientHandler1(){
        byte[] req = "QUERY TIME ORDER".getBytes();
        firstMessage = Unpooled.buffer(req.length);
        firstMessage.writeBytes(req);

    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                        ChannelPromise promise) throws Exception {
        System.out.println("client1 connect");
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("client1 channelActive");
        ctx.fireChannelActive();
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client1 channelInactive");
    }

    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client1 handlerAdded");
    }

    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client1 handlerRemoved");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("client1 channelRead:");
        ctx.fireChannelRead(msg);
    }
    
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client1 channelReadComplete");
        ctx.fireChannelReadComplete();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 释放资源
        System.out.println("client exceptionCaught");
        // logger.warning("Unexpected exception from downstream : " + cause.getMessage());
        ctx.close();
    }
}
