package com.phei.netty.frame.lenField;

import java.nio.charset.Charset;  

import io.netty.buffer.ByteBuf;  
import io.netty.channel.ChannelHandlerContext;  
import io.netty.handler.codec.MessageToByteEncoder;  
  
public class CustomEncoder extends MessageToByteEncoder<CustomMsg> {  
  
    @Override  
    protected void encode(ChannelHandlerContext ctx, CustomMsg msg, ByteBuf out) throws Exception {  
        if(null == msg){  
            throw new Exception("msg is null");  
        }  
          
        String body = msg.getBody();  
        byte[] bodyBytes = body.getBytes(Charset.forName("utf-8"));   
        out.writeInt(bodyBytes.length);  
        out.writeBytes(bodyBytes);  
          
    }  
  
}  