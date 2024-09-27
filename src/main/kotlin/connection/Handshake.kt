package com.momid.connection

import com.momid.vpn.allocateIp
import com.momid.vpn.handleIncomingUserPacket
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.CharsetUtil

class HandshakeHandler : SimpleChannelInboundHandler<ByteBuf>() {

    override fun channelRead0(ctx: ChannelHandlerContext, packet: ByteBuf) {
        val received = ByteArray(packet.readableBytes())
        packet.readBytes(received)
        println("handshake from client: ${received.size}")
        val allocatedIp = allocateIp(ctx.channel()) ?: return
        println("allocated ip: " + allocatedIp.joinToString(".") {
            "" + (it.toInt() and 0xff)
        })
        val data = Unpooled.wrappedBuffer(allocatedIp)
        ctx.writeAndFlush(data)
        ctx.pipeline().remove(this)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
