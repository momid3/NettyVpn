package com.momid.vpn

import org.pcap4j.core.*
import org.pcap4j.packet.*
import org.pcap4j.packet.namednumber.EtherType
import org.pcap4j.util.MacAddress
import java.io.IOException
import java.net.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException

var sourceMacAddress: MacAddress? = null
private var destinationMacAddress: MacAddress? = null
private var clientSourceIp = InetAddress.getByName("192.168.3.8")
var clientIpAddress: Inet4Address? = null
private val executor: ExecutorService = Executors.newSingleThreadExecutor()
private var handle: PcapHandle? = null
const val SERVER_IP = "146.19.130.158"
val SERVER_IP_ADDRESS = Inet4Address.getByName("146.19.130.158") as Inet4Address
val GATEWAY_IP_ADDRESS = "146.19.130.254"
val usedClientSourcePorts = BooleanArray(65535) {
    false
}

val incomingInternetPackets = ArrayBlockingQueue<ByteArray>(300)

fun startVpn() {
    try {
        val networkInterfaces: List<PcapNetworkInterface> = Pcaps.findAllDevs()
        for (index in networkInterfaces.indices) {
            val networkInterface = networkInterfaces[index]

            println("interface " + index + ": ")
            println(networkInterface.addresses)
            println(networkInterface.linkLayerAddresses)
            println(networkInterface.name + " " + networkInterface.description + " " + networkInterface.isUp)

            for (inetAddress in networkInterface.addresses) {
                if (inetAddress.address.hostAddress.equals("146.70.145.152")) {
                }
            }
        }

        println("please choose an interface")
        val interfaceIndex = readln().toInt()
        val networkInterface = networkInterfaces[interfaceIndex]
        sourceMacAddress = MacAddress.getByAddress(networkInterface.linkLayerAddresses[0].address)
        val snapLen = 65536
        val mode: PcapNetworkInterface.PromiscuousMode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS
        val timeout = 10000
        handle = networkInterface.openLive(snapLen, mode, timeout)
        Arp.arpRequest(sourceMacAddress, GATEWAY_IP_ADDRESS) { result -> destinationMacAddress = result }
        println(destinationMacAddress)

        while (true) {
            try {
                val packet: Packet = handle!!.nextPacketEx
                executor.execute {
                    handleIncomingInternetPacket(packet)
                }
            }

            catch (exception : Exception) {
                exception.printStackTrace()
            }
        }
    } catch (e: PcapNativeException) {
        e.printStackTrace()
    } catch (e: NotOpenException) {
        e.printStackTrace()
    } catch (e: TimeoutException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun handleIncomingInternetPacket(packet: Packet) {
    val ethernetPacket = packet.get(EthernetPacket::class.java) ?: return
    val ipV4Packet: IpV4Packet = ethernetPacket.get(IpV4Packet::class.java) ?: return
    if (
        clientIpAddress == null ||
        ipV4Packet.header.srcAddr.address.contentEquals(clientIpAddress?.address) ||
        ipV4Packet.header.srcAddr.address.contentEquals(SERVER_IP_ADDRESS.address)
    ) {
        return
    }

//    println("received packet of size " + packet.rawData.size)
//    println("user ip is " + clientIpAddress)

    var craftedPacket : IpV4Packet? = null

    if (ipV4Packet.contains(TcpPacket::class.java)) {
        val tcpPacket = ipV4Packet.get(TcpPacket::class.java)
        if (!usedClientSourcePorts[tcpPacket.header.dstPort.valueAsInt()]) {
//            println("not for user")
            return
        }

//        println(packet)

        try {
            craftedPacket =
                IpV4Packet.Builder(ipV4Packet).srcAddr(ipV4Packet.header.srcAddr).dstAddr(clientSourceIp as Inet4Address)
                    .payloadBuilder(
                        TcpPacket.Builder(tcpPacket).dstAddr(clientSourceIp)
                            .srcAddr(ipV4Packet.header.srcAddr)
                            .correctChecksumAtBuild(true)
                            .correctLengthAtBuild(true)
                    ).correctChecksumAtBuild(true).correctLengthAtBuild(true).build()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }

        val packetData = craftedPacket!!.rawData

        try {
//            val reCraftedPacket = IpV4Packet.newPacket(packetData, 0, packetData.size)
//            println("recrafted tcp packet")
//            println(reCraftedPacket)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        incomingInternetPackets.put(packetData)
    }

    if (ipV4Packet.contains(UdpPacket::class.java)) {
        val udpPacket = ipV4Packet.get(UdpPacket::class.java)
//        System.out.println("udp port " + udpPacket.header.srcPort)
        if (!usedClientSourcePorts[udpPacket.header.dstPort.valueAsInt()]) {
//            println("not for user")
            return
        }

//        println(packet)

        try {
            craftedPacket = IpV4Packet.Builder(ipV4Packet).srcAddr(ipV4Packet.header.srcAddr).dstAddr(clientSourceIp as Inet4Address)
                .payloadBuilder(
                    UdpPacket.Builder(udpPacket).dstAddr(clientSourceIp)
                        .srcAddr(ipV4Packet.header.srcAddr)
                        .correctChecksumAtBuild(true)
                        .correctLengthAtBuild(true)
                ).correctChecksumAtBuild(true).correctLengthAtBuild(true).build()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }

        val packetData = craftedPacket!!.rawData

        try {
//            val reCraftedPacket = IpV4Packet.newPacket(packetData, 0, packetData.size)
//            println("recrafted udp packet")
//            println(reCraftedPacket)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        incomingInternetPackets.put(packetData)
    }

//    if (craftedPacket != null) {
//        val packetData = craftedPacket.rawData
//        runBlocking {
//        println("sending")
//        packetsToSend.put(packetData)
//        tcpOutputStream?.writeWithSize(packetData, writeToUserBuffer)
//        }
//    }
}

fun handleIncomingUserPacket(packet: ByteArray) {
    if (destinationMacAddress == null) {
//        println("destination source is null : cant process packet")
        return
    }

    val ipV4Packet: IpV4Packet

    try {
        ipV4Packet = IpV4Packet.newPacket(packet, 0, packet.size)

        val requestAddress = ipV4Packet.header.dstAddr
//        println("request address ${requestAddress.hostAddress}")
    } catch (e: IllegalRawDataException) {
        e.printStackTrace()
        return
    }
    if (ipV4Packet.contains(UdpPacket::class.java)) {

        val udpPacket: UdpPacket = ipV4Packet.get(UdpPacket::class.java)
        val port = udpPacket.header.srcPort.valueAsInt()
        usedClientSourcePorts[port] = true
//        println("udp port $port")

        try {
            val ipPacket: IpV4Packet.Builder =
                IpV4Packet.Builder(ipV4Packet).srcAddr(SERVER_IP_ADDRESS).payloadBuilder(
                    UdpPacket.Builder(udpPacket).srcAddr(SERVER_IP_ADDRESS)
                        .dstAddr(ipV4Packet.header.dstAddr).correctChecksumAtBuild(true)
                        .correctLengthAtBuild(true)
                ).correctChecksumAtBuild(true).correctLengthAtBuild(true)
            val ethernetPacket: EthernetPacket =
                EthernetPacket.Builder().srcAddr(sourceMacAddress).dstAddr(destinationMacAddress).payloadBuilder(ipPacket)
                    .type(EtherType.IPV4).paddingAtBuild(true).build()
            handle?.sendPacket(ethernetPacket)

            try {
//                val reCraftedPacket = EthernetPacket.newPacket(ethernetPacket.rawData, 0, ethernetPacket.rawData.size)
//                println("recrafted tcp packet")
//                println(reCraftedPacket)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } catch (e: PcapNativeException) {
            e.printStackTrace()
        } catch (e: NotOpenException) {
            e.printStackTrace()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
    }

    if (ipV4Packet.contains(TcpPacket::class.java)) {

        val tcpPacket: TcpPacket = ipV4Packet.get(TcpPacket::class.java)
        val port: Int = tcpPacket.header.srcPort.valueAsInt()
        usedClientSourcePorts[port] = true
//        println("tcp port $port")

        try {
            val ipPacket: IpV4Packet.Builder =
                IpV4Packet.Builder(ipV4Packet).srcAddr(SERVER_IP_ADDRESS).payloadBuilder(
                    TcpPacket.Builder(tcpPacket).srcAddr(SERVER_IP_ADDRESS)
                        .dstAddr(ipV4Packet.header.dstAddr).correctChecksumAtBuild(true)
                        .correctLengthAtBuild(true)
                ).correctChecksumAtBuild(true).correctLengthAtBuild(true)
            val ethernetPacket: EthernetPacket =
                EthernetPacket.Builder().srcAddr(sourceMacAddress).dstAddr(destinationMacAddress).payloadBuilder(ipPacket)
                    .type(EtherType.IPV4).paddingAtBuild(true).build()
            handle?.sendPacket(ethernetPacket)

            try {
//                val reCraftedPacket = EthernetPacket.newPacket(ethernetPacket.rawData, 0, ethernetPacket.rawData.size)
//                println("recrafted udp packet")
//                println(reCraftedPacket)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } catch (e: PcapNativeException) {
            e.printStackTrace()
        } catch (e: NotOpenException) {
            e.printStackTrace()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
    }
}

fun main() {
    startVpn()
}
