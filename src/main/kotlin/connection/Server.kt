package com.momid.connection

import com.momid.vpn.clientIpAddress
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import java.io.File
import java.net.Inet4Address

val certDirectory = File("/home/momiduser")

var channel: SocketChannel? = null

fun startServer() {
    val bossGroup: EventLoopGroup = NioEventLoopGroup(1)
    val workerGroup: EventLoopGroup = NioEventLoopGroup()
    try {
        // Load the SSL context
        val sslContext: SslContext = SslContextBuilder.forServer(
            certDirectory.resolve("certificate.crt"), certDirectory.resolve("private.key")).build()

        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 100)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(object : ChannelInitializer<SocketChannel>() {

                override fun initChannel(ch: SocketChannel) {
                    channel = ch
                    clientIpAddress = ch.remoteAddress().address as Inet4Address
                    val pipeline = ch.pipeline()
                    // Add SSL handler first to encrypt and decrypt everything
                    pipeline.addLast(sslContext.newHandler(ch.alloc()))
                    // Add frame decoder and prepender
                    pipeline.addLast(LengthFieldBasedFrameDecoder(3800, 0, 4, 0, 4))
                    pipeline.addLast(LengthFieldPrepender(4))
                    // Add the main handler
                    pipeline.addLast(ServerHandler())
                }
            })

        // Bind to port 8443 (common for SSL/TLS)
        val channelFuture = bootstrap.bind(443).sync()
        println("server started on port 8443 with SSL/TLS")

        // Wait until the server socket is closed
        channelFuture.channel().closeFuture().sync()
    } finally {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }
}
