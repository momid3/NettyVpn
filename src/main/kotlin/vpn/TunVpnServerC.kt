package com.momid.vpn

import com.momid.getTunInterface
import com.momid.libc
import com.momid.readFromTun
import com.momid.writeToTun
import org.pcap4j.core.*
import org.pcap4j.packet.*
import org.pcap4j.util.MacAddress
import java.io.IOException
import java.net.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import kotlin.system.exitProcess

var sourceMacAddress: MacAddress? = null
private var destinationMacAddress: MacAddress? = null
private var clientSourceIp = InetAddress.getByName("192.168.3.8")
var clientIpAddress: Inet4Address? = null
private val executor: ExecutorService = Executors.newSingleThreadExecutor()
var tunFd = 0
private var handle: PcapHandle? = null
const val SERVER_IP = "194.146.123.180"
val SERVER_IP_ADDRESS = Inet4Address.getByName("194.146.123.180") as Inet4Address
val GATEWAY_IP_ADDRESS = "146.19.130.254"
val usedClientSourcePorts = BooleanArray(65535) {
    false
}

val incomingInternetPackets = ArrayBlockingQueue<ByteArray>(300)

fun startVpn() {
    val tunName = "tun0"

    // Open and configure the TUN interface
    tunFd = getTunInterface(tunName)
    println("TUN interface $tunName opened with file descriptor $tunFd")

    // Ensure the TUN interface is closed when the program exits
    Runtime.getRuntime().addShutdownHook(Thread {
        libc.close(tunFd)
        println("TUN interface closed")
    })

    // Start reading packets from the TUN interface
    println("Reading packets from TUN interface...")
    while (true) {
        val packet = readFromTun(tunFd)
        if (packet != null) {
            println("Read packet of size ${packet.size}")
            // Process the packet as needed
            executor.execute {
//                    handleIncomingInternetPacket(packet)
                incomingInternetPackets.put(packet)
            }
        }
    }
}

fun handleIncomingUserPacket(packet: ByteArray) {
    writeToTun(tunFd, packet)
}

fun main() {
    startVpn()
}
