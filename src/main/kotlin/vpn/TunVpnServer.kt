//package com.momid.vpn
//
//import org.pcap4j.core.*
//import org.pcap4j.packet.*
//import org.pcap4j.util.MacAddress
//import java.io.IOException
//import java.net.*
//import java.util.concurrent.ArrayBlockingQueue
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//import java.util.concurrent.TimeoutException
//import kotlin.system.exitProcess
//
//var sourceMacAddress: MacAddress? = null
//private var destinationMacAddress: MacAddress? = null
//private var clientSourceIp = InetAddress.getByName("192.168.3.8")
//var clientIpAddress: Inet4Address? = null
//private val executor: ExecutorService = Executors.newSingleThreadExecutor()
//private var handle: PcapHandle? = null
//const val SERVER_IP = "194.146.123.180"
//val SERVER_IP_ADDRESS = Inet4Address.getByName("194.146.123.180") as Inet4Address
//val GATEWAY_IP_ADDRESS = "146.19.130.254"
//val usedClientSourcePorts = BooleanArray(65535) {
//    false
//}
//
//val incomingInternetPackets = ArrayBlockingQueue<ByteArray>(300)
//
//fun startVpn() {
//    try {
//        val networkInterfaces: List<PcapNetworkInterface> = Pcaps.findAllDevs()
//        var networkInterface: PcapNetworkInterface? = null
//        for (index in networkInterfaces.indices) {
//
//            for (inetAddress in networkInterfaces[index].addresses) {
//                if (inetAddress.address.hostAddress.equals("10.0.0.1")) {
//                    networkInterface = networkInterfaces[index]
//                    break
//                }
//            }
//        }
//
//        if (networkInterface == null) {
//            println("no tun interface found")
//            exitProcess(0)
//        }
//
////        sourceMacAddress = MacAddress.getByAddress(networkInterface.linkLayerAddresses[0].address)
//        val snapLen = 65536
//        val mode: PcapNetworkInterface.PromiscuousMode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS
//        val timeout = 1000
//        handle = networkInterface.openLive(snapLen, mode, timeout)
//        handle!!.blockingMode = PcapHandle.BlockingMode.NONBLOCKING
////        Arp.arpRequest(sourceMacAddress, GATEWAY_IP_ADDRESS) { result -> destinationMacAddress = result }
////        println(destinationMacAddress)
//
//        while (true) {
//            try {
//                val packet = handle!!.nextRawPacket
//                if (packet != null) {
//                    println("packet from tun")
//                    executor.execute {
////                    handleIncomingInternetPacket(packet)
//                        incomingInternetPackets.put(packet)
//                    }
//                }
//            }
//
//            catch (exception : Exception) {
//                exception.printStackTrace()
//            }
//        }
//    } catch (e: PcapNativeException) {
//        e.printStackTrace()
//    } catch (e: NotOpenException) {
//        e.printStackTrace()
//    } catch (e: TimeoutException) {
//        e.printStackTrace()
//    } catch (e: IOException) {
//        e.printStackTrace()
//    }
//}
//
//fun handleIncomingUserPacket(packet: ByteArray) {
//    if (handle != null) {
//        handle!!.sendPacket(packet)
//    } else {
//        println("handle is null")
//    }
//}
//
//fun main() {
//    startVpn()
//}
