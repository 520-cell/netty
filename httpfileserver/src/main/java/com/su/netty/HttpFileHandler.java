package com.su.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.*;
import java.net.URLDecoder;

public class HttpFileHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private String path = "E:\\";
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (! ("GET".equals(msg.method().name()))){
            setErr("只处理GET请求");
            return;
        }

        String uri = URLDecoder.decode(msg.uri(), "utf-8");
        path = path + uri;
        File file = new File(path);
        if (! file.exists()){
            setErr("请求路径不存在");
            return;
        }

        if (file.isFile()){
            myFileHandler(file, ctx);
        }else{
            directoryHandle(file, ctx,uri);
        }
    }

    private void myFileHandler(File file, ChannelHandlerContext ctx) {
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            long length = randomAccessFile.length();
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set("Content-Length", length);
            response.headers().set("Content-Type", "application/octet-stream");
            ctx.writeAndFlush(response);
            ChunkedNioFile chunkedNioFile = new ChunkedNioFile(file, 1024 * 1024);
            ChannelFuture channelFuture = ctx.writeAndFlush(chunkedNioFile);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    future.channel().close();
                    System.out.println("下载完毕，关闭连接");
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setErr(String err) {
        System.out.println(err);
    }

    private void directoryHandle(File file, ChannelHandlerContext ctx, String uri) {
        File[] files = file.listFiles();
        StringBuffer html = new StringBuffer();
        html.append("<html>");
        html.append("<ul>");
        for (int i = 0; i < files.length; i++) {
            html.append("<li>");
            String fileName = null;
            if (uri.equals("/")){
                fileName = " <a href = ' " + uri + files[i].getName() + "'>";
            }else {
                fileName = " <a href = ' " + uri + "/" + files[i].getName() + "'>";
            }
            html.append(files[i].isFile() ? "文件" : "文件夹");
            html.append(":" );
            html.append(fileName);
            html.append(files[i].getName());
            html.append("</li>");
        }
        html.append("</ul>");
        html.append("</html>");

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set("Content-Type", "text/html;charset=utf-8");
        try {
            response.content().writeBytes(html.toString().getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ctx.writeAndFlush(response);
        ctx.close();
    }
}
