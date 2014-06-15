package ru.alepar.httppanda.httpclient.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import ru.alepar.httppanda.httpclient.DownloadWorker;

public class NettyDownloadWorker implements DownloadWorker {

    private final Channel ch;
    private final DownloadHandler handler;

    public NettyDownloadWorker(Channel ch, DownloadHandler handler) {
        this.ch = ch;
        this.handler = handler;
    }

    @Override
    public ChannelFuture closeFuture() {
        return ch.closeFuture();
    }
}
