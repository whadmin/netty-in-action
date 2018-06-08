package com.netty.learning;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import org.junit.Test;


public class ByteBufMain {
	
	@Test
	public void create(){
		//创建一个基于非对象池的
		System.out.println(UnpooledByteBufAllocator.DEFAULT.buffer());
		System.out.println(UnpooledByteBufAllocator.DEFAULT.heapBuffer());
		System.out.println(PooledByteBufAllocator.DEFAULT.buffer());
	}
	
	@Test
	public void put(){
		ByteBuf buf=UnpooledByteBufAllocator.DEFAULT.buffer();
		buf.writeByte(1);
		buf.writeByte(2);
		buf.writeByte(3);
		buf.writeByte(4);
		buf.writeByte(5);
		System.out.println("writerIndex:"+buf.writerIndex());
		System.out.println("readerIndex:"+buf.readerIndex());
		System.out.println("readableBytes:"+buf.readableBytes());
		System.out.println(buf.getByte(0));
		System.out.println("------------------------------");
		System.out.println("writerIndex:"+buf.writerIndex());
		System.out.println("readerIndex:"+buf.readerIndex());
		System.out.println("readableBytes:"+buf.readableBytes());
		System.out.println(buf.getByte(0));
		System.out.println("------------------------------");
		System.out.println("writerIndex:"+buf.writerIndex());
		System.out.println("readerIndex:"+buf.readerIndex());
		System.out.println("readableBytes:"+buf.readableBytes());
		System.out.println(buf.readByte());
		System.out.println("------------------------------");
		System.out.println("writerIndex:"+buf.writerIndex());
		System.out.println("readerIndex:"+buf.readerIndex());
		System.out.println("readableBytes:"+buf.readableBytes());

	}
	
	
	

}
