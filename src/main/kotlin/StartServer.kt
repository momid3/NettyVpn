package com.momid

import com.momid.connection.channel
import com.momid.vpn.incomingInternetPackets
import com.momid.vpn.startVpn
import io.netty.buffer.Unpooled

fun startServer() {
    Thread {
        startVpn()
    }.start()
    Thread {
        while (true) {
            val packet = incomingInternetPackets.take()
            val data = Unpooled.wrappedBuffer(packet)
            channel!!.writeAndFlush(data)
        }
    }.start()
    com.momid.connection.startServer()
}
