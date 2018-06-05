package com.phei.netty.frame.lenField;


import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;  
  
public class CustomClientHandler extends ChannelHandlerAdapter {  
      
    @Override  
    public void channelActive(ChannelHandlerContext ctx) throws Exception {  
        CustomMsg customMsg = new CustomMsg("Hello,Netty".length(), "Hello,Netty");  
        ctx.writeAndFlush(customMsg);  
    }
}  
