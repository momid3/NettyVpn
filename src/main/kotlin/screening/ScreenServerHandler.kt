package com.momid.screening

import com.momid.padding.toByteBuff
import com.momid.vpn.availableIps
import com.momid.vpn.put
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class ScreenServerHandler: SimpleChannelInboundHandler<ByteBuf>() {

    override fun channelRead0(ctx: ChannelHandlerContext, packet: ByteBuf) {
        val received = ByteArray(packet.readableBytes())
        packet.readBytes(received)
//        println("received from client: ${received.size}")
        val ips = ByteArray(availableIps.size * 4)
        var index = 0

        availableIps.forEach { (ip, info) ->
            if (info != null) {
                ips.put(index, ip.byteArray)
                index += 4
            }
        }

        ctx.writeAndFlush(ips.toByteBuff())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
