package com.su.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpFileServer {
    public static void main(String[] args) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup master = new NioEventLoopGroup();
        NioEventLoopGroup slaver = new NioEventLoopGroup();
        serverBootstrap
                .group(master, slaver)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer() {
                    protected void initChannel(Channel ch) throws Exception {
                        //核心的时间处理器链
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(1024 * 1024 * 10));
                        pipeline.addLast(new HttpFileHandler());
                    }
                });
        try {
            ChannelFuture channelFuture = serverBootstrap.bind(10000).sync();
            System.out.println("端口绑定成功");
        } catch (InterruptedException e) {
            e.printStackTrace();
            master.shutdownGracefully();
            slaver.shutdownGracefully();
        }
    }
}
