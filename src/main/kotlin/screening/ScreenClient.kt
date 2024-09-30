package com.momid.screening

import com.momid.padding.toByteBuff
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender

fun startScreeningClient() {
    val group: EventLoopGroup = NioEventLoopGroup()

    val bootstrap = Bootstrap()

    bootstrap.group(group)
        .channel(NioSocketChannel::class.java)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(object : ChannelInitializer<SocketChannel>() {
            @Throws(Exception::class)
            override fun initChannel(ch: SocketChannel) {
                val pipeline = ch.pipeline()
                pipeline.addLast(LengthFieldBasedFrameDecoder(3800, 0, 4, 0, 4))
                pipeline.addLast(LengthFieldPrepender(4))
                pipeline.addLast(ScreenClientHandler())
            }
        })

    val channelFuture: ChannelFuture
    try {
        channelFuture = bootstrap.connect("194.146.123.180", 443).sync()
        channelFuture.channel().writeAndFlush(ByteArray(4).toByteBuff())
        channelFuture.channel().closeFuture().sync()
    } catch (t: Throwable) {
        t.printStackTrace()
    } finally {
        group.shutdownGracefully()
    }
}
