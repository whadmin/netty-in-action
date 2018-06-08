package com.phei.netty.frame.attributeMap;

import static  com.phei.netty.frame.attributeMap.AttributeMapConstant.NETTY_CHANNEL_KEY;  
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;    
import io.netty.util.Attribute;  
  
/**
 * ctx.channel().attr 是所有ChannelHandlerAdapter共享的
 * ctx.attr 是每个ChannelHandlerAdapter 独自的
 */
import java.util.Date;  
public class HelloWorldClientHandler extends ChannelHandlerAdapter {  
  
  
    @Override  
    public void channelActive(ChannelHandlerContext ctx) {  
        Attribute<NettyChannel> attr = ctx.attr(NETTY_CHANNEL_KEY);  
      //ctx.channel().attr(NETTY_CHANNEL_KEY);
        NettyChannel nChannel = attr.get();  
        if (nChannel == null) {  
            NettyChannel newNChannel = new NettyChannel("HelloWorld0Client", new Date());  
            nChannel = attr.setIfAbsent(newNChannel);  
        } else {  
            System.out.println("channelActive attributeMap 中是有值的");  
            System.out.println(nChannel.getName() + "=======" + nChannel.getCreateDate());  
        }  
        System.out.println("HelloWorldC0ientHandler Active");  
        ctx.fireChannelActive();  
    }  
  
    @Override  
    public void channelRead(ChannelHandlerContext ctx, Object msg) {  
        Attribute<NettyChannel> attr = ctx.attr(NETTY_CHANNEL_KEY); 
      //ctx.channel().attr(NETTY_CHANNEL_KEY);
        NettyChannel nChannel = attr.get();  
        if (nChannel == null) {  
            NettyChannel newNChannel = new NettyChannel("HelloWorld0Client", new Date());  
            nChannel = attr.setIfAbsent(newNChannel);  
        } else {  
            System.out.println("channelRead attributeMap 中是有值的");  
            System.out.println(nChannel.getName() + "=======" + nChannel.getCreateDate());  
        }  
        System.out.println("HelloWorldClientHandler read Message:" + msg);  
          
        ctx.fireChannelRead(msg);  
    }  
  
    @Override  
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {  
        cause.printStackTrace();  
        ctx.close();  
    }  
  
}  
