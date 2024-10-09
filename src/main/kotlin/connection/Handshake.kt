package com.momid.connection

import com.momid.padding.toByteBuff
import com.momid.vpn.allocateIp
import com.momid.vpn.handleIncomingUserPacket
import com.momid.vpn.put
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.CharsetUtil

val clientHandshake = "hello handshake from\n\n\n server allocated ip".toByteArray(charset("ASCII"))

val handShakes = listOf(
    "hello handshake from\n\n\n server allocated ip from handshakes".toByteArray(charset("ASCII")),
    "hello handshake from\n\n\n server allocated ip of some handshakes".toByteArray(charset("ASCII")),
    "hello handshake from\n\n\n server allocated ip handshake is received".toByteArray(charset("ASCII"))
)

class HandshakeHandler : SimpleChannelInboundHandler<ByteBuf>() {

    override fun channelRead0(ctx: ChannelHandlerContext, packet: ByteBuf) {
        val received = ByteArray(packet.readableBytes())
        packet.readBytes(received)
        if (received.size == clientHandshake.size) {
            println("handshake from client: ${received.size}")
            val allocatedIp = allocateIp(ctx.channel()) ?: return
            println("allocated ip: " + allocatedIp.joinToString(".") {
                "" + (it.toInt() and 0xff)
            })
            val handshake = "hello from handshake\n\n\n allocated ip".toByteArray(charset("ASCII"))
            val handShakeToSend = ByteArray(handshake.size + allocatedIp.size)
            handShakeToSend.put(0, allocatedIp)
            handShakeToSend.put(allocatedIp.size, handshake)
            val data = Unpooled.wrappedBuffer(handShakeToSend)
            ctx.writeAndFlush(data)
            ctx.pipeline().remove(this)
        } else {
            val data = Unpooled.wrappedBuffer(handShakes.find {
                it.size == received.size
            } ?: return)
            ctx.writeAndFlush(data)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
