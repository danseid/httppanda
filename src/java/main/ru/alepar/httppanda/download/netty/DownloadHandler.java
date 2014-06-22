package ru.alepar.httppanda.download.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.alepar.httppanda.buffer.BufferChannel;
import ru.alepar.httppanda.stat.BytePerSecStat;
import ru.alepar.httppanda.stat.IoStat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class DownloadHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger log = LoggerFactory.getLogger(DownloadHandler.class);

    private final BufferChannel bufferChannel;
    private final IoStat bytePerSecStat = new BytePerSecStat();
    private final CompletableFuture<HttpHeaders> headersFuture = new CompletableFuture<>();

    private volatile double bytePerSec;

    private long pos;

    public DownloadHandler(BufferChannel bufferChannel, long offset) {
        this.bufferChannel = bufferChannel;
        this.pos = offset;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpResponse) {
            final HttpResponse response = (HttpResponse) msg;
            headersFuture.complete(response.headers());
        } else if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;

            final ByteBuf byteBuf = content.content();
            bufferChannel.write(byteBuf.nioBuffer(), pos);

            final int length = byteBuf.readableBytes();
            pos += length;
            bytePerSecStat.add(length);
            bytePerSec = bytePerSecStat.get();

            if (content instanceof LastHttpContent) {
                ctx.close();
            }
        }
    }

    public double getBytePerSec() {
        return bytePerSec;
    }

    public Future<HttpHeaders> getHeadersFuture() {
        return headersFuture;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("uncaught exception in download channel", cause);
        ctx.close();
    }
}