package com.phei.netty.frame.lenField;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;  
import io.netty.channel.SimpleChannelInboundHandler;  
  
public class CustomServerHandler extends ChannelHandlerAdapter {  
  
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof String) {  
            String customMsg = (String)msg;  
            System.out.println("Client->Server:"+ctx.channel().remoteAddress()+" send "+customMsg);  
        }  
          
    }


  
}  
