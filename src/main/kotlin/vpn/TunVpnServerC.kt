package com.momid.vpn

import com.momid.getTunInterface
import com.momid.libc
import com.momid.readFromTun
import com.momid.writeToTun
import io.netty.channel.Channel
import org.pcap4j.core.PcapHandle
import org.pcap4j.util.MacAddress
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

val availableIps = generateIPs()

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

fun generateIPs(): HashMap<Ip, Info?> {
    val startIp = byteArrayOf(10, 0, 0, 3)
    val ipList = HashMap<Ip, Info?>()

    for (ip in 0 until 33) {
        val newIp = startIp.clone()
        newIp[3] = (startIp[3] + ip).toByte()  // Increment the last byte
        ipList[Ip(newIp)] = null
    }
    return ipList
}

class Info(val channel: Channel)

class Ip(val byteArray: ByteArray) {
    override fun equals(other: Any?): Boolean {
        return other is Ip && this.byteArray.contentEquals(other.byteArray)
    }

    override fun hashCode(): Int {
        return byteArray.contentHashCode()
    }
}

fun allocateIp(channel: Channel): ByteArray? {
    synchronized(availableIps) {
        val available = availableIps.entries.find { (ip, info) ->
            info == null
        } ?: return null
        val (ip, info) = available
        availableIps[ip] = Info(channel)
        return ip.byteArray
    }
}

fun removeIp(channel: Channel) {
    synchronized(availableIps) {
        val (ip, info) = availableIps.entries.find { (ip, info) ->
            info?.channel == channel
        } ?: return
        availableIps[ip] = null
    }
}

fun channelOfIp(byteArray: ByteArray): Channel? {
    return availableIps[Ip(byteArray)]?.channel
}

fun ByteArray.put(index: Int, byteArray: ByteArray) {
    System.arraycopy(byteArray, 0, this, index, byteArray.size)
}

/**
 * Extracts the destination IP address from an IP packet represented as a ByteArray.
 *
 * Supports both IPv4 and IPv6 packets. The function checks the IP version and extracts
 * the destination address based on the appropriate header structure.
 *
 * @param packetByteArray The IP packet as a ByteArray.
 * @return The destination IP address as a ByteArray.
 * @throws IllegalArgumentException if the packet is invalid or the IP version is unsupported.
 */
fun destinationIpAddress(packetByteArray: ByteArray): ByteArray? {
    if (packetByteArray.isEmpty()) {
        return null
    }

    val version = (packetByteArray[0].toInt() ushr 4) and 0x0F

    return when (version) {
        4 -> {
            if (packetByteArray.size < 20) {
                return null
            }
            packetByteArray.copyOfRange(16, 20)
        }
        6 -> {
            if (packetByteArray.size < 40) {
                return null
            }
            packetByteArray.copyOfRange(24, 40)
        }
        else -> {
            return null
        }
    }
}

fun main() {
    startVpn()
}
