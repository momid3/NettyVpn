package com.momid.connection

import com.momid.vpn.handleIncomingUserPacket
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.CharsetUtil

class ServerHandler : SimpleChannelInboundHandler<ByteBuf>() {

    override fun channelRead0(ctx: ChannelHandlerContext, packet: ByteBuf) {
        val received = ByteArray(packet.readableBytes())
        packet.readBytes(received)
//        println("received from client: ${received.size}")
        handleIncomingUserPacket(received)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
