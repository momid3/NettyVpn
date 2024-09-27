package com.momid

import com.momid.vpn.channelOfIp
import com.momid.vpn.destinationIpAddress
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
            val ip = destinationIpAddress(packet)
            if (ip == null) {
                println("destination ip is null")
                continue
            }
            val channelOfIp = channelOfIp(ip)
            if (channelOfIp == null) {
                println("channel of ip is null")
                continue
            }
            val data = Unpooled.wrappedBuffer(packet)
            channelOfIp.writeAndFlush(data).addListener { future ->
                if (future.isSuccess) {
                    println("Write successful")
                } else {
                    println("Write failed: ${future.cause()}")
                }
            }
        }
    }.start()
    com.momid.connection.startServer()
}
