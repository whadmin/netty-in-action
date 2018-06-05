package com.phei.netty.frame.lenField;

public class CustomMsg {  
          
    //主题信息的长度  
    private int length;  
      
    //主题信息  
    private String body;  
      
    public CustomMsg() {  
          
    }  
      
    public CustomMsg(int length, String body) {    
        this.length = length;  
        this.body = body;  
    }  
  
    public int getLength() {  
        return length;  
    }  
  
    public void setLength(int length) {  
        this.length = length;  
    }  
  
    public String getBody() {  
        return body;  
    }  
  
    public void setBody(String body) {  
        this.body = body;  
    }  
  
}  
