package com.momid.screening

import com.momid.padding.ipText
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class ScreenClientHandler: SimpleChannelInboundHandler<ByteBuf>() {

    override fun channelRead0(ctx: ChannelHandlerContext, packet: ByteBuf) {
        val received = ByteArray(packet.readableBytes())
        packet.readBytes(received)

        val ips = ArrayList<ByteArray>()
        for (index in received.indices step 4) {
            ips.add(received.sliceArray(index until index + 4))
        }

        println(ips.joinToString("\n") {
            ipText(it)
        })
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}
