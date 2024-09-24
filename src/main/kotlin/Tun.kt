package com.momid

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Memory
import com.sun.jna.Pointer

// Define the LibC interface to access C library functions via JNA
interface LibC : Library {
    // Corresponds to int open(const char *pathname, int flags);
    fun open(pathname: String, flags: Int): Int

    // Corresponds to int ioctl(int fd, unsigned long request, ...);
    fun ioctl(fd: Int, request: Long, argp: Pointer): Int

    // Corresponds to ssize_t read(int fd, void *buf, size_t count);
    fun read(fd: Int, buffer: ByteArray, count: Int): Int

    // Corresponds to ssize_t write(int fd, const void *buf, size_t count);
    fun write(fd: Int, buffer: ByteArray, count: Int): Int

    // Corresponds to int close(int fd);
    fun close(fd: Int): Int
}

// Load the standard C library (libc) using JNA
val libc: LibC = Native.load("c", LibC::class.java)

// Define constants needed for file operations and TUN configuration
const val O_RDONLY   = 0x0000
const val O_WRONLY   = 0x0001
const val O_RDWR     = 0x0002
const val O_NONBLOCK = 0x000800

const val IFF_TUN   = 0x0001
const val IFF_NO_PI = 0x1000

const val EAGAIN = 11

const val IFNAMSIZ = 16

// TUNSETIFF ioctl request code
const val TUNSETIFF = 0x400454caL

// Function to open and configure the TUN interface
fun getTunInterface(name: String): Int {
    // Open the TUN device file
    val tunFd = libc.open("/dev/net/tun", O_RDWR or O_NONBLOCK)
    if (tunFd < 0) {
        throw RuntimeException("Cannot open /dev/net/tun")
    }

    // Allocate memory for the ifreq structure (typically 40 bytes on 64-bit systems)
    val ifrSize = 40L
    val ifr = Memory(ifrSize)
    ifr.clear()

    // Write the interface name into ifr.ifr_name (first IFNAMSIZ bytes)
    val nameBytes = name.toByteArray(Charsets.UTF_8)
    if (nameBytes.size >= IFNAMSIZ) {
        throw IllegalArgumentException("Interface name too long")
    }
    ifr.write(0, nameBytes, 0, nameBytes.size)
    // Zero out the rest of ifr.ifr_name
    val padding = ByteArray(IFNAMSIZ - nameBytes.size) { 0 }
    ifr.write(nameBytes.size.toLong(), padding, 0, padding.size)

    // Set the interface flags in ifr.ifr_flags (short located after ifr_name)
    ifr.setShort(IFNAMSIZ.toLong(), (IFF_TUN or IFF_NO_PI).toShort())

    // Call ioctl to configure the TUN device
    val res = libc.ioctl(tunFd, TUNSETIFF, ifr)
    if (res < 0) {
        libc.close(tunFd)
        throw RuntimeException("ioctl TUNSETIFF failed")
    }

    return tunFd
}

// Function to read data from the TUN interface
fun readFromTun(tunFd: Int): ByteArray? {
    val buf = ByteArray(1500) // Typical MTU size for Ethernet
    val n = libc.read(tunFd, buf, buf.size)
    return if (n > 0) {
        buf.copyOf(n)
    } else if (n == 0 || (n == -1 && Native.getLastError() == EAGAIN)) {
        null // No data available right now
    } else {
        throw RuntimeException("Error reading from TUN interface")
    }
}

// Function to write data to the TUN interface
fun writeToTun(tunFd: Int, data: ByteArray): Boolean {
    val n = libc.write(tunFd, data, data.size)
    return n == data.size
}

// Main function demonstrating TUN interface usage
fun main() {
    val tunName = "tun0"

    // Open and configure the TUN interface
    val tunFd = getTunInterface(tunName)
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
        } else {
            // No data read; sleep briefly to avoid busy-waiting
            Thread.sleep(100)
        }
    }
}
